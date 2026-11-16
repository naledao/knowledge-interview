package com.knowledgeinterview.android;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.nekohasekai.libbox.BridgeOptions;
import io.nekohasekai.libbox.BridgeSession;
import io.nekohasekai.libbox.CommandServer;
import io.nekohasekai.libbox.CommandServerHandler;
import io.nekohasekai.libbox.ConnectionOwner;
import io.nekohasekai.libbox.InterfaceUpdateListener;
import io.nekohasekai.libbox.Libbox;
import io.nekohasekai.libbox.LocalDNSTransport;
import io.nekohasekai.libbox.NeighborUpdateListener;
import io.nekohasekai.libbox.NetworkInterfaceIterator;
import io.nekohasekai.libbox.Notification;
import io.nekohasekai.libbox.OverrideOptions;
import io.nekohasekai.libbox.PlatformInterface;
import io.nekohasekai.libbox.PlatformUser;
import io.nekohasekai.libbox.RoutePrefix;
import io.nekohasekai.libbox.RoutePrefixIterator;
import io.nekohasekai.libbox.SetupOptions;
import io.nekohasekai.libbox.ShellSession;
import io.nekohasekai.libbox.StringIterator;
import io.nekohasekai.libbox.SystemProxyStatus;
import io.nekohasekai.libbox.TunOptions;
import io.nekohasekai.libbox.WIFIState;

public class SingBoxVpnService extends VpnService implements PlatformInterface, CommandServerHandler {
    private static final String TAG = "SingBoxService";
    private static final String CHANNEL_ID = "singbox_proxy_channel";
    private static final int NOTIFICATION_ID = 10086;

    public static final String ACTION_START = "com.knowledgeinterview.android.ACTION_START_SINGBOX";
    public static final String ACTION_STOP = "com.knowledgeinterview.android.ACTION_STOP_SINGBOX";
    public static final String EXTRA_CONFIG_JSON = "config_json";

    private static final AtomicBoolean LIBBOX_INITIALIZED = new AtomicBoolean(false);
    private static volatile boolean running = false;
    private static volatile String lastError = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CommandServer commandServer;
    private ParcelFileDescriptor tunFd;
    private String activeConfigJson;

    public static boolean isRunning() {
        return running;
    }

    public static String getLastError() {
        return lastError;
    }

    public static void ensureLibboxSetup(Context context) throws Exception {
        if (!LIBBOX_INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        try {
            File baseDir = new File(context.getFilesDir(), "sing-box");
            File workDir = new File(baseDir, "working");
            File tempDir = new File(context.getCacheDir(), "sing-box-tmp");
            if (!workDir.exists()) workDir.mkdirs();
            if (!tempDir.exists()) tempDir.mkdirs();

            SetupOptions options = new SetupOptions();
            options.setBasePath(baseDir.getAbsolutePath());
            options.setWorkingPath(workDir.getAbsolutePath());
            options.setTempPath(tempDir.getAbsolutePath());
            options.setFixAndroidStack(true);
            options.setDebug(false);
            options.setLogMaxLines(500);
            Libbox.setup(options);
        } catch (Exception e) {
            LIBBOX_INITIALIZED.set(false);
            throw e;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopCoreAndSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            String configJson = intent.getStringExtra(EXTRA_CONFIG_JSON);
            if (configJson == null || configJson.trim().isEmpty()) {
                lastError = "配置 JSON 为空";
                return START_NOT_STICKY;
            }
            startForegroundNotification();
            executor.execute(() -> startOrReloadCore(configJson));
        }
        return START_STICKY;
    }

    private void startForegroundNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "网络代理核心服务", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(this, CHANNEL_ID)
                : new android.app.Notification.Builder(this);
        android.app.Notification notification = builder
                .setContentTitle("KnowledgeInterview 网络服务")
                .setContentText("sing-box 核心运行中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private synchronized void startOrReloadCore(String configJson) {
        try {
            lastError = null;
            ensureLibboxSetup(getApplicationContext());
            Libbox.checkConfig(configJson);

            if (commandServer == null) {
                commandServer = Libbox.newCommandServer(this, this);
                commandServer.start();
            }
            OverrideOptions overrideOptions = new OverrideOptions();
            overrideOptions.setAutoRedirect(false);
            commandServer.startOrReloadService(configJson, overrideOptions);
            activeConfigJson = configJson;
            running = true;
            Log.i(TAG, "sing-box 核心启动成功");
        } catch (Exception e) {
            lastError = e.getMessage();
            Log.e(TAG, "sing-box 启动失败", e);
            running = false;
            stopCoreAndSelf();
        }
    }

    private void stopCoreAndSelf() {
        executor.execute(() -> {
            synchronized (SingBoxVpnService.this) {
                running = false;
                if (commandServer != null) {
                    try {
                        commandServer.closeService();
                    } catch (Exception e) {
                        Log.w(TAG, "关闭 sing-box 服务异常", e);
                    }
                    try {
                        commandServer.close();
                    } catch (Exception ignored) {
                    }
                    commandServer = null;
                }
                if (tunFd != null) {
                    try {
                        tunFd.close();
                    } catch (IOException ignored) {
                    }
                    tunFd = null;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
        });
    }

    @Override
    public void onRevoke() {
        stopCoreAndSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean usePlatformAutoDetectInterfaceControl() {
        return true;
    }

    @Override
    public void autoDetectInterfaceControl(int fd) {
        protect(fd);
    }

    @Override
    public int openTun(TunOptions options) throws Exception {
        Builder builder = new Builder()
                .setSession("KnowledgeInterview")
                .setMtu(options.getMTU() > 0 ? options.getMTU() : 1500);

        boolean hasAddress = false;
        RoutePrefixIterator inet4Addr = options.getInet4Address();
        if (inet4Addr != null) {
            while (inet4Addr.hasNext()) {
                RoutePrefix prefix = inet4Addr.next();
                builder.addAddress(prefix.address(), prefix.prefix());
                hasAddress = true;
            }
        }
        RoutePrefixIterator inet6Addr = options.getInet6Address();
        if (inet6Addr != null) {
            while (inet6Addr.hasNext()) {
                RoutePrefix prefix = inet6Addr.next();
                builder.addAddress(prefix.address(), prefix.prefix());
                hasAddress = true;
            }
        }
        if (!hasAddress) {
            builder.addAddress("172.19.0.1", 30);
        }
        if (options.getAutoRoute()) {
            builder.addRoute("0.0.0.0", 0);
            builder.addRoute("::", 0);
        }
        boolean hasDns = false;
        try {
            StringIterator dnsIter = options.getDNSServerAddress();
            if (dnsIter != null) {
                while (dnsIter.hasNext()) {
                    String dns = dnsIter.next();
                    if (dns != null && !dns.isEmpty()) {
                        builder.addDnsServer(dns);
                        hasDns = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!hasDns) {
            builder.addDnsServer("172.19.0.2");
        }
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (Exception ignored) {
        }

        ParcelFileDescriptor pfd = builder.establish();
        if (pfd == null) {
            throw new IllegalStateException("VpnService.Builder.establish() 返回 null，请先通过 VpnService.prepare() 授权");
        }
        tunFd = pfd;
        return pfd.detachFd();
    }

    @Override
    public void startDefaultInterfaceMonitor(InterfaceUpdateListener listener) {
    }

    @Override
    public void closeDefaultInterfaceMonitor(InterfaceUpdateListener listener) {
    }

    @Override
    public NetworkInterfaceIterator getInterfaces() {
        return null;
    }

    @Override
    public boolean underNetworkExtension() {
        return false;
    }

    @Override
    public boolean includeAllNetworks() {
        return false;
    }

    @Override
    public void clearDNSCache() {
    }

    @Override
    public WIFIState readWIFIState() {
        return null;
    }

    @Override
    public LocalDNSTransport localDNSTransport() {
        return null;
    }

    @Override
    public ConnectionOwner findConnectionOwner(
            int ipProtocol, String sourceAddress, int sourcePort,
            String destinationAddress, int destinationPort) {
        return null;
    }

    @Override
    public void sendNotification(Notification notification) {
    }

    @Override
    public void cancelNotification(String identifier, int id) {
    }

    @Override
    public void registerMyInterface(String name) {
    }

    @Override
    public boolean usePlatformBridge() {
        return false;
    }

    @Override
    public BridgeSession createBridge(BridgeOptions options) throws Exception {
        throw new UnsupportedOperationException("Bridge not supported");
    }

    @Override
    public boolean usePlatformShell() {
        return false;
    }

    @Override
    public void checkPlatformShell() throws Exception {
        throw new UnsupportedOperationException("Platform shell not supported");
    }

    @Override
    public ShellSession openShellSession(
            PlatformUser user, String command, StringIterator args,
            String workingDir, int rows, int cols) throws Exception {
        throw new UnsupportedOperationException("Shell session not supported");
    }

    @Override
    public PlatformUser lookupUser(String username) throws Exception {
        throw new UnsupportedOperationException("lookupUser not supported");
    }

    @Override
    public String lookupSFTPServer() throws Exception {
        throw new UnsupportedOperationException("lookupSFTPServer not supported");
    }

    @Override
    public String readSystemSSHHostKey() throws Exception {
        throw new UnsupportedOperationException("readSystemSSHHostKey not supported");
    }

    @Override
    public String tailscaleHostname() {
        return "";
    }

    @Override
    public void startNeighborMonitor(NeighborUpdateListener listener) {
    }

    @Override
    public void closeNeighborMonitor(NeighborUpdateListener listener) {
    }

    @Override
    public void serviceReload() {
        if (activeConfigJson != null) {
            executor.execute(() -> startOrReloadCore(activeConfigJson));
        }
    }

    @Override
    public void serviceStop() {
        stopCoreAndSelf();
    }

    @Override
    public SystemProxyStatus getSystemProxyStatus() {
        return null;
    }

    @Override
    public void setSystemProxyEnabled(boolean enabled) {
    }

    @Override
    public void writeDebugMessage(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void triggerNativeCrash() {
    }

    @Override
    public int connectSSHAgent() {
        return -1;
    }
}