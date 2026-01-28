# 安全注意事项

## 配置文件安全

### 敏感信息保护

`config/BiliConfig.yml` 包含以下敏感信息：
- **Bilibili Cookie** (SESSDATA, bili_jct)
- **百度翻译 API 密钥** (APP_ID, SECURITY_KEY)
- **NapCat WebSocket Token**

### 安全建议

1. **文件权限控制**
   ```bash
   chmod 600 config/BiliConfig.yml
   ```

2. **不要公开分享**
   - ❌ 不要将配置文件上传到 GitHub
   - ❌ 不要通过聊天工具发送配置文件
   - ❌ 不要在截图中包含配置内容

3. **Docker 部署安全**
   - 容器已配置为非 root 用户运行
   - 挂载 config 卷时确保宿主机权限正确：
     ```bash
     chmod 600 ./config/*.yml
     docker-compose up -d
     ```

4. **日志安全**
   - 程序已对敏感信息进行脱敏处理
   - 日志级别设置为 INFO 或以上，避免 DEBUG 输出过多信息

## 已修复的安全问题

### v1.4.2 (2026-01-28)

- ✅ 修复配置保存时敏感信息日志泄露 (CWE-532)
- ✅ 修复 BiliCookie toString() 敏感信息暴露
- ✅ Docker 容器改为非 root 用户运行

## 报告安全问题

如果您发现安全漏洞，请通过以下方式报告：
- 发送邮件到项目维护者（不要公开提 Issue）
- 描述漏洞详情和复现步骤
- 我们会在 48 小时内响应

## 定期安全检查

建议每月执行：
1. 检查日志文件是否包含敏感信息
2. 更新依赖到最新安全版本
3. 审查配置文件权限
