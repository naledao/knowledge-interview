package com.example.backend.appupdate.application;

import com.example.backend.appupdate.domain.AndroidRelease;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.stereotype.Service;

/** 安卓更新用例：只能下载当前最新正式发布，避免客户端任意指定 GitHub 资源。 */
@Service
public class AndroidUpdateService {
    private final AndroidReleaseSource source;

    public AndroidUpdateService(AndroidReleaseSource source) {
        this.source = source;
    }

    public AndroidRelease latest() {
        return source.latest();
    }

    public AndroidRelease requireLatest(String version) {
        AndroidRelease release = source.latest();
        if (!release.versionName().equals(version)) {
            throw new IllegalArgumentException("所请求的版本不是最新正式版，请重新检查更新");
        }
        return release;
    }

    public void writeApk(AndroidRelease release, OutputStream output) throws IOException {
        source.writeApk(release, output);
    }
}
