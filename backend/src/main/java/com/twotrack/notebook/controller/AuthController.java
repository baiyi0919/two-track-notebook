package com.twotrack.notebook.controller;

import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.LoginDto;
import com.twotrack.notebook.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** POST /api/auth/register */
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody LoginDto dto) {
        String token = userService.register(dto);
        return Result.success(token);
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDto dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }
}
