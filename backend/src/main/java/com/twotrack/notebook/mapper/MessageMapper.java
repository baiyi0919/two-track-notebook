package com.twotrack.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twotrack.notebook.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
