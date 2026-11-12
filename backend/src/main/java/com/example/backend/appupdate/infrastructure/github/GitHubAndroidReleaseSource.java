package com.example.backend.appupdate.infrastructure.github;

import com.example.backend.appupdate.application.AndroidReleaseSource;
import com.example.backend.appupdate.domain.AndroidRelease;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/** 私有 GitHub Release 适配器。令牌只存在于后端环境变量，不返回给安卓客户端。 */
@Repository
public class GitHubAndroidReleaseSource implements AndroidReleaseSource {
    private static final String API = "https://api.github.com/repos/naledao/knowledge-interview";
    private static final Pattern TAG = Pattern.compile("android-v1\\.0\\.([0-9]+)");
    private final RestClient client = RestClient.create();
    private final String token;

    public GitHubAndroidReleaseSource(@Value("${app.update.github-token:}") String token) {
        this.token = token;
    }

    private String token() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("服务器尚未配置 GITHUB_RELEASE_TOKEN");
        }
        return token;
    }

    /** 只选择非草稿、非预发布的 Android 标签，并使用版本最高的 APK。 */
    @Override
    public AndroidRelease latest() {
        List<?> releases = client.get().uri(API + "/releases?per_page=50")
                .header("Authorization", "Bearer " + token())
                .header("Accept", "application/vnd.github+json")
                .retrieve().body(List.class);
        AndroidRelease latest = null;
        if (releases != null) {
            for (Object item : releases) {
                if (!(item instanceof Map<?, ?> release)
                        || Boolean.TRUE.equals(release.get("draft"))
                        || Boolean.TRUE.equals(release.get("prerelease"))) continue;
                Matcher tag = TAG.matcher(String.valueOf(release.get("tag_name")));
                if (!tag.matches()) continue;
                int patch;
                try {
                    patch = Integer.parseInt(tag.group(1));
                } catch (NumberFormatException invalid) {
                    continue;
                }
                if (patch >= Integer.MAX_VALUE - 1) continue;
                String version = "1.0." + patch;
                if (!(release.get("assets") instanceof List<?> assets)) continue;
                for (Object entry : assets) {
                    if (!(entry instanceof Map<?, ?> asset)
                            || !("KnowledgeInterview-v" + version + ".apk").equals(asset.get("name"))
                            || !"uploaded".equals(asset.get("state"))) continue;
                    String digest = String.valueOf(asset.get("digest"));
                    if (!digest.matches("sha256:[a-fA-F0-9]{64}")) continue;
                    if (!(asset.get("id") instanceof Number id)
                            || !(asset.get("size") instanceof Number size)) continue;
                    try {
                        AndroidRelease found = new AndroidRelease(version, patch + 1,
                                id.longValue(), size.longValue(), digest.substring(7));
                        if (latest == null || found.versionCode() > latest.versionCode()) latest = found;
                    } catch (IllegalArgumentException ignored) {
                        // 跳过无效的发布资产，不返回无校验信息的 APK。
                    }
                }
            }
        }
        if (latest == null) throw new IllegalStateException("GitHub 尚无可用的安卓正式发布");
        return latest;
    }

    /** 处理 GitHub API 的 200/302 下载响应，跳转到受信任 CDN 时不转发令牌。 */
    @Override
    public void writeApk(AndroidRelease release, OutputStream output) throws IOException {
        URL url = URI.create(API + "/releases/assets/" + release.assetId()).toURL();
        for (int redirects = 0; redirects < 5; redirects++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/octet-stream");
            if ("api.github.com".equalsIgnoreCase(url.getHost())) {
                connection.setRequestProperty("Authorization", "Bearer " + token());
            }
            try {
                int code = connection.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("GitHub 未返回下载跳转地址");
                    URI next = URI.create(url.toExternalForm()).resolve(location);
                    String host = next.getHost();
                    if (!"https".equalsIgnoreCase(next.getScheme()) || host == null
                            || !("github.com".equalsIgnoreCase(host)
                                    || "api.github.com".equalsIgnoreCase(host)
                                    || host.toLowerCase(java.util.Locale.ROOT).endsWith(".githubusercontent.com"))) {
                        throw new IOException("GitHub 下载跳转目标不可信");
                    }
                    url = next.toURL();
                    continue;
                }
                if (code != 200) throw new IOException("GitHub APK 下载失败：HTTP " + code);
                long length = 0;
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        length += count;
                        if (length > release.size()) throw new IOException("APK 超过发布声明大小");
                        output.write(buffer, 0, count);
                    }
                }
                if (length != release.size()) throw new IOException("APK 大小与发布信息不一致");
                output.flush();
                return;
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("GitHub 下载跳转次数过多");
    }
}
