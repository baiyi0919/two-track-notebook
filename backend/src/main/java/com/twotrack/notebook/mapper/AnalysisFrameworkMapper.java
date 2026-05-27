package com.twotrack.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twotrack.notebook.entity.AnalysisFramework;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnalysisFrameworkMapper extends BaseMapper<AnalysisFramework> {

    /** 统计某用户待更新的框架数量（next_update_time <= now） */
    @Select("""
        SELECT COUNT(*) FROM analysis_framework f
        INNER JOIN persona_config p ON f.persona_id = p.id
        WHERE p.user_id = #{userId} AND p.is_deleted = 0
          AND f.next_update_time <= #{now}
    """)
    long countPendingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 查询某用户所有待更新的框架 */
    @Select("""
        SELECT f.* FROM analysis_framework f
        INNER JOIN persona_config p ON f.persona_id = p.id
        WHERE p.user_id = #{userId} AND p.is_deleted = 0
          AND f.next_update_time <= #{now}
    """)
    List<AnalysisFramework> selectPendingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 查询所有待更新的框架（定时任务用，不限制用户）*/
    @Select("""
        SELECT f.* FROM analysis_framework f
        INNER JOIN persona_config p ON f.persona_id = p.id
        WHERE p.is_deleted = 0 AND f.next_update_time <= #{now}
    """)
    List<AnalysisFramework> selectAllPending(@Param("now") LocalDateTime now);
}
