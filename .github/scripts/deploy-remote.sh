#!/usr/bin/env bash
set -Eeuo pipefail

repo="${1:?缺少仓库名称}"
port="${2:-8080}"
if [[ ! "$repo" =~ ^[A-Za-z0-9._-]+$ ]] || [[ ! "$port" =~ ^[0-9]+$ ]] || (( 10#$port < 1 || 10#$port > 65535 )); then
  echo '仓库名称或服务端口不合法' >&2
  exit 1
fi

dir="/root/service/$repo"
unit="${repo}.service"
java_bin="$(command -v java || true)"
if [[ -z "$java_bin" ]] || ! command -v systemctl >/dev/null || ! command -v curl >/dev/null; then
  echo '服务器必须安装 Java 17+、systemd 和 curl' >&2
  exit 1
fi
if [[ ! -f "$dir/.env" ]]; then
  echo "缺少服务器环境配置：$dir/.env，请先配置 MYSQL_PASSWORD、MCP_API_KEY 等变量" >&2
  exit 1
fi
test -s "$dir/app.jar.next"

cat > "/etc/systemd/system/$unit" <<EOF
[Unit]
Description=$repo Spring Boot Service
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
WorkingDirectory=$dir
ExecStart=$java_bin -jar $dir/app.jar --server.port=$port
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

# 保存上一版本，以便新版本健康检查失败时回滚。
if [[ -f "$dir/app.jar" ]]; then
  cp -f "$dir/app.jar" "$dir/app.jar.previous"
fi
mv -f "$dir/app.jar.next" "$dir/app.jar"
systemctl daemon-reload
systemctl enable "$unit" >/dev/null
systemctl restart "$unit"

healthy=false
for ((i=0; i<30; i++)); do
  if systemctl is-active --quiet "$unit" && curl -fsS -o /dev/null --max-time 3 "http://127.0.0.1:$port/"; then
    healthy=true
    break
  fi
  sleep 2
done
if [[ "$healthy" != true ]]; then
  echo '部署后健康检查失败，尝试恢复上一版本' >&2
  if [[ -f "$dir/app.jar.previous" ]]; then
    cp -f "$dir/app.jar.previous" "$dir/app.jar"
    systemctl restart "$unit" || true
  else
    systemctl stop "$unit" || true
  fi
  systemctl status "$unit" --no-pager -l || true
  exit 1
fi
echo "部署成功：$repo，目录：$dir，服务：$unit"
