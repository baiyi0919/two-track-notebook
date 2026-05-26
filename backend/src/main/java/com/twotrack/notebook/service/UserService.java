package com.twotrack.notebook.service;

import com.twotrack.notebook.dto.LoginDto;
import com.twotrack.notebook.vo.UserVo;

public interface UserService {

    /** 注册，返回 JWT token */
    String register(LoginDto dto);

    /** 登录，返回 JWT token */
    String login(LoginDto dto);

    /** 获取当前登录用户信息 */
    UserVo getUserInfo();

    /** 更新当前登录用户信息 */
    UserVo updateUser(com.twotrack.notebook.dto.UserUpdateDto dto);
}
