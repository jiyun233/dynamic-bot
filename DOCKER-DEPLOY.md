# BiliBili 动态推送 Bot - Docker 部署文档

## 目录

- [系统要求](#系统要求)
- [快速开始](#快速开始)
- [详细部署步骤](#详细部署步骤)
- [配置说明](#配置说明)
- [常用命令](#常用命令)
- [故障排查](#故障排查)
- [进阶配置](#进阶配置)

---

## 系统要求

### 硬件要求
- CPU: 1 核心或以上
- 内存: 至少 512MB 可用内存（推荐 1GB）
- 磁盘: 至少 2GB 可用空间

### 软件要求
- Docker Engine 20.10 或更高版本
- Docker Compose 2.0 或更高版本
- 已部署并运行的 NapCat（OneBot v11 协议）

### 支持的操作系统
- Linux (x64)
- Windows (x64) with Docker Desktop
- macOS with Docker Desktop

---

## 快速开始

### 1. 克隆或下载项目

```bash
cd /path/to/your/workspace
# 如果已有项目，跳过此步骤
```

### 2. 构建 Docker 镜像

```bash
cd dynamic-bot

# 首先编译 JAR 包
./gradlew shadowJar -x test

# 使用 Docker Compose 构建镜像
docker-compose build
```

### 3. 准备配置文件

创建必要的目录和配置文件：

```bash
mkdir -p config data temp logs
```

创建 `config/bot.yml` 文件：

```yaml
napcat:
  host: "127.0.0.1"      # NapCat WebSocket 服务器地址
  port: 3001              # NapCat WebSocket 端口
  accessToken: ""         # NapCat 访问令牌（如果设置了的话）
  reconnectInterval: 5000 # 重连间隔（毫秒）
```

创建 `config/config.yml` 文件（从项目中复制默认配置）：

```bash
# Linux/macOS
cp src/main/resources/config.yml config/config.yml

# Windows PowerShell
Copy-Item src\main\resources\config.yml config\config.yml
```

然后编辑 `config/config.yml`，设置管理员 QQ 号：

```yaml
admin: 123456789  # 替换为您的 QQ 号
```

### 4. 启动容器

```bash
docker-compose up -d
```

### 5. 查看日志

```bash
docker-compose logs -f
```

### 6. 登录 BiliBili 账号

在 QQ 群或私聊中发送：

```
/login
```

或

```
登录
```

Bot 会发送二维码，使用 BiliBili 手机 APP 扫码登录。

---

## 详细部署步骤

### 步骤 1: 准备 NapCat 环境

#### 方案 A: NapCat 在宿主机上运行

1. 确保 NapCat 已启动并监听 WebSocket 端口（默认 3001）
2. 在 `config/bot.yml` 中配置 NapCat 地址：

```yaml
napcat:
  host: "host.docker.internal"  # Docker 访问宿主机的特殊地址
  port: 3001
```

如果在 Linux 上运行，需要在 `docker-compose.yml` 中添加：

```yaml
services:
  bilibili-bot:
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

#### 方案 B: NapCat 也在 Docker 中运行（推荐）

修改 `docker-compose.yml`，取消注释 NapCat 相关配置：

```yaml
version: '3.8'

services:
  napcat:
    image: mlikiowa/napcat-docker:latest  # 或其他 NapCat 镜像
    container_name: napcat
    restart: unless-stopped
    ports:
      - "3001:3001"
      - "6099:6099"  # QQ 登录端口
    volumes:
      - ./napcat-config:/app/napcat/config
    networks:
      - bot-network

  bilibili-bot:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: dynamic-bot
    restart: unless-stopped
    environment:
      - TZ=Asia/Shanghai
      - JAVA_OPTS=-Xms256m -Xmx512m
    volumes:
      - ./config:/app/config
      - ./data:/app/data
      - ./temp:/app/temp
      - ./logs:/app/logs
    depends_on:
      - napcat
    networks:
      - bot-network

networks:
  bot-network:
    driver: bridge
```

然后在 `config/bot.yml` 中配置：

```yaml
napcat:
  host: "napcat"  # 使用服务名称
  port: 3001
```

### 步骤 2: 构建镜像

```bash
# 确保 JAR 包已构建
./gradlew shadowJar -x test

# 构建 Docker 镜像
docker-compose build

# 查看构建的镜像
docker images | grep bilibili
```

### 步骤 3: 配置文件详解

#### config/bot.yml（必需）

```yaml
napcat:
  # NapCat WebSocket 服务器地址
  # - 宿主机运行: "host.docker.internal" (Windows/Mac) 或具体 IP
  # - Docker 运行: "napcat" (服务名)
  host: "napcat"

  # NapCat WebSocket 端口
  port: 3001

  # 访问令牌（如果 NapCat 配置了 access_token）
  accessToken: ""

  # 重连间隔（毫秒）
  reconnectInterval: 5000
```

#### config/config.yml（必需）

主配置文件，包含：
- `admin`: 管理员 QQ 号（必须设置）
- 图片质量、主题配置
- 链接解析触发模式
- 推送设置等

详细配置说明请参考项目主 README。

#### config/data.yml（自动生成）

保存订阅数据，首次运行后自动生成。

### 步骤 4: 启动服务

```bash
# 前台启动（查看日志）
docker-compose up

# 后台启动
docker-compose up -d

# 查看启动状态
docker-compose ps

# 查看实时日志
docker-compose logs -f bilibili-bot
```

### 步骤 5: 验证部署

1. 检查容器状态：

```bash
docker-compose ps
```

应该看到：

```
NAME                    STATUS              PORTS
dynamic-bot   Up 10 seconds
```

2. 查看日志确认连接：

```bash
docker-compose logs bilibili-bot | tail -20
```

应该看到类似输出：

```
[INFO] ========================================
[INFO]   BiliBili 动态推送 Bot
[INFO]   版本: 4.0.0-STANDALONE
[INFO] ========================================
[INFO] 正在加载配置...
[INFO] 正在初始化 NapCat 客户端...
[INFO] NapCat WebSocket 客户端已连接
[INFO] Bot 启动成功！
```

3. 测试功能：

在 QQ 群或私聊中发送 `/login`，应该收到二维码。

---

## 配置说明

### 目录结构

```
dynamic-bot/
├── Dockerfile                # Docker 镜像定义
├── docker-compose.yml        # Docker Compose 配置
├── .dockerignore            # Docker 构建排除文件
├── config/                  # 配置文件目录（挂载）
│   ├── bot.yml             # NapCat 连接配置
│   ├── config.yml          # Bot 主配置
│   └── data.yml            # 订阅数据（自动生成）
├── data/                    # 数据目录（挂载）
│   └── cache/              # 图片缓存
├── temp/                    # 临时文件目录（挂载）
└── logs/                    # 日志目录（挂载）
```

### 环境变量

在 `docker-compose.yml` 中可以配置以下环境变量：

```yaml
environment:
  # 时区设置
  - TZ=Asia/Shanghai

  # JVM 参数
  - JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
```

JVM 参数说明：
- `-Xms256m`: 初始堆内存 256MB
- `-Xmx512m`: 最大堆内存 512MB（可根据实际情况调整）
- `-XX:+UseG1GC`: 使用 G1 垃圾回收器

### 数据持久化

以下目录通过 Docker Volume 持久化，容器重启后数据不会丢失：

- `./config` → `/app/config`: 配置文件
- `./data` → `/app/data`: 缓存和数据
- `./temp` → `/app/temp`: 临时文件
- `./logs` → `/app/logs`: 日志文件

---

## 常用命令

### 容器管理

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose stop

# 重启服务
docker-compose restart

# 停止并删除容器
docker-compose down

# 停止并删除容器、镜像
docker-compose down --rmi all

# 查看容器状态
docker-compose ps

# 查看资源使用情况
docker stats dynamic-bot
```

### 日志管理

```bash
# 查看实时日志
docker-compose logs -f

# 查看最后 100 行日志
docker-compose logs --tail=100

# 查看特定时间的日志
docker-compose logs --since 2024-01-01T00:00:00

# 导出日志到文件
docker-compose logs > bot.log
```

### 配置更新

```bash
# 修改配置文件后重启容器
vim config/config.yml
docker-compose restart

# 如果修改了 Dockerfile 或 docker-compose.yml，需要重新构建
docker-compose down
docker-compose build
docker-compose up -d
```

### 进入容器调试

```bash
# 进入容器 Shell
docker-compose exec bilibili-bot bash

# 查看容器内文件
docker-compose exec bilibili-bot ls -la /app

# 查看配置文件
docker-compose exec bilibili-bot cat /app/config/config.yml
```

### 备份与恢复

```bash
# 备份配置和数据
tar -czf bilibili-bot-backup-$(date +%Y%m%d).tar.gz config/ data/

# 恢复备份
tar -xzf bilibili-bot-backup-20240101.tar.gz
```

---

## 故障排查

### 问题 1: 容器无法启动

**症状**: `docker-compose up` 后容器立即退出

**排查步骤**:

```bash
# 查看容器日志
docker-compose logs bilibili-bot

# 查看容器退出原因
docker-compose ps -a
```

**常见原因**:
1. JAR 包未构建或路径错误
   ```bash
   # 重新构建 JAR
   ./gradlew clean shadowJar -x test
   ls -la build/libs/
   ```

2. 配置文件缺失或格式错误
   ```bash
   # 检查配置文件
   cat config/bot.yml
   cat config/config.yml
   ```

### 问题 2: 无法连接到 NapCat

**症状**: 日志显示 "Connection refused" 或 "Unable to connect"

**排查步骤**:

1. 检查 NapCat 是否正在运行：
   ```bash
   # 如果 NapCat 在宿主机
   netstat -an | grep 3001

   # 如果 NapCat 在 Docker
   docker-compose ps napcat
   ```

2. 检查网络连通性：
   ```bash
   # 从容器内测试连接
   docker-compose exec bilibili-bot ping napcat

   # 测试端口
   docker-compose exec bilibili-bot nc -zv napcat 3001
   ```

3. 检查 `config/bot.yml` 中的地址配置

**解决方案**:
- 宿主机运行的 NapCat: 使用 `host.docker.internal`
- Docker 运行的 NapCat: 使用服务名 `napcat`
- 确保网络模式正确配置

### 问题 3: 图片无法发送

**症状**: 链接解析后无法发送图片

**排查步骤**:

```bash
# 检查临时目录权限
docker-compose exec bilibili-bot ls -la /app/temp

# 检查数据目录
docker-compose exec bilibili-bot ls -la /app/data/cache
```

**解决方案**:

```bash
# 确保目录权限正确
chmod -R 777 temp/ data/

# 重启容器
docker-compose restart
```

### 问题 4: 内存不足

**症状**: 容器被 OOM (Out of Memory) 杀死

**排查步骤**:

```bash
# 查看容器资源使用
docker stats dynamic-bot

# 查看系统日志
dmesg | grep -i oom
```

**解决方案**:

修改 `docker-compose.yml`，增加内存限制和 JVM 堆内存：

```yaml
services:
  bilibili-bot:
    environment:
      - JAVA_OPTS=-Xms256m -Xmx1024m -XX:+UseG1GC
    deploy:
      resources:
        limits:
          memory: 1.5G
        reservations:
          memory: 512M
```

### 问题 5: 字体缺失导致图片生成失败

**症状**: 日志显示字体加载失败

**解决方案**:

重新构建镜像，确保字体已安装：

```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

---

## 进阶配置

### 1. 使用外部数据库

如果需要使用外部数据库（未来扩展），可以在 `docker-compose.yml` 中添加：

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: bilibili-db
    environment:
      POSTGRES_DB: bilibili
      POSTGRES_USER: bilibili
      POSTGRES_PASSWORD: your_password
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - bot-network

volumes:
  postgres-data:
```

### 2. 使用 Nginx 反向代理

如果需要通过 Web 访问（未来功能），可以添加 Nginx：

```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: bilibili-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - bilibili-bot
    networks:
      - bot-network
```

### 3. 监控和告警

使用 Prometheus + Grafana 监控（需要后续集成）：

```yaml
services:
  prometheus:
    image: prom/prometheus
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    networks:
      - bot-network

  grafana:
    image: grafana/grafana
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - bot-network

volumes:
  prometheus-data:
  grafana-data:
```

### 4. 自动更新

使用 Watchtower 自动更新镜像：

```yaml
services:
  watchtower:
    image: containrrr/watchtower
    container_name: watchtower
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    command: --interval 86400  # 每天检查一次
```

### 5. 日志收集

使用 Loki + Promtail 收集日志：

```yaml
services:
  loki:
    image: grafana/loki
    container_name: loki
    ports:
      - "3100:3100"
    networks:
      - bot-network

  promtail:
    image: grafana/promtail
    container_name: promtail
    volumes:
      - ./logs:/var/log
      - ./promtail-config.yml:/etc/promtail/config.yml
    networks:
      - bot-network
```

### 6. 多实例部署

如果需要部署多个 Bot 实例：

```yaml
services:
  bilibili-bot-1:
    build: .
    container_name: bilibili-bot-1
    volumes:
      - ./config-1:/app/config
      - ./data-1:/app/data
    networks:
      - bot-network

  bilibili-bot-2:
    build: .
    container_name: bilibili-bot-2
    volumes:
      - ./config-2:/app/config
      - ./data-2:/app/data
    networks:
      - bot-network
```

---

## 生产环境建议

### 1. 资源配置

根据订阅数量调整资源：

| 订阅用户数 | 推荐内存 | 推荐 CPU |
|----------|---------|---------|
| < 10     | 512MB   | 1 核心   |
| 10-50    | 1GB     | 2 核心   |
| 50-200   | 2GB     | 2 核心   |
| > 200    | 4GB     | 4 核心   |

### 2. 安全建议

1. 不要将敏感信息提交到 Git：
   ```bash
   # .gitignore
   config/bot.yml
   config/data.yml
   data/
   temp/
   logs/
   ```

2. 使用环境变量存储敏感信息：
   ```yaml
   environment:
     - NAPCAT_ACCESS_TOKEN=${NAPCAT_ACCESS_TOKEN}
   ```

3. 定期备份数据：
   ```bash
   # 添加到 crontab
   0 2 * * * cd /path/to/bot && tar -czf backup-$(date +\%Y\%m\%d).tar.gz config/ data/
   ```

### 3. 性能优化

1. 使用 SSD 存储
2. 启用 Docker BuildKit：
   ```bash
   export DOCKER_BUILDKIT=1
   docker-compose build
   ```

3. 优化镜像大小（多阶段构建，未来考虑）

### 4. 监控指标

重点监控：
- 容器 CPU/内存使用率
- WebSocket 连接状态
- 图片生成耗时
- 消息发送成功率

---

## 更新升级

### 升级 Bot 版本

```bash
# 1. 备份数据
tar -czf backup-$(date +%Y%m%d).tar.gz config/ data/

# 2. 拉取最新代码
git pull

# 3. 重新构建 JAR
./gradlew clean shadowJar -x test

# 4. 重新构建镜像
docker-compose build

# 5. 停止旧容器
docker-compose down

# 6. 启动新容器
docker-compose up -d

# 7. 查看日志确认
docker-compose logs -f
```

### 回滚版本

```bash
# 1. 停止当前容器
docker-compose down

# 2. 恢复备份
tar -xzf backup-20240101.tar.gz

# 3. 切换到旧版本代码
git checkout v3.x.x

# 4. 重新构建
./gradlew shadowJar -x test
docker-compose build
docker-compose up -d
```

---

## 常见问题 FAQ

### Q1: 如何查看 Bot 的 QQ 号？

A: 查看 NapCat 的日志或配置文件。

### Q2: 如何修改管理员 QQ 号？

A: 编辑 `config/config.yml`，修改 `admin` 字段，然后重启容器：

```bash
vim config/config.yml
docker-compose restart
```

### Q3: 如何清理缓存？

A: 删除 `data/cache` 目录：

```bash
rm -rf data/cache/*
docker-compose restart
```

### Q4: 如何完全重置 Bot？

A: 删除所有数据并重新启动：

```bash
docker-compose down
rm -rf data/* temp/* logs/*
docker-compose up -d
```

然后重新执行 `/login` 登录。

### Q5: 容器时区不正确？

A: 在 `docker-compose.yml` 中设置 `TZ` 环境变量：

```yaml
environment:
  - TZ=Asia/Shanghai
```

---

## 技术支持

- GitHub Issues: https://github.com/your-repo/issues
- 文档: https://github.com/your-repo/wiki

---

## 许可证

本项目遵循 AGPL-3.0 许可证。
