# Docker 部署快速指南

## 一键部署

### Linux / macOS

```bash
# 1. 赋予执行权限
chmod +x deploy.sh

# 2. 运行部署脚本
./deploy.sh

# 3. 按照提示选择部署模式
# 4. 编辑配置文件 config/config.yml，设置管理员 QQ 号
# 5. 在 QQ 中发送 /login 登录 BiliBili 账号
```

### Windows

```cmd
# 双击运行 deploy.bat
# 或在命令提示符中执行：
deploy.bat
```

## 手动部署

### 步骤 1: 构建 JAR 包

```bash
# Linux/macOS
./gradlew shadowJar -x test

# Windows
gradlew.bat shadowJar -x test
```

### 步骤 2: 准备配置文件

```bash
# 创建目录
mkdir -p config data temp logs

# 创建 config/bot.yml
cat > config/bot.yml <<EOF
napcat:
  host: "napcat"
  port: 3001
  accessToken: ""
  reconnectInterval: 5000
EOF

# 复制默认配置
cp src/main/resources/config.yml config/config.yml

# 编辑 config/config.yml，设置管理员 QQ 号
vim config/config.yml  # 或使用其他编辑器
```

### 步骤 3: 选择部署模式

#### 模式 A: 仅部署 BiliBili Bot（NapCat 在宿主机运行）

```bash
# 使用默认的 docker-compose.yml
docker-compose build
docker-compose up -d
```

**注意**: 需要修改 `config/bot.yml`：

```yaml
napcat:
  host: "host.docker.internal"  # Windows/macOS
  # host: "宿主机IP"              # Linux（或使用 host.docker.internal + extra_hosts）
  port: 3001
```

#### 模式 B: 同时部署 NapCat + BiliBili Bot（推荐）

```bash
# 使用完整示例配置
cp docker-compose.full-example.yml docker-compose.yml

# 编辑配置，设置 QQ 号等信息
vim docker-compose.yml

# 构建并启动
docker-compose build
docker-compose up -d
```

### 步骤 4: 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 仅查看 BiliBili Bot 日志
docker-compose logs -f dynamic-bot

# 仅查看 NapCat 日志（如果部署了）
docker-compose logs -f napcat
```

### 步骤 5: 登录 BiliBili

在 QQ 群或私聊中发送：
```
/login
```

扫描二维码即可登录。

## 常用命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose stop

# 重启
docker-compose restart

# 停止并删除容器
docker-compose down

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 进入容器
docker-compose exec dynamic-bot bash

# 重新构建
docker-compose build --no-cache
docker-compose up -d
```

## 目录结构

```
.
├── Dockerfile                    # Docker 镜像定义
├── docker-compose.yml            # Docker Compose 配置
├── docker-compose.full-example.yml  # 完整示例配置
├── deploy.sh                     # Linux/macOS 部署脚本
├── deploy.bat                    # Windows 部署脚本
├── config/                       # 配置文件目录
│   ├── bot.yml                  # NapCat 连接配置
│   ├── config.yml               # Bot 主配置
│   └── data.yml                 # 订阅数据（自动生成）
├── data/                         # 数据目录
│   └── cache/                   # 图片缓存
├── temp/                         # 临时文件
└── logs/                         # 日志文件
```

## 故障排查

### 容器无法启动

```bash
# 查看详细日志
docker-compose logs dynamic-bot

# 检查配置文件
cat config/bot.yml
cat config/config.yml

# 检查 JAR 文件
ls -la build/libs/
```

### 无法连接 NapCat

```bash
# 检查 NapCat 状态
docker-compose ps napcat

# 测试网络连通性
docker-compose exec dynamic-bot ping napcat
docker-compose exec dynamic-bot nc -zv napcat 3001

# 检查配置
cat config/bot.yml
```

### 重新构建

```bash
# 完全重新构建
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## 更多信息

详细部署文档请参考: [DOCKER-DEPLOY.md](DOCKER-DEPLOY.md)
