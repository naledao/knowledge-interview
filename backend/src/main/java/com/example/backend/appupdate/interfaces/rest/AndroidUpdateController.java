package com.example.backend.appupdate.interfaces.rest;

import com.example.backend.appupdate.application.AndroidUpdateService;
import com.example.backend.appupdate.domain.AndroidRelease;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 安卓更新 REST 接口，需要沿用现有 Bearer API Key 鉴权。 */
@RestController
@RequestMapping("/api/app-updates")
public class AndroidUpdateController {
    private final AndroidUpdateService service;

    public AndroidUpdateController(AndroidUpdateService service) {
        this.service = service;
    }

    /** 返回最新 APK 版本、文件大小及 SHA-256，客户端校验后才能安装。 */
    @GetMapping("/latest")
    public AndroidRelease latest() {
        return service.latest();
    }

    /** 仅代理最新版本的 APK，避免让客户端控制私有仓库资产编号。 */
    @GetMapping(value = "/download", produces = "application/vnd.android.package-archive")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String version) {
        AndroidRelease release = service.requireLatest(version);
        StreamingResponseBody body = output -> service.writeApk(release, output);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .contentLength(release.size())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=KnowledgeInterview-v" + release.versionName() + ".apk")
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail unavailable(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
