package top.bilibili.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import top.bilibili.core.BiliBiliBot
import top.bilibili.BiliConfigManager
import top.bilibili.utils.decode
import top.bilibili.utils.isNotBlank
import top.bilibili.utils.json
import java.io.IOException

open class BiliClient : Closeable {
    override fun close() = clients.forEach { it.close() }

    private val proxys = if (BiliConfigManager.config.proxyConfig.proxy.isNotBlank()) {
        mutableListOf<ProxyConfig>().apply {
            BiliConfigManager.config.proxyConfig.proxy.forEach {
                if (it != "") {
                    add(ProxyBuilder.http(it))
                }
            }
        }
    } else {
        null
    }

    val clients = MutableList(3) { client() }

    protected fun client() = HttpClient(OkHttp) {
        defaultRequest {
            header(HttpHeaders.Origin, "https://t.bilibili.com")
            header(HttpHeaders.Referrer, "https://t.bilibili.com")
        }
        install(HttpTimeout) {
            socketTimeoutMillis = BiliConfigManager.config.checkConfig.timeout * 1000L
            connectTimeoutMillis = BiliConfigManager.config.checkConfig.timeout * 1000L
            requestTimeoutMillis = BiliConfigManager.config.checkConfig.timeout * 1000L
        }
        expectSuccess = true
        Json {
            json
        }
        //BrowserUserAgent()
        install(UserAgent) {
            agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"
        }
    }

    suspend inline fun <reified T> get(url: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): T =
        useHttpClient<String> {
            it.get(url) {
                header(HttpHeaders.Cookie, BiliBiliBot.cookie.toString() + "DedeUserID=" + BiliBiliBot.uid)
                block()
            }.body()
        }.decode()

    suspend inline fun <reified T> post(url: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): T =
        useHttpClient<String> {
            it.post(url) {
                header(HttpHeaders.Cookie, BiliBiliBot.cookie.toString() + "DedeUserID=" + BiliBiliBot.uid)
                block()
            }.body()
        }.decode()

    private var clientIndex = 0
    private var proxyIndex = 0

    suspend fun <T> useHttpClient(block: suspend (HttpClient) -> T): T = supervisorScope {
        while (isActive) {
            try {
                val client = clients[clientIndex]
                if (proxys != null && BiliConfigManager.config.enableConfig.proxyEnable) {
                    client.engineConfig.proxy = proxys[proxyIndex]
                    proxyIndex = (proxyIndex + 1) % proxys.size
                }
                return@supervisorScope block(client)
            } catch (throwable: Throwable) {
                if (isActive && (throwable is IOException || throwable is HttpRequestTimeoutException)) {
                    clientIndex = (clientIndex + 1) % clients.size
                } else {
                    throw throwable
                }
            }
        }
        throw CancellationException()
    }

}