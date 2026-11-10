package com.example.backend.knowledge.search.infrastructure.persistence;

import com.example.backend.knowledge.search.application.KnowledgeSearchCandidate;
import com.example.backend.knowledge.search.application.KnowledgeSearchKind;

/** 搜索查询结果行，列别名与属性名一致，依赖 MyBatis 自动映射。 */
public class KnowledgeSearchRow {
    private Long id;
    private String title;
    private String body;
    private Integer bodyOffset;
    private Integer bodyLength;
    private String breadcrumb;

    public KnowledgeSearchCandidate toCandidate(KnowledgeSearchKind kind) {
        return new KnowledgeSearchCandidate(kind, id, title, body,
                bodyOffset == null ? 1 : bodyOffset, bodyLength == null ? 0 : bodyLength, breadcrumb);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Integer getBodyOffset() { return bodyOffset; }
    public void setBodyOffset(Integer bodyOffset) { this.bodyOffset = bodyOffset; }
    public Integer getBodyLength() { return bodyLength; }
    public void setBodyLength(Integer bodyLength) { this.bodyLength = bodyLength; }
    public String getBreadcrumb() { return breadcrumb; }
    public void setBreadcrumb(String breadcrumb) { this.breadcrumb = breadcrumb; }
}
