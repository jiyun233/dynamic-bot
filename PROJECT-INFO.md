# BiliBili 动态推送 Bot - 迁移版本

这是从原项目迁移过来的纯净版本，只包含源代码和编译所需的文件。

## 项目结构

```
dynamic-bot/
├── src/                    # 源代码目录
│   ├── main/
│   │   ├── kotlin/        # Kotlin 源代码
│   │   │   └── top/
│   │   │       └── bilibili/
│   │   │           ├── api/           # B站 API 接口
│   │   │           ├── client/        # HTTP 客户端
│   │   │           ├── config/        # 配置管理
│   │   │           ├── core/          # 核心模块
│   │   │           ├── data/          # 数据模型
│   │   │           ├── draw/          # 图片渲染
│   │   │           ├── napcat/        # NapCat 客户端
│   │   │           ├── service/       # 业务服务
│   │   │           ├── tasker/        # 定时任务
│   │   │           ├── utils/         # 工具类
│   │   │           ├── BiliConfig.kt  # 配置文件
│   │   │           ├── BiliData.kt    # 数据文件
│   │   │           └── Main.kt        # 程序入口
│   │   └── resources/     # 资源文件
│   │       └── font/      # 字体文件（可选）
│   └── test/              # （已删除旧的 Mirai 测试）
├── gradle/                # Gradle wrapper
├── build.gradle.kts       # Gradle 构建脚本
├── settings.gradle.kts    # Gradle 设置
├── gradle.properties      # Gradle 属性
├── gradlew                # Gradle wrapper 脚本（Linux/Mac）
├── gradlew.bat            # Gradle wrapper 脚本（Windows）
├── .env.example           # 环境变量示例
├── .gitignore             # Git 忽略文件
├── Dockerfile             # Docker 镜像构建文件
├── docker-compose.yml     # Docker Compose 配置
├── README.md              # 项目说明
└── LICENSE                # 许可证

```

## 快速开始

### 1. 编译项目

```bash
# Windows
.\gradlew.bat build -x test

# Linux/Mac
./gradlew build -x test
```

编译完成后，可执行文件位于：
- `build/libs/dynamic-bot-*-all.jar`

### 2. 配置文件

首次运行时，程序会自动创建配置文件目录结构：

```
config/
├── bot.yml          # Bot 基础配置（NapCat 连接信息）
└── data.yml         # 订阅数据和推送配置

data/
├── font/            # 字体文件目录
└── cookies.json     # B站 Cookie（可选）
```

### 3. 运行 Bot

```bash
java -jar build/libs/dynamic-bot-*-all.jar
```

或者使用 Docker：

```bash
docker-compose up -d
```

## 主要功能

### 1. 链接解析
- 自动解析群消息中的 B站链接
- 支持视频、动态、直播、番剧、专栏等多种类型
- 支持 QQ 小程序分享的 B站链接
- 生成精美的图文卡片
- 返回标准的 BV 号链接

### 2. 动态订阅
- 订阅 B站用户的动态
- 自动检测新动态并推送到群聊/私聊
- 支持自定义推送模板
- 支持直播开播/关播通知

### 3. 管理命令
- `/subscribe <UID>` - 订阅用户
- `/unsubscribe <UID>` - 取消订阅
- `/list` - 查看订阅列表
- `/check` - 手动触发检查（测试用）
- `/login` - B站扫码登录

## 配置说明

### bot.yml 示例

```yaml
# NapCat WebSocket 配置
napcat:
  host: "127.0.0.1"
  port: 3001
  accessToken: ""  # 如果设置了访问令牌

# 管理员 QQ 号
admin: 123456789
```

### data.yml 示例

```yaml
# 动态订阅数据
dynamic:
  # UID: 订阅信息
  123456:
    name: "用户名"
    contacts:
      - "group:987654321"  # 群聊
      - "private:123456789"  # 私聊
    banList: {}

# 检查配置
checkConfig:
  interval: 60  # 检查间隔（秒）

# 推送配置
pushConfig:
  toShortLink: false  # 是否使用短链接
  pushInterval: 1000  # 推送间隔（毫秒）
  messageInterval: 500  # 消息间隔（毫秒）

# 链接解析配置
linkResolveConfig:
  triggerMode: "Always"  # 触发模式：Always/At/Never
  returnLink: true  # 是否返回链接
```

## 开发说明

### 技术栈
- Kotlin 1.9.22
- Ktor 2.3.7（HTTP 客户端）
- Skiko 0.7.93（图片渲染）
- kotlinx.serialization（JSON 处理）
- kotlinx.coroutines（协程）
- OneBot v11 协议（NapCat）

### 项目特点
- 独立运行，不依赖 Mirai 框架
- 使用 NapCat 作为 QQ 机器人框架
- 基于 WebSocket 通信
- 使用 Skiko 进行高质量图片渲染
- 支持 Docker 部署

## 与原项目的区别

1. **移除了以下内容**：
   - Mirai 框架相关代码和测试
   - 编译输出（build/、.gradle/）
   - IDE 配置文件（.idea/、.kotlin/）
   - 临时文件和运行时数据

2. **保留了以下内容**：
   - 完整的源代码
   - Gradle 构建配置
   - Docker 部署文件
   - 文档和示例配置

## 更新日志

### 最新改进

1. **链接解析增强**
   - 添加了 QQ 小程序链接解析支持
   - 短链接自动转换为标准 BV 号
   - 优化了无限循环防护机制

2. **番剧渲染优化**
   - 添加了番剧介绍显示
   - 新增元信息显示（标签、首播时间、完结状态、集数等）
   - 优化了布局，减少空白区域

3. **字体加载改进**
   - 支持从 resources 目录加载字体
   - 字体文件可以打包到 JAR 中
   - 无需手动下载字体文件

4. **订阅功能完善**
   - 添加了订阅管理命令
   - 支持手动触发检查（/check）
   - 优化了推送逻辑

## 许可证

本项目基于 AGPL-3.0 许可证开源。

## 联系方式

如有问题，请在 GitHub 上提交 Issue。
