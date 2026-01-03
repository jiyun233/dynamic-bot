package top.bilibili.core

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import top.bilibili.BiliConfigManager
import top.bilibili.config.ConfigManager
import top.bilibili.napcat.MessageEvent
import top.bilibili.napcat.MessageSegment
import top.bilibili.napcat.NapCatClient
import top.bilibili.data.BiliCookie
import top.bilibili.data.DynamicDetail
import top.bilibili.data.LiveDetail
import top.bilibili.data.BiliMessage
import top.bilibili.api.userInfo
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.Path

/**
 * BiliBili 动态推送 Bot 核心类
 * 替代原来的 BiliBiliBot 插件对象
 */
object BiliBiliBot : CoroutineScope {
    val logger = LoggerFactory.getLogger(BiliBiliBot::class.java)
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    /** 数据文件夹 */
    val dataFolder = File("data")
    val dataFolderPath: Path = Path("data")

    /** 临时文件目录 */
    val tempPath: Path = Path("temp").also { path ->
        if (!path.toFile().exists()) {
            path.toFile().mkdirs()
        }
    }

    /** B站 Cookie */
    var cookie = BiliCookie()

    /** NapCat WebSocket 客户端 */
    lateinit var napCat: NapCatClient
        private set

    /** Bot 配置 */
    lateinit var config: top.bilibili.config.BotConfig
        private set

    /** B站用户 UID */
    var uid: Long = 0L

    /** B站关注分组 ID */
    var tagid: Int = 0

    /** 数据 Channel */
    val dynamicChannel = Channel<DynamicDetail>(20)
    val liveChannel = Channel<LiveDetail>(20)
    val messageChannel = Channel<BiliMessage>(20)
    val missChannel = Channel<BiliMessage>(10)
    val liveUsers = mutableMapOf<Long, Long>()

    /** 获取资源文件流 */
    fun getResourceAsStream(path: String): InputStream? {
        return this::class.java.getResourceAsStream(path)
    }

    /** 启动 Bot */
    fun start() {
        logger.info("========================================")
        logger.info("  BiliBili 动态推送 Bot")
        logger.info("  版本: 4.0.0-STANDALONE")
        logger.info("========================================")

        try {
            // 1. 加载配置
            logger.info("正在加载配置...")
            BiliConfigManager.init()
            ConfigManager.init()
            config = ConfigManager.botConfig

            if (!config.napcat.validate()) {
                logger.error("NapCat 配置无效，请检查 config/bot.yml")
                return
            }

            // 2. 初始化 NapCat 客户端
            logger.info("正在初始化 NapCat 客户端...")
            napCat = NapCatClient(config.napcat)

            // 3. 订阅消息事件
            launch {
                napCat.eventFlow.collect { event ->
                    handleMessageEvent(event)
                }
            }

            // 4. 启动 NapCat 客户端
            napCat.start()

            // 5. 初始化 B站数据
            launch {
                delay(3000) // 等待 WebSocket 连接
                initBiliData()
            }

            // 6. 启动任务
            launch {
                delay(5000) // 等待初始化完成
                startTasks()
            }

            logger.info("Bot 启动成功！")

        } catch (e: Exception) {
            logger.error("Bot 启动失败: ${e.message}", e)
            stop()
        }
    }

    /** 停止 Bot */
    fun stop() {
        logger.info("正在停止 Bot...")

        try {
            // 停止 NapCat 客户端
            if (::napCat.isInitialized) {
                napCat.stop()
            }

            // 保存配置
            if (::config.isInitialized) {
                BiliConfigManager.saveConfig()
            }

            // 取消所有协程
            job.cancel()

            logger.info("Bot 已停止")
        } catch (e: Exception) {
            logger.error("停止 Bot 时发生错误: ${e.message}", e)
        }
    }

    /** 处理 QQ 消息事件 */
    private suspend fun handleMessageEvent(event: MessageEvent) {
        try {
            when (event.messageType) {
                "group" -> handleGroupMessage(event)
                "private" -> handlePrivateMessage(event)
                else -> logger.debug("收到未知类型的消息: ${event.messageType}")
            }
        } catch (e: Exception) {
            logger.error("处理消息事件失败: ${e.message}", e)
        }
    }

    /** 处理群消息 */
    private suspend fun handleGroupMessage(event: MessageEvent) {
        val groupId = event.groupId ?: return
        val userId = event.userId
        val message = event.rawMessage

        logger.info("群消息 [$groupId] 来自 $userId: $message")

        // 处理登录命令（仅管理员可用）
        if (isAdmin(userId) && (message.trim() == "/login" || message.trim() == "登录")) {
            logger.info("触发登录命令，准备发送二维码...")
            launch {
                top.bilibili.service.LoginService.login(isGroup = true, contactId = groupId)
            }
            return
        }

        // 处理订阅命令（仅管理员可用）
        if (isAdmin(userId) && message.trim().startsWith("/subscribe ")) {
            val uid = message.trim().removePrefix("/subscribe ").trim().toLongOrNull()
            if (uid != null) {
                handleSubscribe(groupId, uid, isGroup = true)
            } else {
                sendGroupMessage(groupId, listOf(MessageSegment.text("UID 格式错误")))
            }
            return
        }

        // 处理取消订阅命令（仅管理员可用）
        if (isAdmin(userId) && message.trim().startsWith("/unsubscribe ")) {
            val uid = message.trim().removePrefix("/unsubscribe ").trim().toLongOrNull()
            if (uid != null) {
                handleUnsubscribe(groupId, uid, isGroup = true)
            } else {
                sendGroupMessage(groupId, listOf(MessageSegment.text("UID 格式错误")))
            }
            return
        }

        // 处理订阅列表查询命令（仅管理员可用）
        if (isAdmin(userId) && message.trim() == "/list") {
            handleListSubscriptions(groupId, isGroup = true)
            return
        }

        // 处理手动触发检查命令（仅管理员可用，用于测试）
        if (isAdmin(userId) && message.trim() == "/check") {
            sendGroupMessage(groupId, listOf(MessageSegment.text("手动触发订阅检查...")))
            launch {
                try {
                    top.bilibili.tasker.DynamicCheckTasker.executeOnce()
                    top.bilibili.tasker.LiveCheckTasker.executeOnce()
                    sendGroupMessage(groupId, listOf(MessageSegment.text("检查完成！")))
                } catch (e: Exception) {
                    sendGroupMessage(groupId, listOf(MessageSegment.text("检查失败: ${e.message}")))
                }
            }
            return
        }

        // TODO: 实现其他命令处理
    }

    /** 处理私聊消息 */
    private suspend fun handlePrivateMessage(event: MessageEvent) {
        val userId = event.userId
        val message = event.rawMessage

        logger.info("私聊消息 来自 $userId: $message")

        // 处理登录命令（仅管理员可用）
        if (isAdmin(userId) && (message.trim() == "/login" || message.trim() == "登录")) {
            logger.info("触发登录命令，准备发送二维码...")
            launch {
                top.bilibili.service.LoginService.login(isGroup = false, contactId = userId)
            }
            return
        }

        // TODO: 实现其他私聊命令处理
    }

    /** 检查是否为管理员 */
    private fun isAdmin(userId: Long): Boolean {
        return userId == BiliConfigManager.config.admin
    }

    /** 处理订阅 */
    private suspend fun handleSubscribe(contactId: Long, uid: Long, isGroup: Boolean) {
        try {
            val contactStr = if (isGroup) "group:$contactId" else "private:$contactId"

            // 获取用户信息
            val userInfo = top.bilibili.utils.biliClient.userInfo(uid)
            if (userInfo == null) {
                val msg = "无法获取用户信息，UID 可能不存在: $uid"
                if (isGroup) {
                    sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
                } else {
                    sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
                }
                return
            }

            // 添加订阅
            val subData = top.bilibili.BiliData.dynamic.getOrPut(uid) {
                top.bilibili.SubData(
                    name = userInfo.name ?: "未知用户",
                    contacts = mutableSetOf(),
                    banList = mutableMapOf()
                )
            }

            if (contactStr in subData.contacts) {
                val userName = userInfo.name ?: "未知用户"
                val msg = "已经订阅过 $userName (UID: $uid) 了"
                if (isGroup) {
                    sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
                } else {
                    sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
                }
                return
            }

            subData.contacts.add(contactStr)
            top.bilibili.BiliConfigManager.saveConfig()

            val userName = userInfo.name ?: "未知用户"
            val msg = "订阅成功！\n用户: $userName\nUID: $uid"
            if (isGroup) {
                sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
            } else {
                sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
            }

        } catch (e: Exception) {
            logger.error("处理订阅失败: ${e.message}", e)
            val msg = "订阅失败: ${e.message}"
            if (isGroup) {
                sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
            } else {
                sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
            }
        }
    }

    /** 处理取消订阅 */
    private suspend fun handleUnsubscribe(contactId: Long, uid: Long, isGroup: Boolean) {
        try {
            val contactStr = if (isGroup) "group:$contactId" else "private:$contactId"

            val subData = top.bilibili.BiliData.dynamic[uid]
            if (subData == null || contactStr !in subData.contacts) {
                val msg = "没有订阅过 UID: $uid"
                if (isGroup) {
                    sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
                } else {
                    sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
                }
                return
            }

            subData.contacts.remove(contactStr)
            top.bilibili.BiliConfigManager.saveConfig()

            val msg = "取消订阅成功！UID: $uid"
            if (isGroup) {
                sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
            } else {
                sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
            }

        } catch (e: Exception) {
            logger.error("处理取消订阅失败: ${e.message}", e)
            val msg = "取消订阅失败: ${e.message}"
            if (isGroup) {
                sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
            } else {
                sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
            }
        }
    }

    /** 处理查询订阅列表 */
    private suspend fun handleListSubscriptions(contactId: Long, isGroup: Boolean) {
        try {
            val contactStr = if (isGroup) "group:$contactId" else "private:$contactId"

            val subscriptions = top.bilibili.BiliData.dynamic.filter { (_, subData) ->
                contactStr in subData.contacts
            }

            if (subscriptions.isEmpty()) {
                val msg = "当前没有任何订阅"
                if (isGroup) {
                    sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
                } else {
                    sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
                }
                return
            }

            val msg = buildString {
                appendLine("当前订阅列表：")
                subscriptions.forEach { (uid, _) ->
                    val userInfo = top.bilibili.utils.biliClient.userInfo(uid)
                    if (userInfo != null) {
                        val userName = userInfo.name ?: "未知用户"
                        appendLine("- $userName (UID: $uid)")
                    } else {
                        appendLine("- UID: $uid")
                    }
                }
            }

            if (isGroup) {
                sendGroupMessage(contactId, listOf(MessageSegment.text(msg.trim())))
            } else {
                sendPrivateMessage(contactId, listOf(MessageSegment.text(msg.trim())))
            }

        } catch (e: Exception) {
            logger.error("查询订阅列表失败: ${e.message}", e)
            val msg = "查询失败: ${e.message}"
            if (isGroup) {
                sendGroupMessage(contactId, listOf(MessageSegment.text(msg)))
            } else {
                sendPrivateMessage(contactId, listOf(MessageSegment.text(msg)))
            }
        }
    }

    /** 初始化 B站数据 */
    private suspend fun initBiliData() {
        try {
            logger.info("正在初始化 B站数据...")
            // 调用 Init.kt 中的初始化函数
            top.bilibili.initData()
            logger.info("B站数据初始化完成")
        } catch (e: Exception) {
            logger.error("初始化 B站数据失败: ${e.message}", e)
        }
    }

    /** 启动所有任务 */
    private fun startTasks() {
        try {
            logger.info("正在启动任务...")

            // 启动链接解析任务
            logger.info("启动 ListenerTasker...")
            top.bilibili.tasker.ListenerTasker.start()

            // 启动订阅相关任务
            logger.info("启动 DynamicCheckTasker...")
            top.bilibili.tasker.DynamicCheckTasker.start()

            logger.info("启动 LiveCheckTasker...")
            top.bilibili.tasker.LiveCheckTasker.start()

            logger.info("启动 LiveCloseCheckTasker...")
            top.bilibili.tasker.LiveCloseCheckTasker.start()

            logger.info("启动 DynamicMessageTasker...")
            top.bilibili.tasker.DynamicMessageTasker.start()

            logger.info("启动 LiveMessageTasker...")
            top.bilibili.tasker.LiveMessageTasker.start()

            logger.info("启动 SendTasker...")
            top.bilibili.tasker.SendTasker.start()

            logger.info("启动 CacheClearTasker...")
            top.bilibili.tasker.CacheClearTasker.start()

            logger.info("所有任务已启动")
        } catch (e: Exception) {
            logger.error("启动任务失败: ${e.message}", e)
        }
    }

    /** 发送群消息 */
    suspend fun sendGroupMessage(groupId: Long, message: List<MessageSegment>): Boolean {
        return napCat.sendGroupMessage(groupId, message)
    }

    /** 发送私聊消息 */
    suspend fun sendPrivateMessage(userId: Long, message: List<MessageSegment>): Boolean {
        return napCat.sendPrivateMessage(userId, message)
    }

    /** 发送消息到指定联系人 */
    suspend fun sendMessage(contact: ContactId, message: List<MessageSegment>): Boolean {
        return when (contact.type) {
            "group" -> sendGroupMessage(contact.id, message)
            "private" -> sendPrivateMessage(contact.id, message)
            else -> {
                logger.warn("未知的联系人类型: ${contact.type}")
                false
            }
        }
    }
}
