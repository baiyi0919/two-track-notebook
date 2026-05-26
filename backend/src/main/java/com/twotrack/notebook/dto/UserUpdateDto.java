package com.twotrack.notebook.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserUpdateDto {

    @Length(min = 2, max = 20, message = "昵称长度应在2-20个字符之间")
    private String nickname;

    private String avatarUrl;
}
