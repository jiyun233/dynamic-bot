package top.bilibili.tasker

import kotlinx.coroutines.withTimeout
import top.bilibili.core.BiliBiliBot
import top.bilibili.BiliConfigManager
import top.bilibili.BiliData
import top.bilibili.api.getNewDynamic
import top.bilibili.data.DynamicDetail
import top.bilibili.data.DynamicType
import top.bilibili.utils.sendAll
import top.bilibili.utils.time
import top.bilibili.utils.logger
import java.time.Instant
import java.io.File

object DynamicCheckTasker : BiliCheckTasker("Dynamic") {

    override var interval = BiliConfigManager.config.checkConfig.interval

    private val dynamicChannel by BiliBiliBot::dynamicChannel

    private val dynamic by BiliData::dynamic
    private val bangumi by BiliData::bangumi

    private val listenAllDynamicMode = false

    private val banType = listOf(
        DynamicType.DYNAMIC_TYPE_LIVE,
        DynamicType.DYNAMIC_TYPE_LIVE_RCMD,
        //DynamicType.DYNAMIC_TYPE_PGC,
        //DynamicType.DYNAMIC_TYPE_PGC_UNION
    )

    private const val HISTORY_CAPACITY = 200
    private val historyDynamic = ArrayDeque<String>(HISTORY_CAPACITY)
    private val historyFile = File("data/dynamic_history.txt")

    // 初始化为当前时间减去10分钟，避免首次启动推送大量旧动态
    private var lastDynamic: Long = Instant.now().epochSecond - 600

    override fun init() {
        super.init()
        // ✅ 初始化时加载历史记录
        if (historyFile.exists()) {
            try {
                historyFile.readLines()
                    .takeLast(HISTORY_CAPACITY)
                    .forEach { historyDynamic.addLast(it) }
                logger.info("已加载 ${historyDynamic.size} 条动态历史记录")
            } catch (e: Exception) {
                logger.error("加载动态历史记录失败", e)
            }
        }
    }

    override suspend fun main() = withTimeout(180001) {
        // ✅ 优化：无订阅时跳过 API 调用
        val followingUsers = dynamic.filter { it.value.contacts.isNotEmpty() }.map { it.key }
        if (followingUsers.isEmpty() && bangumi.isEmpty()) {
            logger.debug("没有任何订阅，跳过动态检查")
            return@withTimeout
        }

        val dynamicList = client.getNewDynamic()
        if (dynamicList != null) {
            val dynamics = dynamicList.items
                .filter {
                    !banType.contains(it.type)
                }.filter {
                    it.time > lastDynamic
                }.filter {
                    !historyDynamic.contains(it.did)
                }.filter {
                    if (listenAllDynamicMode) true
                    else if (it.type == DynamicType.DYNAMIC_TYPE_PGC || it.type == DynamicType.DYNAMIC_TYPE_PGC_UNION)
                        bangumi.contains(it.modules.moduleAuthor.mid)
                    else followingUsers.contains(it.modules.moduleAuthor.mid)
                }.sortedBy {
                    it.time
                }

            if (dynamics.isNotEmpty()) {
                logger.info("检测到 ${dynamics.size} 条新动态")
                dynamics.forEach {
                    logger.info("新动态: ${it.modules.moduleAuthor.name} - ${it.modules.moduleDynamic.desc?.text?.take(50) ?: "无文本"}")
                }
            }

            // ✅ 使用 ArrayDeque，自动限制大小
            dynamics.map { it.did }.forEach { did ->
                if (historyDynamic.size >= HISTORY_CAPACITY) {
                    historyDynamic.removeFirst()
                }
                historyDynamic.addLast(did)
            }
            if (dynamics.isNotEmpty()) {
                lastDynamic = dynamics.last().time
                // ✅ 保存历史记录
                saveHistory()
            }
            dynamicChannel.sendAll(dynamics.map { DynamicDetail(it) })
        } else {
            logger.warn("获取动态列表失败")
        }
    }

    /**
     * 手动检查，忽略时间和历史限制，用于 /check 命令
     * @return 检测到的动态数量
     */
    suspend fun executeManualCheck(): Int = withTimeout(180001) {
        logger.info("$taskerName 手动触发检查（忽略时间限制）...")
        val dynamicList = client.getNewDynamic()
        if (dynamicList != null) {
            val followingUsers = dynamic.filter { it.value.contacts.isNotEmpty() }.map { it.key }

            // 忽略时间和历史限制，只过滤类型和订阅用户
            val dynamics = dynamicList.items
                .filter {
                    !banType.contains(it.type)
                }.filter {
                    if (listenAllDynamicMode) true
                    else if (it.type == DynamicType.DYNAMIC_TYPE_PGC || it.type == DynamicType.DYNAMIC_TYPE_PGC_UNION)
                        bangumi.contains(it.modules.moduleAuthor.mid)
                    else followingUsers.contains(it.modules.moduleAuthor.mid)
                }.sortedByDescending {
                    it.time  // 按时间降序，最新的在前
                }.take(1)  // 只取最新的1条

            if (dynamics.isNotEmpty()) {
                logger.info("手动检查检测到 ${dynamics.size} 条动态")
                dynamics.forEach {
                    logger.info("动态: ${it.modules.moduleAuthor.name} - ${it.modules.moduleDynamic.desc?.text?.take(50) ?: "无文本"}")
                }
                // 发送到处理流程
                dynamicChannel.sendAll(dynamics.map { DynamicDetail(it) })
                dynamics.size
            } else {
                logger.info("手动检查未检测到动态")
                0
            }
        } else {
            logger.warn("手动检查: 获取动态列表失败")
            0
        }
    }

    /**
     * ✅ 保存历史记录到文件
     */
    private fun saveHistory() {
        try {
            historyFile.parentFile?.mkdirs()
            historyFile.writeText(historyDynamic.joinToString("\n"))
        } catch (e: Exception) {
            logger.error("保存动态历史记录失败", e)
        }
    }
}