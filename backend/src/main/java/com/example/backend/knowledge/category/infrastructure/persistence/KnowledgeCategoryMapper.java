package com.example.backend.knowledge.category.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
/** 分类数据库操作接口，使用 MyBatis 注解维护 SQL 与字段映射。 */
public interface KnowledgeCategoryMapper {
    @Select("SELECT id, name, description, sort_order, created_at, updated_at FROM knowledge_category WHERE id = #{id}")
    @Results(id = "categoryResult", value = {
        @Result(id = true, column = "id", property = "id"),
        @Result(column = "name", property = "name"),
        @Result(column = "description", property = "description"),
        @Result(column = "sort_order", property = "sortOrder"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    // 根据分类 ID 查询，使用显式映射处理下划线字段。
    KnowledgeCategoryRow findById(@Param("id") long id);

    @Select("SELECT id, name, description, sort_order, created_at, updated_at FROM knowledge_category WHERE name = #{name} LIMIT 1")
    @ResultMap("categoryResult")
    // 根据分类名称精确查询。
    KnowledgeCategoryRow findByName(@Param("name") String name);

    @Select("SELECT id, name, description, sort_order, created_at, updated_at FROM knowledge_category ORDER BY sort_order ASC, id ASC")
    @ResultMap("categoryResult")
    // 按排序值及主键升序查询全部分类。
    List<KnowledgeCategoryRow> findAll();

    @Insert("INSERT INTO knowledge_category (name, description, sort_order) VALUES (#{name}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    // 插入分类并回填自增主键。
    int insert(KnowledgeCategoryRow row);

    @Update("UPDATE knowledge_category SET name = #{name}, description = #{description}, sort_order = #{sortOrder} WHERE id = #{id}")
    // 更新指定分类的名称、描述及排序值。
    int update(KnowledgeCategoryRow row);

    @Delete("DELETE FROM knowledge_category WHERE id = #{id}")
    // 根据主键删除分类，关联题目时由数据库外键阻止删除。
    int deleteById(@Param("id") long id);
}
