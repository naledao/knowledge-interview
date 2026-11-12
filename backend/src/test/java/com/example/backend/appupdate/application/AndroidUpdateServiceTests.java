package com.example.backend.appupdate.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.appupdate.domain.AndroidRelease;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 安卓更新用例：校验只允许下载当前最新正式版。 */
class AndroidUpdateServiceTests {
    private final AndroidReleaseSource source = Mockito.mock(AndroidReleaseSource.class);
    private final AndroidUpdateService service = new AndroidUpdateService(source);
    private final AndroidRelease latest = new AndroidRelease("1.0.2", 3, 738457794L,
            417103, "1d42be00d08f2bf687d463c7071c5312bfed2010472fa52e78be3f358cbe85a0");

    @Test
    void returnsReleaseMetadata() {
        when(source.latest()).thenReturn(latest);
        assertEquals(latest, service.latest());
    }

    @Test
    void onlyPermitsLatestVersionDownload() {
        when(source.latest()).thenReturn(latest);
        assertEquals(latest, service.requireLatest("1.0.2"));
        assertThrows(IllegalArgumentException.class, () -> service.requireLatest("1.0.1"));
    }

    @Test
    void delegatesBinaryStreamingToReleaseSource() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        service.writeApk(latest, buffer);
        verify(source).writeApk(latest, buffer);
    }

    @Test
    void rejectsInvalidMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new AndroidRelease("1.0.2", 3, 738457794L, 417103, "invalid-digest"));
        assertThrows(IllegalArgumentException.class,
                () -> new AndroidRelease("1.0.2", 3, 738457794L, 110L * 1024 * 1024, latest.sha256()));
    }
}
