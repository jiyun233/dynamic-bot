package top.bilibili.service

// TODO: [Mirai依赖] 需要重写为 NapCat 实现
// import net.mamoe.mirai.contact.Contact
// import net.mamoe.mirai.message.data.buildForwardMessage
import top.bilibili.BiliConfigManager
import top.bilibili.BiliData
import top.bilibili.api.getDynamicDetail
import top.bilibili.api.getLive
import top.bilibili.data.DynamicMessage
import top.bilibili.data.LIVE_LINK
import top.bilibili.data.LiveCloseMessage
import top.bilibili.data.LiveMessage
// TODO: [Mirai依赖] buildMessage 方法需要重写
// import top.bilibili.tasker.DynamicMessageTasker.buildMessage
// import top.bilibili.tasker.LiveMessageTasker.buildMessage
// import top.bilibili.tasker.SendTasker.buildMessage
import top.bilibili.utils.biliClient

object TemplateService {
    // TODO: [Mirai依赖] 需要重写模板列表展示功能
    /*
    suspend fun listTemplate(type: String, subject: Contact) {
        val template = when (type) {
            "d" -> BiliConfigManager.config.templateConfig.dynamicPush
            "l" -> BiliConfigManager.config.templateConfig.livePush
            "le" -> BiliConfigManager.config.templateConfig.liveClose
            else -> {
                subject.sendMessage("类型错误 d:动态 l:直播 le:直播结束")
                return
            }
        }

        // https://t.bilibili.com/385190177693666264
        val dynamic = when (type) {
            "d" -> biliClient.getDynamicDetail("385190177693666264")?.buildMessage()!!
            "l" -> biliClient.getLive(1, 1)?.rooms?.first()?.buildMessage()!!
            "le" -> LiveCloseMessage(
                0,0,"Test", "2022年1月1日 00:00:00", 1640966400, "2022年1月1日 01:02:03",
                "1小时 2分钟 3秒", "测试测试测试TEST", "游戏", LIVE_LINK("0")
            )
            else -> return
        }
        subject.sendMessage(buildForwardMessage(subject) {
            var pt = 0
            subject.bot named dynamic.name at dynamic.timestamp says if (type == "d") "动态推送模板" else "直播推送模板"
            subject.bot named dynamic.name at dynamic.timestamp says "下面每个转发消息都代表一个模板推送效果"
            for (t in template) {
                subject.bot named dynamic.name at dynamic.timestamp + pt says t.key
                subject.bot named dynamic.name at dynamic.timestamp + pt says buildForwardMessage(subject) {
                    when (dynamic) {
                        is DynamicMessage -> dynamic.buildMessage(t.value, listOf(subject)).forEach {
                            subject.bot named dynamic.name at dynamic.timestamp + pt says it
                        }
                        is LiveMessage -> dynamic.buildMessage(t.value, listOf(subject)).forEach {
                            subject.bot named dynamic.name at dynamic.timestamp + pt says it
                        }
                        is LiveCloseMessage -> dynamic.buildMessage(t.value).forEach {
                            subject.bot named dynamic.name at dynamic.timestamp + pt says it
                        }
                    }
                }
                pt += 86400
            }
        })
    }
    */

    suspend fun listTemplate(type: String, subject: Any) {
        // TODO: 实现基于 NapCat 的模板列表展示
    }

    fun setTemplate(type: String, template: String, subject: String): String {
        val pushTemplates = when (type) {
            "d" -> BiliConfigManager.config.templateConfig.dynamicPush
            "l" -> BiliConfigManager.config.templateConfig.livePush
            "le" -> BiliConfigManager.config.templateConfig.liveClose
            else -> return "类型错误 d:动态 l:直播 le:直播结束"
        }
        val push = when (type) {
            "d" -> BiliConfigManager.data.dynamicPushTemplate
            "l" -> BiliConfigManager.data.livePushTemplate
            "le" -> BiliConfigManager.data.liveCloseTemplate
            else -> return "类型错误 d:动态 l:直播 le:直播结束"
        }
        return if (pushTemplates.containsKey(template)) {
            push.forEach { (_, u) -> u.remove(subject) }
            if (!push.containsKey(template)) push[template] = mutableSetOf()
            push[template]!!.add(subject)
            "配置完成"
        } else "没有这个模板哦 $template"
    }
}
