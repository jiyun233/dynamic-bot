package top.bilibili

import org.slf4j.LoggerFactory
import top.bilibili.core.BiliBiliBot
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Main")

/**
 * 程序主入口
 */
fun main(args: Array<String>) {
    try {
        // 添加 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("收到停止信号，正在关闭...")
            BiliBiliBot.stop()
        })

        // 启动 Bot
        BiliBiliBot.start()

        // 保持程序运行
        Thread.currentThread().join()

    } catch (e: InterruptedException) {
        logger.info("程序被中断")
    } catch (e: Exception) {
        logger.error("程序运行异常: ${e.message}", e)
        exitProcess(1)
    }
}
