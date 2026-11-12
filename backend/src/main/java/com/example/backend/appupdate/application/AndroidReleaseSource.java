package com.example.backend.appupdate.application;

import com.example.backend.appupdate.domain.AndroidRelease;
import java.io.IOException;
import java.io.OutputStream;

/** 发布源端口：由基础设施层接入私有 GitHub 仓库，应用层不感知访问令牌。 */
public interface AndroidReleaseSource {
    AndroidRelease latest();
    void writeApk(AndroidRelease release, OutputStream output) throws IOException;
}
