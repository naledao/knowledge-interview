#!/usr/bin/env bash
set -Eeuo pipefail

repo="${1:?缺少仓库名称}"
port="${2:-8080}"
if [[ ! "$repo" =~ ^[A-Za-z0-9._-]+$ ]] || [[ ! "$port" =~ ^[0-9]+$ ]] || (( 10#$port < 1 || 10#$port > 65535 )); then
  echo '仓库名称或服务端口不合法' >&2
  exit 1
fi

dir="/root/service/$repo"
jar="$dir/app.jar"
pidfile="$dir/app.pid"
logfile="$dir/app.log"

java_bin="$(command -v java || true)"
if [[ -z "$java_bin" ]] || ! command -v nohup >/dev/null || ! command -v curl >/dev/null; then
  echo '服务器必须安装 Java 17+、nohup 和 curl' >&2
  exit 1
fi
if [[ ! -f "$dir/.env" ]]; then
  echo "缺少服务器环境配置：$dir/.env" >&2
  exit 1
fi
if [[ ! -s "$jar.next" ]]; then
  echo '上传的 JAR 不存在或为空' >&2
  exit 1
fi

# 仅管理 PID 文件记录且命令行确实包含当前应用 JAR 的进程，避免误杀其他 Java 服务。
managed_pid() {
  local pid cmdline
  [[ -f "$pidfile" ]] || return 0
  pid="$(cat "$pidfile")"
  [[ "$pid" =~ ^[0-9]+$ ]] || return 0
  kill -0 "$pid" 2>/dev/null || return 0
  [[ -r "/proc/$pid/cmdline" ]] || return 0
  cmdline="$(tr '\0' ' ' < "/proc/$pid/cmdline")"
  if [[ "$cmdline" == *"$jar"* ]]; then
    printf '%s\n' "$pid"
  fi
}

stop_app() {
  local pid="$1" i
  [[ -n "$pid" ]] || return 0
  kill "$pid" 2>/dev/null || true
  for ((i=0; i<15; i++)); do
    kill -0 "$pid" 2>/dev/null || return 0
    sleep 1
  done
  kill -9 "$pid" 2>/dev/null || true
}

start_app() {
  # 关闭标准输入并重定向输出，确保 SSH 会话结束后 Java 进程继续运行。
  cd "$dir"
  nohup "$java_bin" -jar "$jar" "--server.port=$port" </dev/null >>"$logfile" 2>&1 &
  printf '%s\n' "$!" > "$pidfile"
}

check_health() {
  local pid="$1" i
  for ((i=0; i<30; i++)); do
    if kill -0 "$pid" 2>/dev/null && curl -fsS -o /dev/null --max-time 3 "http://127.0.0.1:$port/"; then
      sleep 1
      if kill -0 "$pid" 2>/dev/null; then
        return 0
      fi
    fi
    sleep 2
  done
  return 1
}

# 替换之前先备份，并停止旧版本，避免覆盖正在运行的 JAR。
if [[ -f "$jar" ]]; then
  cp -f "$jar" "$jar.previous"
fi
old_pid="$(managed_pid)"
stop_app "$old_pid"
rm -f "$pidfile"
mv -f "$jar.next" "$jar"
start_app
new_pid="$(cat "$pidfile")"

if check_health "$new_pid"; then
  echo "部署成功：$repo，PID：$new_pid，目录：$dir"
  exit 0
fi

echo '新版本健康检查失败，尝试回滚' >&2
stop_app "$(managed_pid)"
rm -f "$pidfile"
if [[ -f "$jar.previous" ]]; then
  cp -f "$jar.previous" "$jar"
  start_app
  rollback_pid="$(cat "$pidfile")"
  if check_health "$rollback_pid"; then
    echo '上一版本已恢复运行' >&2
  else
    echo "回滚版本启动失败，请检查 $logfile" >&2
  fi
else
  echo '没有可用的上一版本' >&2
fi
exit 1
