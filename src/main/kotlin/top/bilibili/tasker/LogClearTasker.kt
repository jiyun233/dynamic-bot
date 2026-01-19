package top.bilibili.tasker

import org.slf4j.LoggerFactory
import java.io.File

object LogClearTasker : BiliTasker() {
    private val logger = LoggerFactory.getLogger(LogClearTasker::class.java)
    override var interval: Int = 60 * 60 * 24 * 7
    private val normalLogPattern = Regex("^bilibili-bot\\.\\d{4}-\\d{2}-\\d{2}\\.log$")

    override suspend fun main() {
        val logDir = File("logs")
        if (!logDir.exists() || !logDir.isDirectory) {
            logger.info("日志目录不存在，跳过清理")
            return
        }

        val now = System.currentTimeMillis()
        val expireMillis = 7L * 24 * 60 * 60 * 1000
        var deleted = 0

        logDir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            if (!normalLogPattern.matches(file.name)) return@forEach

            val age = now - file.lastModified()
            if (age >= expireMillis) {
                if (file.delete()) {
                    deleted++
                } else {
                    logger.warn("删除日志失败: ${file.name}")
                }
            }
        }

        logger.info("日志清理完成，删除 $deleted 个过期普通日志文件")
    }
}
