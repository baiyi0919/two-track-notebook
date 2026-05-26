package com.twotrack.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twotrack.notebook.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
