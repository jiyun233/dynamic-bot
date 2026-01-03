# 项目迁移完成

## 迁移详情

- **源目录**: `C:\Users\mengh\Desktop\1\dynamic-bot\dynamic-bot`
- **目标目录**: `C:\Users\mengh\Desktop\1\dynamic-bot`
- **迁移时间**: 2026-01-04
- **文件数量**: 108 个文件（不含编译输出）

## 迁移内容

### ✅ 已复制的文件和目录

#### 源代码
- `src/main/kotlin/` - Kotlin 源代码（88 个文件）
- `src/main/resources/` - 资源文件

#### 构建配置
- `build.gradle.kts` - Gradle 构建脚本
- `settings.gradle.kts` - Gradle 设置
- `gradle.properties` - Gradle 属性
- `gradle/` - Gradle wrapper 文件
- `gradlew` - Gradle wrapper 脚本（Unix）
- `gradlew.bat` - Gradle wrapper 脚本（Windows）

#### 配置文件
- `.env.example` - 环境变量示例
- `.gitignore` - Git 忽略规则
- `.editorconfig` - 编辑器配置

#### Docker 相关
- `Dockerfile` - Docker 镜像构建文件
- `docker-compose.yml` - Docker Compose 配置
- `.dockerignore` - Docker 忽略规则
- `DOCKER-README.md` - Docker 使用说明
- `DOCKER-DEPLOY.md` - Docker 部署指南

#### 文档
- `README.md` - 项目说明
- `LICENSE` - 开源许可证
- `PROJECT-INFO.md` - 迁移后的项目信息（新创建）

### ❌ 未复制的文件和目录（不需要）

- `.gradle/` - Gradle 缓存
- `.kotlin/` - Kotlin 编译缓存
- `build/` - 编译输出
- `.claude/` - Claude 会话数据
- `.github/` - GitHub Actions 配置
- `docs/` - 旧文档
- `scripts/` - 旧脚本
- `src/test/` - 旧的 Mirai 测试代码（已在迁移后删除）
- `data/` - 运行时数据
- `temp/` - 临时文件
- `logs/` - 日志文件
- `*.iml` - IntelliJ IDEA 项目文件
- `nul` - 临时文件

## 项目状态

✅ **编译测试**: 通过
```bash
.\gradlew.bat build -x test
BUILD SUCCESSFUL
```

✅ **依赖完整**: 所有源代码和依赖配置已迁移

✅ **结构完整**: 保持原有目录结构

## 后续步骤

### 1. 首次编译

```bash
cd C:\Users\mengh\Desktop\1\dynamic-bot
.\gradlew.bat build -x test
```

### 2. 运行程序

```bash
java -jar build\libs\dynamic-bot-*-all.jar
```

### 3. 配置 Bot

首次运行后，编辑以下配置文件：
- `config/bot.yml` - NapCat 连接信息和管理员设置
- `config/data.yml` - 订阅数据和推送配置

### 4. 添加字体（可选）

如果需要使用自定义字体，将字体文件放到：
- `data/font/` 目录

或者将字体文件打包到：
- `src/main/resources/font/` 目录（需要重新编译）

## 核心功能

### 链接解析
- ✅ 自动解析 B站链接（视频、动态、直播、番剧、专栏）
- ✅ 支持 QQ 小程序分享
- ✅ 生成精美图文卡片
- ✅ 短链接转标准 BV 号
- ✅ 无限循环防护

### 动态订阅
- ✅ 订阅用户动态
- ✅ 自动推送新动态
- ✅ 直播开播/关播通知
- ✅ 自定义推送模板

### 管理命令
- ✅ `/subscribe <UID>` - 订阅用户
- ✅ `/unsubscribe <UID>` - 取消订阅
- ✅ `/list` - 查看订阅列表
- ✅ `/check` - 手动触发检查
- ✅ `/login` - B站扫码登录

## 技术栈

- **语言**: Kotlin 1.9.22
- **HTTP**: Ktor 2.3.7
- **图片渲染**: Skiko 0.7.93
- **协程**: kotlinx.coroutines
- **序列化**: kotlinx.serialization
- **Bot 框架**: OneBot v11（NapCat）

## 注意事项

1. **测试代码已移除**: 旧的 Mirai 测试代码已删除，因为依赖已废弃
2. **编译输出已清理**: build/ 和 .gradle/ 目录已清理，首次编译会重新生成
3. **配置需要手动创建**: 首次运行会自动创建配置文件模板
4. **需要 NapCat**: 确保 NapCat 已正确安装和配置

## 项目大小

- **源代码**: ~108 个文件
- **编译后 JAR**: ~50MB（包含所有依赖）

## 许可证

AGPL-3.0 License

## 支持

如有问题，请参考：
- `PROJECT-INFO.md` - 项目详细信息
- `README.md` - 原项目说明
- `DOCKER-README.md` - Docker 部署指南
