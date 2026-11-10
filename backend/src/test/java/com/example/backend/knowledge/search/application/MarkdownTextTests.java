package com.example.backend.knowledge.search.application;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownTextTests {
    /** 与前端 utils/plainText.ts 使用同一份样例，两边结果应完全一致。 */
    static final String SAMPLE = """
            ## 排查步骤
            1. 先确认进程所属的 **cgroup**：
            ```bash
            cat /proc/$PID/cgroup
            ```
            - `memory.max`：上限
            - [x] 查看 [官方文档](https://docs.kernel.org/admin-guide/cgroup-v2.html)
            > 注意：*不要*直接调大 ~~swap~~。
            | 文件 | 含义 |
            | --- | :---: |
            | oom_kill | 被 OOM 杀死的次数 |
            ---
            ![架构图](a.png) snake_case_name 保持不变
            """;

    static final String EXPECTED = "排查步骤 先确认进程所属的 cgroup： cat /proc/$PID/cgroup memory.max：上限 查看 官方文档 "
            + "注意：不要直接调大 swap。 文件 含义 oom_kill 被 OOM 杀死的次数 架构图 snake_case_name 保持不变";

    @Test
    void stripsMarkdownSyntax() {
        assertThat(MarkdownText.toPlainText(SAMPLE)).isEqualTo(EXPECTED);
    }

    @Test
    void handlesEmptyInput() {
        assertThat(MarkdownText.toPlainText(null)).isEmpty();
        assertThat(MarkdownText.toPlainText("  \n ")).isEmpty();
    }

    @Test
    void excerptUsesPlainText() {
        KnowledgeSearchCandidate candidate = new KnowledgeSearchCandidate(KnowledgeSearchKind.QUESTION, 1, "t",
                "## 原因\n**Redis** 是 `单线程` 模型", 1, 24, null);
        assertThat(KnowledgeSearchService.excerpt(candidate, List.of("redis"))).isEqualTo("原因 Redis 是 单线程 模型");
    }
}
