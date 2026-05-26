package com.twotrack.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.twotrack.notebook.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
