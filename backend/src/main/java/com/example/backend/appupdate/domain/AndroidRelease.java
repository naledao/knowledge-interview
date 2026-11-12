package com.example.backend.appupdate.domain;

/** GitHub 正式发布的安卓 APK 元数据，版本号必须与构建工作流保持一致。 */
public record AndroidRelease(String versionName, int versionCode, long assetId, long size, String sha256) {
    public AndroidRelease {
        if (versionName == null || !versionName.matches("1\\.0\\.[0-9]+") || versionCode < 1
                || assetId < 1 || size < 1 || size > 100L * 1024 * 1024
                || sha256 == null || !sha256.matches("[a-fA-F0-9]{64}")) {
            throw new IllegalArgumentException("GitHub 安卓发布信息不合法");
        }
    }
}
