package top.bilibili.tasker

import top.bilibili.BiliConfigManager
import top.bilibili.utils.cachePath
import java.nio.file.Path
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory

object CacheClearTasker : BiliTasker() {
    override var interval: Int = 60 * 60 * 24

    private val expires by BiliConfigManager.config.cacheConfig::expires

    override suspend fun main() {
        for (e in expires) {
            if (e.value > 0) {
                e.key.cachePath().clearExpireFile(e.value)
            }
        }
    }

    private fun Path.clearExpireFile(expire: Int) {
        forEachDirectoryEntry {
            if (it.isDirectory()) {
                it.clearExpireFile(expire)
            } else if (System.currentTimeMillis() - it.toFile().lastModified() >= expire * interval * unitTime) {
                it.toFile().delete()
            }
        }
    }
}
