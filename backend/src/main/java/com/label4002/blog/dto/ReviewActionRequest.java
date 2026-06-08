package com.label4002.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewActionRequest(
        @NotBlank(message = "操作不能为空")
        String action,

        String reason
) {
}
