package com.twotrack.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twotrack.notebook.entity.ThreadPersona;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ThreadPersonaMapper extends BaseMapper<ThreadPersona> {

    /**
     * 查询某议题中未删除的角色关联列表
     * 只返回 thread_persona 表字段，前端负责合并 persona_config 信息
     */
    @Select("""
        SELECT tp.*
        FROM thread_persona tp
        WHERE tp.thread_id = #{threadId}
          AND tp.is_deleted = 0
        ORDER BY tp.sort_order ASC, tp.id ASC
    """)
    List<ThreadPersona> selectActiveByThreadId(@Param("threadId") Long threadId);

    /**
     * 查询某议题中所有角色关联（包括已隐藏的）
     */
    @Select("SELECT * FROM thread_persona WHERE thread_id = #{threadId} AND persona_id = #{personaId} LIMIT 1")
    ThreadPersona selectByThreadIdAndPersonaId(@Param("threadId") Long threadId, @Param("personaId") Long personaId);

    /**
     * 软删除某议题中的角色（隐藏）
     */
    @Update("UPDATE thread_persona SET is_deleted = 1 WHERE thread_id = #{threadId} AND persona_id = #{personaId}")
    int hidePersona(@Param("threadId") Long threadId, @Param("personaId") Long personaId);

    /**
     * 恢复某议题中的角色（显示）
     */
    @Update("UPDATE thread_persona SET is_deleted = 0 WHERE thread_id = #{threadId} AND persona_id = #{personaId}")
    int showPersona(@Param("threadId") Long threadId, @Param("personaId") Long personaId);
}
