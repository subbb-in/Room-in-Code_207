package com.hamcheese.roomincode.domain.user.dto;

import lombok.Getter;

@Getter
public class PasswordChangeDto {
    private String oldPassword;
    private String newPassword;
}
