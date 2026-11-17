package com.example.backend.knowledge.search.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/** 搜索关键词数据库操作接口，使用 MyBatis 注解维护 SQL 与字段映射。 */
@Mapper
public interface SearchKeywordMapper {
    /** 一条语句完成“不存在则新增、存在则次数加一”，依赖 keyword 唯一键，并发下计数不丢失。 */
    @Insert("INSERT INTO knowledge_search_keyword (keyword, search_count, last_searched_at) VALUES (#{keyword}, 1, NOW()) "
            + "ON DUPLICATE KEY UPDATE search_count = search_count + 1, last_searched_at = NOW()")
    int upsertSearch(@Param("keyword") String keyword);

    /** 查询启用的热门词：置顶优先，置顶词按排序值升序，其余按搜索次数倒序。 */
    @Select("SELECT keyword, search_count, pinned, sort_order, enabled FROM knowledge_search_keyword "
            + "WHERE enabled = 1 ORDER BY pinned DESC, sort_order ASC, search_count DESC, id ASC LIMIT #{limit}")
    @Results(id = "searchKeywordResult", value = {
        @Result(column = "keyword", property = "keyword"),
        @Result(column = "search_count", property = "searchCount"),
        @Result(column = "pinned", property = "pinned"),
        @Result(column = "sort_order", property = "sortOrder"),
        @Result(column = "enabled", property = "enabled")
    })
    List<SearchKeywordRow> findHot(@Param("limit") int limit);
}
