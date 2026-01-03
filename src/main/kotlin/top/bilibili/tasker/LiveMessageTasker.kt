package top.bilibili.tasker

import kotlinx.coroutines.withTimeout
import org.jetbrains.skia.Color
import top.bilibili.core.BiliBiliBot
import top.bilibili.BiliConfigManager
import top.bilibili.BiliData
import top.bilibili.data.LIVE_LINK
import top.bilibili.data.LiveInfo
import top.bilibili.data.LiveMessage
import top.bilibili.draw.makeDrawLive
import top.bilibili.draw.makeRGB
import top.bilibili.utils.formatTime
import top.bilibili.utils.logger

object LiveMessageTasker : BiliTasker() {
    override var interval: Int = 0

    private val liveChannel by BiliBiliBot::liveChannel
    private val messageChannel by BiliBiliBot::messageChannel

    override suspend fun main() {
        val liveDetail = liveChannel.receive()
        withTimeout(180004) {
            val liveInfo = liveDetail.item
            logger.debug("直播: ${liveInfo.uname}@${liveInfo.uid}@${liveInfo.title}")
            messageChannel.send(liveInfo.buildMessage(liveDetail.contact))
        }
    }

    suspend fun LiveInfo.buildMessage(contact: String? = null): LiveMessage {
        return LiveMessage(
            roomId,
            uid,
            this.uname,
            liveTime.formatTime,
            liveTime.toInt(),
            title,
            cover,
            area,
            LIVE_LINK(roomId.toString()),
            makeLive(),
            contact
        )
    }

    suspend fun LiveInfo.makeLive(): String? {
        return if (BiliConfigManager.config.enableConfig.drawEnable) {
            val color = BiliConfigManager.data.dynamic[uid]?.color ?: BiliConfigManager.config.imageConfig.defaultColor
            val colors = color.split(";", "；").map { Color.makeRGB(it.trim()) }
            makeDrawLive(colors)
        } else null
    }

}