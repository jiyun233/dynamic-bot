package top.bilibili.service

// TODO: [Mirai依赖] 需要重写为 NapCat 实现
// ConfigService - 配置交互功能需要用 NapCat 消息处理重新实现
// 原有功能：通过交互式对话配置 At全体、主题色、推送模板、过滤器等

/*
原 ConfigService 完整代码已注释
主要功能包括：
1. 配置 At全体功能
2. 配置主题色
3. 配置推送模板（动态推送、直播推送、直播结束）
4. 配置过滤器（类型过滤、正则过滤）
5. 使用 whileSelectMessages 实现交互式配置流程

需要重写为基于 NapCatClient 的实现
*/

object ConfigService {
    // 临时占位：功能暂未实现
    suspend fun config(event: Any, uid: Long = 0L, contact: Any) {
        // TODO: 实现基于 NapCat 的配置功能
    }
}
