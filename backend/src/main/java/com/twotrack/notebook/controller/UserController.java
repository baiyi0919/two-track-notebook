package com.twotrack.notebook.controller;

import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.UserUpdateDto;
import com.twotrack.notebook.service.UserService;
import com.twotrack.notebook.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVo> getUserInfo() {
        return Result.success(userService.getUserInfo());
    }

    /** 更新当前登录用户信息 */
    @PutMapping("/info")
    public Result<UserVo> updateInfo(@Validated @RequestBody UserUpdateDto dto) {
        return Result.success(userService.updateUser(dto));
    }
}
