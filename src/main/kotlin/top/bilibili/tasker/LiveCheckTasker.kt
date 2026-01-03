package top.bilibili.tasker

import kotlinx.coroutines.withTimeout
import top.bilibili.core.BiliBiliBot
import top.bilibili.core.BiliBiliBot.liveUsers
import top.bilibili.BiliConfigManager
import top.bilibili.BiliData
import top.bilibili.api.getLive
import top.bilibili.data.LiveDetail
import top.bilibili.utils.sendAll
import java.time.Instant

object LiveCheckTasker : BiliCheckTasker("Live") {
    override var interval = BiliConfigManager.config.checkConfig.liveInterval
    private val liveCloseEnable = BiliConfigManager.config.enableConfig.liveCloseNotifyEnable

    private val liveChannel by BiliBiliBot::liveChannel
    private val dynamic by BiliData::dynamic

    private var lastLive: Long = Instant.now().epochSecond

    override suspend fun main() = withTimeout(180003) {
        val liveList = client.getLive()

        if (liveList != null) {
            val followingUsers = dynamic.filter { it.value.contacts.isNotEmpty() }.map { it.key }
            val lives = liveList.rooms
                .filter {
                    it.liveTime > lastLive
                }.filter {
                    followingUsers.contains(it.uid)
                }.sortedBy {
                    it.liveTime
                }

            if (lives.isNotEmpty()) {
                lastLive = lives.last().liveTime
                liveChannel.sendAll(lives.map { LiveDetail(it) })
                if (liveCloseEnable) liveUsers.putAll(lives.map { it.uid to it.liveTime })
            }
        }

    }
}