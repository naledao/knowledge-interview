package com.knowledgeinterview.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** 检查后端代理的私有 GitHub Release，并下载、校验 APK 后交给系统安装。 */
final class AppUpdater {
    private static final String TAG = "AppUpdater";
    private static final String UPDATE_PATH = "/api/app-updates";
    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final long MAX_APK_BYTES = 100L * 1024 * 1024;
    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean downloading = new AtomicBoolean();
    private volatile File pendingInstall;

    AppUpdater(Activity activity) {
        this.activity = activity;
    }

    /** 密钥仅从受信任的内置 WebView 顶层页面读取，不在 APK 中保存 GitHub Token。 */
    void check(WebView webView, boolean manual) {
        if (webView == null || webView.getUrl() == null
                || !webView.getUrl().startsWith("https://appassets.androidplatform.net/")) return;
        webView.evaluateJavascript("localStorage.getItem('knowledge-interview-api-key')", raw -> {
            String apiKey = "";
            try {
                apiKey = new JSONArray("[" + raw + "]").optString(0, "");
            } catch (Exception ignored) {
                // 用户尚未配置 API Key，后台检查静默跳过。
            }
            if (apiKey == null || apiKey.isBlank()) {
                if (manual) message("请先在密钥设置中保存 API Key");
                return;
            }
            final String key = apiKey;
            final String backend = activity.getSharedPreferences("backend_settings", Activity.MODE_PRIVATE)
                    .getString("backend_address", "http://frp.kangnasi.xyz:9881");
            executor.execute(() -> {
                try {
                    Latest latest = fetchLatest(backend, key);
                    long installed = installedVersion();
                    if (latest.code > installed) {
                        activity.runOnUiThread(() -> {
                            if (!activity.isDestroyed() && !activity.isFinishing()) askToUpdate(latest, backend, key);
                        });
                    } else if (manual) {
                        message("已经是最新版本（" + installedVersionName() + "）");
                    }
                } catch (Exception error) {
                    Log.w(TAG, "检查更新失败", error);
                    if (manual) message("检查更新失败，请确认服务器地址、API Key 和网络连接");
                }
            });
        });
    }

    private Latest fetchLatest(String backend, String key) throws Exception {
        HttpURLConnection connection = open(backend + UPDATE_PATH + "/latest", key);
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("更新接口 HTTP " + connection.getResponseCode());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (bytes.size() + n > 16384) throw new IllegalStateException("更新元数据过大");
                    bytes.write(buffer, 0, n);
                }
            }
            JSONObject json = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            Latest latest = new Latest(json.getString("versionName"), json.getLong("versionCode"),
                    json.getLong("size"), json.getString("sha256"));
            if (!latest.name.matches("1[.]0[.][0-9]+") || latest.code < 1
                    || latest.size < 1 || latest.size > MAX_APK_BYTES
                    || !latest.sha256.matches("[0-9a-fA-F]{64}")) {
                throw new IllegalStateException("更新元数据不合法");
            }
            return latest;
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String address, String key) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Authorization", "Bearer " + key);
        connection.setRequestProperty("Accept", "application/json, application/vnd.android.package-archive");
        return connection;
    }

    /** 仅提示用户安装，普通安卓应用不能绕过系统安装确认界面。 */
    private void askToUpdate(Latest latest, String backend, String key) {
        if (downloading.get()) return;
        new AlertDialog.Builder(activity)
                .setTitle("发现新版本 v" + latest.name)
                .setMessage("当前版本：" + installedVersionName() + "\n最新版本：" + latest.name + "\n\n是否下载并安装更新？")
                .setNegativeButton("稍后", null)
                .setPositiveButton("立即更新", (dialog, which) -> download(latest, backend, key))
                .show();
    }

    private void download(Latest latest, String backend, String key) {
        if (!downloading.compareAndSet(false, true)) return;
        cancelled.set(false);
        AlertDialog progress = new AlertDialog.Builder(activity)
                .setTitle("正在下载 v" + latest.name)
                .setMessage("正在下载并验证安装包…")
                .setNegativeButton("取消", (dialog, which) -> cancelled.set(true))
                .create();
        progress.setCancelable(false);
        progress.show();
        executor.execute(() -> {
            File staging = new File(activity.getCacheDir(), "app-update.part");
            try {
                downloadAndVerify(latest, backend, key, staging, progress);
                if (cancelled.get()) return;
                File apk = new File(activity.getCacheDir(), "app-update.apk");
                if (apk.exists() && !apk.delete()) throw new IllegalStateException("无法清理旧安装包");
                if (!staging.renameTo(apk)) throw new IllegalStateException("保存安装包失败");
                pendingInstall = apk;
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    if (!activity.isDestroyed()) installPending();
                });
            } catch (Exception error) {
                Log.w(TAG, "下载更新失败", error);
                if (!cancelled.get()) message("更新失败：" + error.getMessage());
            } finally {
                if (staging.exists()) staging.delete();
                downloading.set(false);
                activity.runOnUiThread(progress::dismiss);
            }
        });
    }

    /** 下载时校验文件长度和 SHA-256，安装前还必须比对应用包名与签名证书。 */
    private void downloadAndVerify(Latest latest, String backend, String key, File staging,
                                   AlertDialog progress) throws Exception {
        String url = backend + UPDATE_PATH + "/download?version=" + latest.name;
        HttpURLConnection connection = open(url, key);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long bytes = 0;
        int lastPercent = -1;
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("APK 下载 HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(staging)) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (cancelled.get()) return;
                    bytes += n;
                    if (bytes > latest.size || bytes > MAX_APK_BYTES) throw new IllegalStateException("安装包大小异常");
                    output.write(buffer, 0, n);
                    digest.update(buffer, 0, n);
                    int percent = (int) (bytes * 100 / latest.size);
                    if (percent != lastPercent && percent % 5 == 0) {
                        lastPercent = percent;
                        final int shown = percent;
                        activity.runOnUiThread(() -> progress.setMessage("已下载 " + shown + "%"));
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
        if (cancelled.get()) return;
        if (bytes != latest.size) throw new IllegalStateException("安装包未完整下载");
        StringBuilder sha = new StringBuilder(64);
        for (byte b : digest.digest()) sha.append(String.format(Locale.ROOT, "%02x", b & 255));
        if (!latest.sha256.equalsIgnoreCase(sha.toString())) throw new IllegalStateException("安装包 SHA-256 校验失败");
        verifyPackage(staging, latest.code);
    }

    private void verifyPackage(File file, long version) throws Exception {
        PackageManager manager = activity.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo archive = manager.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        PackageInfo current = manager.getPackageInfo(activity.getPackageName(), flags);
        if (archive == null || !activity.getPackageName().equals(archive.packageName)
                || code(archive) != version || version <= code(current)) {
            throw new IllegalStateException("安装包的应用标识或版本号不正确");
        }
        Signature[] expected = signatures(current);
        Signature[] actual = signatures(archive);
        if (expected == null || actual == null || expected.length != actual.length || expected.length == 0) {
            throw new IllegalStateException("无法验证安装包签名");
        }
        for (Signature signature : expected) {
            if (!Arrays.asList(actual).contains(signature)) {
                throw new IllegalStateException("安装包签名与当前应用不一致，无法覆盖安装");
            }
        }
    }

    private static Signature[] signatures(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            return info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners();
        }
        return info.signatures;
    }

    private static long code(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private long installedVersion() throws Exception {
        return code(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0));
    }

    private String installedVersionName() {
        try {
            return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception error) {
            return "未知";
        }
    }

    /** Android 8+ 需要用户单独授权安装未知来源应用，授权返回后继续系统安装。 */
    void installPending() {
        File apk = pendingInstall;
        if (apk == null || !apk.isFile()) return;
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity).setTitle("需要安装权限")
                    .setMessage("请允许面试知识库安装应用，返回后会继续打开系统安装界面。")
                    .setNegativeButton("取消", (d, w) -> pendingInstall = null)
                    .setPositiveButton("前往授权", (d, w) -> {
                        Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(settings);
                    }).show();
            return;
        }
        pendingInstall = null;
        Uri uri = Uri.parse("content://" + activity.getPackageName() + ".apkupdates/app-update.apk");
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, APK_MIME);
        install.setClipData(ClipData.newRawUri("更新安装包", uri));
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(install);
        } catch (Exception error) {
            Log.w(TAG, "无法启动系统安装器", error);
            message("无法打开系统安装器");
        }
    }

    private void message(String text) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    void shutdown() {
        cancelled.set(true);
        executor.shutdownNow();
    }

    private static final class Latest {
        final String name;
        final long code;
        final long size;
        final String sha256;

        Latest(String name, long code, long size, String sha256) {
            this.name = name;
            this.code = code;
            this.size = size;
            this.sha256 = sha256;
        }
    }
}
