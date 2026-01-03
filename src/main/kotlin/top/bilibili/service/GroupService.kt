package top.bilibili.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// TODO: [Mirai依赖] 需要重写为 NapCat 实现
// import net.mamoe.mirai.contact.Friend
import top.bilibili.BiliConfigManager
import top.bilibili.BiliData
import top.bilibili.Group
// import top.bilibili.utils.delegate
// import top.bilibili.utils.findContactAll
// import top.bilibili.utils.name

object GroupService {
    private val mutex = Mutex()

    suspend fun createGroup(name: String, operator: Long) = mutex.withLock {
        if (!group.containsKey(name)) {
            if (name.matches("^[0-9]*\$".toRegex())) return@withLock "分组名不能全为数字"
            group[name] = Group(name, operator)
            "创建成功"
        } else "分组名称重复"
    }

    suspend fun delGroup(name: String, operator: Long) = mutex.withLock {
        if (group.containsKey(name)) {
            if (group[name]!!.creator == operator) {
                dynamic.forEach { (_, s) -> s.contacts.remove(name) }
                BiliConfigManager.data.dynamicPushTemplate.forEach { (_, c) -> c.remove(name) }
                BiliConfigManager.data.livePushTemplate.forEach { (_, c) -> c.remove(name) }
                filter.remove(name)
                group.remove(name)
                "删除成功"
            } else "无权删除"
        } else "没有此分组 [$name]"
    }

    suspend fun listGroup(name: String? = null, operator: Long) = mutex.withLock {
        if (name == null) {
            group.values.filter {
                operator == BiliConfigManager.config.admin || operator == it.creator || it.admin.contains(operator)
            }.joinToString("\n") {
                // TODO: [Mirai依赖] 需要重写联系人查找逻辑
                "${it.name}@${it.creator}"
            }.ifEmpty { "没有创建或管理任何分组哦" }
        } else {
            group[name]?.toString() ?: "没有此分组哦"
        }
    }

    suspend fun setGroupAdmin(name: String, contacts: String, operator: Long) = mutex.withLock {
        if (group.containsKey(name)) {
            if (group[name]!!.creator == operator) {
                // TODO: [Mirai依赖] 需要重写联系人查找和验证逻辑
                var failMsg = ""
                group[name]?.admin?.addAll(contacts.split(",", "，").mapNotNull {
                    try {
                        it.toLong()
                    } catch (e: NumberFormatException) {
                        failMsg += "$it, "
                        null
                    }
                }.toSet())
                if (failMsg.isEmpty()) "添加成功"
                else "[$failMsg] 添加失败"
            } else "无权添加"
        } else "没有此分组 [$name]"
    }

    suspend fun banGroupAdmin(name: String, contacts: String, operator: Long) = mutex.withLock {
        if (group.containsKey(name)) {
            if (group[name]!!.creator == operator) {
                var failMsg = ""
                val admin = group[name]!!.admin
                contacts.split(",", "，").map {
                    try {
                        it.toLong()
                    } catch (e: NumberFormatException) {
                        failMsg += "$it, "
                        null
                    }
                }.filterNotNull().toSet().forEach {
                    if (!admin.remove(it)) failMsg += "$it, "
                }
                if (failMsg.isEmpty()) "删除成功"
                else "[$failMsg] 删除失败"
            } else "无权删除"
        } else "没有此分组 [$name]"
    }

    suspend fun pushGroupContact(name: String, contacts: String, operator: Long) = mutex.withLock {
        if (group.containsKey(name)) {
            if (checkGroupPerm(name, operator)) {
                // TODO: [Mirai依赖] 需要重写联系人查找逻辑
                var failMsg = ""
                group[name]?.contacts?.addAll(contacts.split(",", "，").mapNotNull {
                    // 简化实现：直接使用字符串作为联系人ID
                    try {
                        it.toLong().toString()
                    } catch (e: NumberFormatException) {
                        failMsg += "$it, "
                        null
                    }
                }.toSet())
                if (failMsg.isEmpty()) "添加成功"
                else "[$failMsg] 添加失败"
            } else "无权添加"
        } else "没有此分组 [$name]"
    }

    suspend fun delGroupContact(name: String, contacts: String, operator: Long) = mutex.withLock {
        if (group.containsKey(name)) {
            if (checkGroupPerm(name, operator)) {
                // TODO: [Mirai依赖] 需要重写联系人查找逻辑
                var failMsg = ""
                group[name]?.contacts?.removeAll(contacts.split(",", "，").map {
                    try {
                        it.toLong().toString()
                    } catch (e: NumberFormatException) {
                        failMsg += "$it, "
                        ""
                    }
                }.filter { it.isNotEmpty() }.toSet())
                if (failMsg.isEmpty()) "删除成功"
                else "[$failMsg] 删除失败"
            } else "无权删除"
        } else "没有此分组 [$name]"
    }

    fun checkGroupPerm(name: String, operator: Long): Boolean =
        group[name]?.creator == operator || group[name]?.admin?.contains(operator) == true

}
