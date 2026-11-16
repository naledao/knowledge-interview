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

/** 直接从 GitHub Release 获取最新 Android 版本并下载、校验 APK 后交给系统安装。 */
final class AppUpdater {
    private static final String TAG = "AppUpdater";
    private static final String GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/naledao/knowledge-interview/releases/latest";
    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final long MAX_APK_BYTES = 100L * 1024 * 1024;
    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean downloading = new AtomicBoolean();
    private volatile File pendingInstall;
    private UpdateDialogs.Busy activeProgress;
    private final AtomicBoolean checking = new AtomicBoolean();
    private boolean manualCheckRequested;
    private UpdateDialogs.Busy activeCheckProgress;

    AppUpdater(Activity activity) {
        this.activity = activity;
    }

    /** 手动检查从点击开始显示加载动画；自动检查保持静默。 */
    void check(WebView webView, boolean manual) {
        if (webView == null || webView.getUrl() == null
                || !webView.getUrl().startsWith("https://appassets.androidplatform.net/")) {
            if (manual) message("页面尚未准备好，请稍后重试");
            return;
        }
        if (downloading.get()) {
            if (manual) message("正在下载更新，请稍候");
            return;
        }
        if (!checking.compareAndSet(false, true)) {
            if (manual) {
                manualCheckRequested = true;
                showChecking();
            }
            return;
        }
        manualCheckRequested = manual;
        if (manual) showChecking();
        if (activeCheckProgress != null) activeCheckProgress.update("正在查询 GitHub 最新版本…", -1);
        try {
            executor.execute(() -> {
                try {
                    Latest latest = fetchLatest();
                    long installed = installedVersion();
                    finishCheck(latest, installed, null);
                } catch (Exception error) {
                    Log.w(TAG, "检查更新失败", error);
                    finishCheck(null, 0, "检查更新失败：" + error.getMessage());
                }
            });
        } catch (RuntimeException error) {
            Log.w(TAG, "无法启动更新检查", error);
            finishCheck(null, 0, "无法启动更新检查，请稍后重试");
        }
    }

    /** 进入检查流程时立即展示持续加载卡片，直到检查完成才关闭。 */
    private void showChecking() {
        if (activeCheckProgress != null || activity.isFinishing() || activity.isDestroyed()) return;
        activeCheckProgress = UpdateDialogs.busy(activity, "正在检查更新", "正在获取 GitHub 最新版本信息", null);
        activeCheckProgress.update("正在连接 GitHub Release…", -1);
    }

    /** 请求成功或失败时统一收尾，先关闭加载卡片再呈现结果。 */
    private void finishCheck(Latest latest, long installed, String errorMessage) {
        activity.runOnUiThread(() -> {
            boolean manual = manualCheckRequested;
            manualCheckRequested = false;
            checking.set(false);
            if (activeCheckProgress != null) {
                activeCheckProgress.dismiss();
                activeCheckProgress = null;
            }
            if (activity.isFinishing() || activity.isDestroyed()) return;
            if (errorMessage != null) {
                if (manual) message(errorMessage);
            } else if (latest != null && latest.code > installed) {
                askToUpdate(latest);
            } else if (manual) {
                message("已经是最新版本（" + installedVersionName() + "）");
            }
        });
    }

    private Latest fetchLatest() throws Exception {
        HttpURLConnection connection = openFollowingRedirects(activity, GITHUB_LATEST_RELEASE_API, "application/vnd.github+json");
        try {
            int status = connection.getResponseCode();
            if (status != 200) {
                throw new IllegalStateException("GitHub Release 接口 HTTP " + status);
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (bytes.size() + n > 262144) throw new IllegalStateException("Release 元数据过大");
                    bytes.write(buffer, 0, n);
                }
            }
            JSONObject json = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            String tagName = json.optString("tag_name", "");
            if (!tagName.matches("android-v1[.]0[.][0-9]+")) {
                throw new IllegalStateException("未找到有效的 Android Release 标签: " + tagName);
            }
            String versionName = tagName.substring("android-v".length());
            int patch = Integer.parseInt(versionName.substring("1.0.".length()));
            long versionCode = patch + 1L;

            String expectedAssetName = "KnowledgeInterview-v" + versionName + ".apk";
            JSONArray assets = json.optJSONArray("assets");
            String downloadUrl = null;
            long size = -1;
            String sha256 = null;
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) continue;
                    String assetName = asset.optString("name", "");
                    if (expectedAssetName.equals(assetName) || assetName.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "");
                        size = asset.optLong("size", -1);
                        String digest = asset.optString("digest", "");
                        if (digest.startsWith("sha256:")) {
                            sha256 = digest.substring("sha256:".length()).trim();
                        }
                        if (expectedAssetName.equals(assetName)) break;
                    }
                }
            }
            if (downloadUrl == null || !downloadUrl.startsWith("https://") || size < 1 || size > MAX_APK_BYTES) {
                throw new IllegalStateException("Release 中缺少有效的 APK 安装包资产");
            }
            return new Latest(versionName, versionCode, size, sha256, downloadUrl);
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openFollowingRedirects(Activity activity, String startUrl, String accept) throws Exception {
        String currentUrl = startUrl;
        for (int redirects = 0; redirects < 6; redirects++) {
            URL url = new URL(currentUrl);
            HttpURLConnection connection = TrojanProxyManager.openConnection(activity, url);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "KnowledgeInterviewAndroid/1.0");
            if (accept != null && !accept.isEmpty()) {
                connection.setRequestProperty("Accept", accept);
            }
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307 || code == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IllegalStateException("GitHub 下载重定向缺少 Location");
                }
                currentUrl = new URL(url, location).toString();
                continue;
            }
            return connection;
        }
        throw new IllegalStateException("GitHub 下载重定向次数过多");
    }

    /** 使用与应用一致的蓝白圆角卡片提示更新。 */
    private void askToUpdate(Latest latest) {
        if (downloading.get()) return;
        UpdateDialogs.confirm(activity, installedVersionName(), latest.name,
                () -> download(latest));
    }

    /** 用户确认后持续显示加载状态，直到网络下载和安装包校验真正结束。 */
    private void download(Latest latest) {
        if (!downloading.compareAndSet(false, true)) return;
        cancelled.set(false);
        UpdateDialogs.Busy progress = UpdateDialogs.busy(activity,
                "正在更新 v" + latest.name, "从 GitHub Release 下载并校验安装包", () -> cancelled.set(true));
        activeProgress = progress;
        progress.update("正在连接 GitHub 下载服务器…", -1);
        executor.execute(() -> {
            File staging = new File(activity.getCacheDir(), "app-update.part");
            try {
                downloadAndVerify(latest, staging, progress);
                if (cancelled.get()) return;
                File apk = new File(activity.getCacheDir(), "app-update.apk");
                if (apk.exists() && !apk.delete()) throw new IllegalStateException("无法清理旧安装包");
                if (!staging.renameTo(apk)) throw new IllegalStateException("保存安装包失败");
                pendingInstall = apk;
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    if (cancelled.get()) {
                        pendingInstall = null;
                    } else if (!activity.isDestroyed() && !activity.isFinishing()) {
                        installPending();
                    }
                });
            } catch (Exception error) {
                Log.w(TAG, "下载更新失败", error);
                if (!cancelled.get()) message("更新失败：" + error.getMessage());
            } finally {
                if (staging.exists()) staging.delete();
                downloading.set(false);
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    if (activeProgress == progress) activeProgress = null;
                });
            }
        });
    }

    /** 下载时校验文件长度和 SHA-256（若提供），安装前还必须比对应用包名与签名证书。 */
    private void downloadAndVerify(Latest latest, File staging, UpdateDialogs.Busy progress) throws Exception {
        HttpURLConnection connection = openFollowingRedirects(activity, latest.downloadUrl, "application/octet-stream");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long bytes = 0;
        int lastPercent = -1;
        try {
            if (connection.getResponseCode() != 200) {
                throw new IllegalStateException("APK 下载 HTTP " + connection.getResponseCode());
            }
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
                    if (percent == 100 || percent >= lastPercent + 5) {
                        lastPercent = percent;
                        final int shown = percent;
                        activity.runOnUiThread(() -> progress.update("已下载 " + shown + "%", shown));
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
        if (cancelled.get()) return;
        activity.runOnUiThread(() -> progress.update("下载完成，正在校验安装包…", -1));
        if (bytes != latest.size) throw new IllegalStateException("安装包未完整下载");
        if (latest.sha256 != null && latest.sha256.matches("[0-9a-fA-F]{64}")) {
            StringBuilder sha = new StringBuilder(64);
            for (byte b : digest.digest()) sha.append(String.format(Locale.ROOT, "%02x", b & 255));
            if (!latest.sha256.equalsIgnoreCase(sha.toString())) {
                throw new IllegalStateException("安装包 SHA-256 校验失败");
            }
        }
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
        checking.set(false);
        manualCheckRequested = false;
        if (activeCheckProgress != null) {
            activeCheckProgress.dismiss();
            activeCheckProgress = null;
        }
        if (activeProgress != null) {
            activeProgress.dismiss();
            activeProgress = null;
        }
    }

    private static final class Latest {
        final String name;
        final long code;
        final long size;
        final String sha256;
        final String downloadUrl;

        Latest(String name, long code, long size, String sha256, String downloadUrl) {
            this.name = name;
            this.code = code;
            this.size = size;
            this.sha256 = sha256;
            this.downloadUrl = downloadUrl;
        }
    }
}