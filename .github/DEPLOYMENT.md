# GitHub Actions 自动部署说明

## 触发条件

向 `main` 分支推送或合并 PR 后自动构建部署，也可以在 Actions 页面手动运行 `Build and deploy`。

## 构建流程

1. 在 Ubuntu Runner 上安装 Java 17 和 Node.js 22。
2. 先用 Maven 打包后端（跳过 pom.xml 中仅支持 Windows 的 npm.cmd 构建任务）。
3. 在 frontend 目录执行 npm ci、npm run build。
4. 再执行 Maven package，将 frontend/dist 复制进 JAR 的 BOOT-INF/classes/static，前端由 Spring Boot 统一托管。
5. 使用 SSH 上传 JAR 并通过 systemd 重启服务。失败时尝试回滚到上一个 JAR。

## 必须配置的 GitHub Actions Secrets

进入仓库 Settings → Secrets and variables → Actions → New repository secret：

| 名称 | 用途 |
| --- | --- |
| DEPLOY_HOST | 服务器公网 IP 或域名 |
| DEPLOY_USER | SSH 登录用户名；当前脚本要求 root，因为部署目录在 /root 下 |
| DEPLOY_PORT | SSH 端口（可不设置，默认 22） |
| DEPLOY_SSH_KEY | 部署专用 SSH 私钥全文（含 BEGIN/END 行），不要提交到 Git |
| DEPLOY_KNOWN_HOSTS | 服务器经过人工确认指纹的 SSH known_hosts 条目，防止中间人攻击 |

可选 Repository Variable：`DEPLOY_APP_PORT`，默认 `8080`；会作为 Spring Boot 的 `server.port` 并用于健康检查。

## 服务器准备

服务器需要安装 Java 17+、systemd 和 curl，并允许 SSH 登录。建议单独创建部署密钥，限制密钥访问权限。root SSH 有较高风险；如果生产环境不允许 root 登录，应改为非 root 部署目录及相应 systemd 授权方案。

提前创建 `/root/service/knowledge-interview/.env`，至少提供以下变量（示例值仅作占位，不可直接使用）：

```properties
MYSQL_URL=jdbc:mysql://127.0.0.1:3306/knowledge_interview?serverTimezone=Asia/Shanghai
MYSQL_USERNAME=your_user
MYSQL_PASSWORD=your_database_password
MCP_API_KEY=your_long_random_api_key
```

将 `.env` 权限限制为 `600`。`.env` 只保留在服务器，不由 GitHub Actions 上传或覆盖。

在可信环境中通过 `ssh-keyscan -p <SSH端口> <服务器域名或IP>` 获取主机公钥条目，并与服务器控制台查看到的主机密钥指纹独立核对后，保存至 `DEPLOY_KNOWN_HOSTS`；不要未经核对就信任扫描结果。

## 运行结果

- 部署位置：`/root/service/knowledge-interview/app.jar`
- 上一版本备份：`/root/service/knowledge-interview/app.jar.previous`
- systemd 服务：`knowledge-interview.service`
- 查看日志：`journalctl -u knowledge-interview.service -n 100 --no-pager`
- 查看状态：`systemctl status knowledge-interview.service`

**注意：** 在 Secrets 配置完成、服务器准备好之前，工作流的部署阶段会失败。工作流文件创建不代表已执行部署。
