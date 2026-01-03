# Docker 部署测试脚本
# 用于测试 dynamic-bot 的 Docker 部署

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  Dynamic Bot - Docker 部署测试" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查 Docker 是否安装
Write-Host "[1/6] 检查 Docker 环境..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "✓ Docker 已安装: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker 未安装或未启动！" -ForegroundColor Red
    Write-Host "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit 1
}

# 2. 检查 JAR 文件是否存在
Write-Host "`n[2/6] 检查编译产物..." -ForegroundColor Yellow
$jarFile = "build\libs\dynamic-bot-4.0.0-STANDALONE.jar"
if (Test-Path $jarFile) {
    $jarSize = (Get-Item $jarFile).Length / 1MB
    Write-Host "✓ JAR 文件存在: $jarFile ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green
} else {
    Write-Host "✗ JAR 文件不存在！" -ForegroundColor Red
    Write-Host "请先编译项目: .\gradlew.bat build -x test" -ForegroundColor Yellow
    exit 1
}

# 3. 检查 Docker 配置文件
Write-Host "`n[3/6] 检查 Docker 配置文件..." -ForegroundColor Yellow
if (Test-Path "Dockerfile") {
    Write-Host "✓ Dockerfile 存在" -ForegroundColor Green
} else {
    Write-Host "✗ Dockerfile 不存在！" -ForegroundColor Red
    exit 1
}

if (Test-Path "docker-compose.yml") {
    Write-Host "✓ docker-compose.yml 存在" -ForegroundColor Green
} else {
    Write-Host "✗ docker-compose.yml 不存在！" -ForegroundColor Red
    exit 1
}

# 4. 构建 Docker 镜像
Write-Host "`n[4/6] 构建 Docker 镜像..." -ForegroundColor Yellow
Write-Host "执行: docker compose build" -ForegroundColor Gray
docker compose build
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Docker 镜像构建成功" -ForegroundColor Green
} else {
    Write-Host "✗ Docker 镜像构建失败！" -ForegroundColor Red
    exit 1
}

# 5. 启动容器
Write-Host "`n[5/6] 启动 Docker 容器..." -ForegroundColor Yellow
Write-Host "执行: docker compose up -d" -ForegroundColor Gray
docker compose up -d
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Docker 容器启动成功" -ForegroundColor Green
} else {
    Write-Host "✗ Docker 容器启动失败！" -ForegroundColor Red
    exit 1
}

# 等待容器启动
Write-Host "`n等待容器启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# 6. 检查容器状态
Write-Host "`n[6/6] 检查容器状态..." -ForegroundColor Yellow
$containerStatus = docker compose ps
Write-Host $containerStatus

# 7. 查看日志
Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "  容器日志 (最后 30 行)" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
docker compose logs --tail=30 dynamic-bot

# 8. 提供后续操作提示
Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "  部署测试完成" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "后续操作:" -ForegroundColor Yellow
Write-Host "1. 查看实时日志:     docker compose logs -f dynamic-bot" -ForegroundColor White
Write-Host "2. 停止容器:         docker compose stop" -ForegroundColor White
Write-Host "3. 重启容器:         docker compose restart" -ForegroundColor White
Write-Host "4. 停止并删除容器:   docker compose down" -ForegroundColor White
Write-Host "5. 查看容器状态:     docker compose ps" -ForegroundColor White
Write-Host ""
Write-Host "配置说明:" -ForegroundColor Yellow
Write-Host "- 配置文件位置: config/" -ForegroundColor White
Write-Host "- 编辑 config/config.yml 设置管理员 QQ 号" -ForegroundColor White
Write-Host "- 编辑 config/bot.yml 配置 NapCat 连接信息" -ForegroundColor White
Write-Host "- 在 QQ 中发送 /login 登录 B站账号" -ForegroundColor White
Write-Host ""
