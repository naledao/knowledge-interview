# GitHub Actions 自动部署说明

## 触发与构建

推送或合并到 `main` 后自动部署，也支持在 Actions 页面手动触发。

1. Maven 先打包 Spring Boot 后端（跳过原有 Windows 专用 npm.cmd 构建步骤）。
2. 执行 `npm ci` 和 `npm run build` 构建 Vue 前端。
3. 再次打包后端，将 `frontend/dist` 嵌入 JAR，由 Spring Boot 统一托管。
4. 通过 SSH + rsync 增量上传 JAR，以服务器上已部署的 `app.jar` 为基准。上传暂存于 `.deploy-upload/`，失败最多重试 5 次，续传数据保存在 `.rsync-partial/` 子目录。SHA-256 校验通过后才将产物交给部署脚本。部署脚本停止旧进程并通过 `nohup` 启动新进程，健康检查失败时尝试恢复上一版本。

首次部署没有基准文件，会上传完整 JAR。查看 `rsync` 日志中的 `Total bytes sent` 判断实际发送的字节数，进度百分比按完整 JAR 的大小计算。

## GitHub Actions Secrets

在仓库 Settings → Secrets and variables → Actions 配置：

| 名称 | 说明 |
| --- | --- |
| DEPLOY_HOST | 服务器地址，例如 frp.kangnasi.xyz |
| DEPLOY_USER | 当前方案要求 root |
| DEPLOY_PORT | SSH 端口，例如 2222；不设置默认 22 |
| DEPLOY_SSH_KEY | SSH 私钥全文，禁止提交到仓库 |
| DEPLOY_KNOWN_HOSTS | 核验指纹后的服务器 SSH 主机公钥条目 |

可选仓库变量 `DEPLOY_APP_PORT`，默认 `8080`，同时用于启动参数和健康检查。

## 服务器准备

需要 Java 17+、nohup、curl、rsync、SSH 服务以及应用所需的数据库等依赖，**不需要 systemd**。

提前创建 `/root/service/knowledge-interview/.env`，配置 Spring Boot 所需的 `MYSQL_PASSWORD`、`MCP_API_KEY` 等环境变量，权限建议设置为 `600`。该文件不会通过 GitHub Actions 覆盖。

部署产物：`/root/service/knowledge-interview/app.jar`

进程 PID：`/root/service/knowledge-interview/app.pid`

应用日志：`/root/service/knowledge-interview/app.log`

上一版本：`/root/service/knowledge-interview/app.jar.previous`

查看运行状态：`ps -p "$(cat /root/service/knowledge-interview/app.pid)" -o pid,args`

查看日志：`tail -n 100 /root/service/knowledge-interview/app.log`

**注意：** `nohup` 可以在 SSH 会话断开后维持进程，但不能在机器重启或进程异常退出后自动恢复。健康检查默认访问本机应用根路径 `/`。
