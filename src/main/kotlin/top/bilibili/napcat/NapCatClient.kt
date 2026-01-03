package top.bilibili.napcat

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import top.bilibili.config.NapCatConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * NapCat OneBot v11 WebSocket 客户端（反向 WS）
 */
class NapCatClient(
    private val config: NapCatConfig
) {
    private val logger = LoggerFactory.getLogger(NapCatClient::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            pingIntervalMillis = config.heartbeatInterval
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isConnected = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)

    private val _eventFlow = MutableSharedFlow<MessageEvent>(replay = 0, extraBufferCapacity = 100)
    val eventFlow = _eventFlow.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    private val sendChannel = Channel<String>(Channel.UNLIMITED)

    /** Bot 的 QQ 号 */
    var selfId: Long = 0L
        private set

    /** 启动客户端 */
    fun start() {
        logger.info("正在启动 NapCat WebSocket 客户端...")
        logger.info("目标地址: ${config.getWebSocketUrl()}")
        scope.launch {
            connectLoop()
        }
    }

    /** 停止客户端 */
    fun stop() {
        logger.info("正在停止 NapCat WebSocket 客户端...")
        isConnected.set(false)
        sendChannel.close()
        scope.cancel()
        runBlocking {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client stopped"))
        }
        client.close()
        logger.info("NapCat WebSocket 客户端已停止")
    }

    /** 连接循环（支持自动重连） */
    private suspend fun connectLoop() {
        while (scope.isActive) {
            try {
                if (config.maxReconnectAttempts != -1 &&
                    reconnectAttempts.get() >= config.maxReconnectAttempts
                ) {
                    logger.error("已达到最大重连次数 (${config.maxReconnectAttempts})，停止重连")
                    break
                }

                connect()

            } catch (e: Exception) {
                logger.error("WebSocket 连接失败: ${e.message}")
                reconnectAttempts.incrementAndGet()

                if (config.maxReconnectAttempts == -1 ||
                    reconnectAttempts.get() < config.maxReconnectAttempts
                ) {
                    logger.info("将在 ${config.reconnectInterval}ms 后重连...")
                    delay(config.reconnectInterval)
                }
            }
        }
    }

    /** 建立 WebSocket 连接 */
    private suspend fun connect() {
        logger.info("正在连接到 NapCat WebSocket 服务器...")

        client.webSocket(
            urlString = config.getWebSocketUrl(),
            request = {
                if (config.token.isNotBlank()) {
                    headers["Authorization"] = "Bearer ${config.token}"
                }
            }
        ) {
            session = this
            isConnected.set(true)
            reconnectAttempts.set(0)
            logger.info("WebSocket 连接成功!")

            // 获取 Bot 的 QQ 号
            launch {
                try {
                    delay(500) // 等待连接稳定
                    val loginInfo = getLoginInfo()
                    if (loginInfo != null) {
                        selfId = loginInfo
                        logger.info("Bot QQ 号: $selfId")
                    }
                } catch (e: Exception) {
                    logger.error("获取登录信息失败: ${e.message}", e)
                }
            }

            // 启动发送协程
            val sendJob = launch {
                for (message in sendChannel) {
                    try {
                        send(message)
                        logger.debug("已发送消息: $message")
                    } catch (e: Exception) {
                        logger.error("发送消息失败: ${e.message}", e)
                    }
                }
            }

            try {
                // 接收消息循环
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            logger.debug("收到消息: $text")
                            handleMessage(text)
                        }

                        is Frame.Close -> {
                            logger.warn("WebSocket 连接被关闭: ${frame.readReason()}")
                            break
                        }

                        else -> {
                            logger.debug("收到其他类型的帧: ${frame.frameType}")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("接收消息时发生错误: ${e.message}", e)
            } finally {
                isConnected.set(false)
                sendJob.cancel()
                session = null
                logger.info("WebSocket 连接已断开")
            }
        }
    }

    /** 处理收到的消息 */
    private suspend fun handleMessage(text: String) {
        try {
            logger.debug("收到原始消息: $text")

            // 尝试解析为消息事件
            val event = json.decodeFromString<MessageEvent>(text)
            logger.info("成功解析 ${event.messageType} 消息事件 [${event.messageId}] 来自 ${event.userId}: ${event.rawMessage}")
            _eventFlow.emit(event)
        } catch (e: Exception) {
            logger.debug("不是消息事件，尝试解析为其他类型: ${e.message}")
            // 如果不是消息事件，尝试其他类型
            try {
                val metaEvent = json.decodeFromString<MetaEvent>(text)
                if (metaEvent.metaEventType == "heartbeat") {
                    logger.debug("收到心跳包")
                } else {
                    logger.debug("收到元事件: ${metaEvent.metaEventType}")
                }
            } catch (e2: Exception) {
                logger.debug("收到未知类型的消息，忽略: ${e2.message}")
            }
        }
    }

    /** 发送群消息 */
    suspend fun sendGroupMessage(groupId: Long, message: List<MessageSegment>): Boolean {
        return sendMessage("send_group_msg", mapOf(
            "group_id" to groupId,
            "message" to message
        ))
    }

    /** 发送私聊消息 */
    suspend fun sendPrivateMessage(userId: Long, message: List<MessageSegment>): Boolean {
        return sendMessage("send_private_msg", mapOf(
            "user_id" to userId,
            "message" to message
        ))
    }

    /** 通用发送消息方法 */
    private suspend fun sendMessage(action: String, params: Map<String, Any>): Boolean {
        if (!isConnected.get()) {
            logger.warn("WebSocket 未连接，无法发送消息")
            return false
        }

        return try {
            val paramsJson = buildMap<String, kotlinx.serialization.json.JsonElement> {
                params.forEach { (key, value) ->
                    put(key, when (value) {
                        is Long -> kotlinx.serialization.json.JsonPrimitive(value)
                        is List<*> -> json.encodeToJsonElement(
                            kotlinx.serialization.builtins.ListSerializer(MessageSegment.serializer()),
                            value as List<MessageSegment>
                        )
                        else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
                    })
                }
            }

            val request = OneBotAction(
                action = action,
                params = paramsJson,
                echo = System.currentTimeMillis().toString()
            )

            val message = json.encodeToString(request)
            sendChannel.send(message)
            logger.debug("消息已加入发送队列: $action")
            true
        } catch (e: Exception) {
            logger.error("发送消息失败: ${e.message}", e)
            false
        }
    }

    /** 获取连接状态 */
    fun isConnected(): Boolean = isConnected.get()

    /** 获取重连次数 */
    fun getReconnectAttempts(): Int = reconnectAttempts.get()

    /** 获取登录信息（Bot QQ 号） */
    private suspend fun getLoginInfo(): Long? {
        if (!isConnected.get()) {
            logger.warn("WebSocket 未连接，无法获取登录信息")
            return null
        }

        return try {
            val request = OneBotAction(
                action = "get_login_info",
                params = emptyMap(),
                echo = "login_info_${System.currentTimeMillis()}"
            )

            val message = json.encodeToString(request)
            sendChannel.send(message)
            logger.debug("已请求登录信息")

            // TODO: 实现响应等待机制
            // 目前由于 OneBot API 的异步特性，我们暂时无法同步获取响应
            // 可以考虑在 handleMessage 中解析 API 响应
            null
        } catch (e: Exception) {
            logger.error("获取登录信息失败: ${e.message}", e)
            null
        }
    }
}
