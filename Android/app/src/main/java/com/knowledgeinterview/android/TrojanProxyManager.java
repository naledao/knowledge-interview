package com.knowledgeinterview.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * 纯 Java 实现的 Trojan 客户端与本地 HTTP 代理桥接器：
 * - 无需 VpnService / sing-box / NDK，宿主机无需开启全局代理；
 * - 严格校验证书（insecure = false），支持 SNI；
 * - 默认预置 zq-洛杉矶 (38.49.42.56:9443) 节点参数。
 */
public final class TrojanProxyManager {
    private static final String TAG = "TrojanProxy";
    private static final String SETTINGS_FILE = "backend_settings";

    private static final String KEY_ENABLED = "trojan_enabled";
    private static final String KEY_SERVER = "trojan_server";
    private static final String KEY_PORT = "trojan_port";
    private static final String KEY_PASSWORD = "trojan_password";
    private static final String KEY_SNI = "trojan_sni";

    public static final String DEFAULT_SERVER = "38.49.42.56";
    public static final int DEFAULT_PORT = 9443;
    public static final String DEFAULT_PASSWORD = "7DIqmcuUa3KX2kXD7qGCvu88DIbrzGgnNiYJLwrRhBU";
    public static final String DEFAULT_SNI = "38.49.42.56";

    private static final Object LOCK = new Object();
    private static volatile boolean running = false;
    private static volatile int boundPort = 0;
    private static volatile String lastError = null;
    private static ServerSocket localServer;
    private static ExecutorService acceptExecutor;
    private static ExecutorService relayExecutor;

    public static final class Config {
        public final boolean enabled;
        public final String server;
        public final int port;
        public final String password;
        public final String sni;

        public Config(boolean enabled, String server, int port, String password, String sni) {
            this.enabled = enabled;
            this.server = server;
            this.port = port;
            this.password = password;
            this.sni = sni;
        }
    }

    private TrojanProxyManager() {}

    public static Config loadConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_ENABLED, true);
        String server = prefs.getString(KEY_SERVER, DEFAULT_SERVER);
        int port = prefs.getInt(KEY_PORT, DEFAULT_PORT);
        String password = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD);
        String sni = prefs.getString(KEY_SNI, DEFAULT_SNI);
        return new Config(enabled, server, port, password, sni);
    }

    public static void saveConfig(Context context, Config cfg) {
        context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, cfg.enabled)
                .putString(KEY_SERVER, cfg.server)
                .putInt(KEY_PORT, cfg.port)
                .putString(KEY_PASSWORD, cfg.password)
                .putString(KEY_SNI, cfg.sni)
                .apply();
    }

    public static boolean isRunning() {
        return running && boundPort > 0;
    }

    public static String getLastError() {
        return lastError;
    }

    public static void ensureStartedIfEnabled(Context context) {
        Config cfg = loadConfig(context);
        if (cfg.enabled) {
            start(cfg);
        }
    }

    public static void start(Config cfg) {
        synchronized (LOCK) {
            stopInternal();
            lastError = null;
            if (cfg.server == null || cfg.server.trim().isEmpty()
                    || cfg.port <= 0 || cfg.port > 65535
                    || cfg.password == null || cfg.password.isEmpty()) {
                lastError = "节点服务器、端口或密码不合法";
                return;
            }
            try {
                ServerSocket ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
                localServer = ss;
                boundPort = ss.getLocalPort();
                running = true;
                acceptExecutor = Executors.newSingleThreadExecutor();
                relayExecutor = Executors.newCachedThreadPool();
                final Config activeCfg = cfg;
                acceptExecutor.execute(() -> acceptLoop(ss, activeCfg));
                Log.i(TAG, "纯 Java Trojan 本地代理已启动，监听 127.0.0.1:" + boundPort);
            } catch (IOException e) {
                lastError = e.getMessage();
                Log.e(TAG, "启动本地 Trojan 代理失败", e);
                stopInternal();
            }
        }
    }

    public static void stop(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, false).apply();
        synchronized (LOCK) {
            stopInternal();
        }
    }

    private static void stopInternal() {
        running = false;
        boundPort = 0;
        if (localServer != null) {
            try { localServer.close(); } catch (IOException ignored) {}
            localServer = null;
        }
        if (acceptExecutor != null) {
            acceptExecutor.shutdownNow();
            acceptExecutor = null;
        }
        if (relayExecutor != null) {
            relayExecutor.shutdownNow();
            relayExecutor = null;
        }
    }

    /** 供 MainActivity.proxyApi 与 AppUpdater 统一打开网络连接。 */
    public static HttpURLConnection openConnection(Context context, URL url) throws IOException {
        Config cfg = loadConfig(context);
        if (cfg.enabled && !isRunning()) {
            start(cfg);
        }
        if (cfg.enabled && isRunning()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", boundPort));
            return (HttpURLConnection) url.openConnection(proxy);
        }
        return (HttpURLConnection) url.openConnection();
    }

    private static void acceptLoop(ServerSocket server, Config cfg) {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                client.setTcpNoDelay(true);
                ExecutorService pool = relayExecutor;
                if (pool != null && !pool.isShutdown()) {
                    pool.execute(() -> handleLocalHttpClient(client, cfg));
                } else {
                    client.close();
                }
            } catch (IOException e) {
                if (!server.isClosed()) {
                    Log.w(TAG, "接收本地连接异常", e);
                }
                break;
            }
        }
    }

    private static void handleLocalHttpClient(Socket client, Config cfg) {
        Socket upstream = null;
        try {
            client.setSoTimeout(15000);
            InputStream clientIn = client.getInputStream();
            OutputStream clientOut = client.getOutputStream();

            String requestLine = readLineCRLF(clientIn);
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 3) return;
            String method = parts[0];
            String target = parts[1];
            String version = parts[2];

            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
            String targetHost = null;
            int targetPort = "CONNECT".equalsIgnoreCase(method) ? 443 : 80;
            String pathAndQuery = target;

            if ("CONNECT".equalsIgnoreCase(method)) {
                int colon = target.lastIndexOf(':');
                if (colon > 0) {
                    targetHost = target.substring(0, colon);
                    targetPort = Integer.parseInt(target.substring(colon + 1));
                } else {
                    targetHost = target;
                }
                while (true) {
                    String line = readLineCRLF(clientIn);
                    if (line == null || line.isEmpty()) break;
                }
            } else {
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    URL u = new URL(target);
                    targetHost = u.getHost();
                    targetPort = u.getPort() > 0 ? u.getPort() : ("https".equalsIgnoreCase(u.getProtocol()) ? 443 : 80);
                    pathAndQuery = (u.getPath() == null || u.getPath().isEmpty() ? "/" : u.getPath())
                            + (u.getQuery() != null ? "?" + u.getQuery() : "");
                }
                headerBuf.write((method + " " + pathAndQuery + " " + version + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
                while (true) {
                    String line = readLineCRLF(clientIn);
                    if (line == null) return;
                    if (line.isEmpty()) {
                        headerBuf.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
                        break;
                    }
                    if (line.regionMatches(true, 0, "Proxy-Connection:", 0, 17)
                            || line.regionMatches(true, 0, "Proxy-Authorization:", 0, 20)) {
                        continue;
                    }
                    if (targetHost == null && line.regionMatches(true, 0, "Host:", 0, 5)) {
                        String hostVal = line.substring(5).trim();
                        int colon = hostVal.lastIndexOf(':');
                        if (colon > 0) {
                            targetHost = hostVal.substring(0, colon);
                            targetPort = Integer.parseInt(hostVal.substring(colon + 1));
                        } else {
                            targetHost = hostVal;
                        }
                    }
                    headerBuf.write((line + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
                }
            }

            if (targetHost == null || targetHost.isEmpty()) return;

            upstream = openTrojanTunnel(cfg, targetHost, targetPort);
            client.setSoTimeout(0);
            upstream.setSoTimeout(0);

            OutputStream upOut = upstream.getOutputStream();
            InputStream upIn = upstream.getInputStream();

            if ("CONNECT".equalsIgnoreCase(method)) {
                clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                clientOut.flush();
            } else {
                upOut.write(headerBuf.toByteArray());
                upOut.flush();
            }

            final Socket cSock = client;
            final Socket uSock = upstream;
            Thread t1 = new Thread(() -> pipe(clientIn, upOut, uSock));
            t1.setDaemon(true);
            t1.start();
            pipe(upIn, clientOut, cSock);
            t1.join(1000);
        } catch (Exception e) {
            lastError = e.getMessage();
            Log.w(TAG, "Trojan 请求转发失败: " + e.getMessage());
        } finally {
            closeQuietly(upstream);
            closeQuietly(client);
        }
    }

    /** 建立标准 TLS 连接（严格校验服务器证书）并发送 Trojan 协议握手头。 */
    private static Socket openTrojanTunnel(Config cfg, String destHost, int destPort) throws Exception {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket();
        sslSocket.connect(new InetSocketAddress(cfg.server.trim(), cfg.port), 6000);
        sslSocket.setSoTimeout(10000);
        sslSocket.setTcpNoDelay(true);

        SSLParameters params = sslSocket.getSSLParameters();
        String sniHost = (cfg.sni != null && !cfg.sni.trim().isEmpty()) ? cfg.sni.trim() : cfg.server.trim();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        if (!isIpAddress(sniHost)) {
            params.setServerNames(java.util.Collections.singletonList(new SNIHostName(sniHost)));
        }
        sslSocket.setSSLParameters(params);
        sslSocket.startHandshake();

        ByteArrayOutputStream handshake = new ByteArrayOutputStream();
        handshake.write(sha224Hex(cfg.password).getBytes(StandardCharsets.US_ASCII));
        handshake.write(new byte[]{'\r', '\n', 0x01});

        byte[] ipv4 = parseIpv4(destHost);
        if (ipv4 != null) {
            handshake.write(0x01);
            handshake.write(ipv4);
        } else {
            byte[] domainBytes = destHost.getBytes(StandardCharsets.US_ASCII);
            if (domainBytes.length > 255) throw new IllegalArgumentException("目标域名过长");
            handshake.write(0x03);
            handshake.write(domainBytes.length);
            handshake.write(domainBytes);
        }
        handshake.write((destPort >> 8) & 0xFF);
        handshake.write(destPort & 0xFF);
        handshake.write(new byte[]{'\r', '\n'});

        OutputStream out = sslSocket.getOutputStream();
        out.write(handshake.toByteArray());
        out.flush();
        return sslSocket;
    }

    private static String sha224Hex(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-224");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(56);
        for (byte b : digest) {
            sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static boolean isIpAddress(String host) {
        return parseIpv4(host) != null || host.contains(":");
    }

    private static byte[] parseIpv4(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) return null;
        byte[] out = new byte[4];
        try {
            for (int i = 0; i < 4; i++) {
                int v = Integer.parseInt(parts[i]);
                if (v < 0 || v > 255) return null;
                out[i] = (byte) v;
            }
            return out;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readLineCRLF(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buf.size() == 0) return null;
                throw new EOFException("Unexpected EOF");
            }
            if (prev == '\r' && b == '\n') {
                byte[] raw = buf.toByteArray();
                return new String(raw, 0, raw.length - 1, StandardCharsets.ISO_8859_1);
            }
            buf.write(b);
            prev = b;
        }
    }

    private static void pipe(InputStream in, OutputStream out, Socket targetToShutdown) {
        byte[] buf = new byte[16384];
        try {
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
        } finally {
            try { targetToShutdown.shutdownOutput(); } catch (Exception ignored) {}
        }
    }

    private static void closeQuietly(Socket s) {
        if (s != null) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }
}