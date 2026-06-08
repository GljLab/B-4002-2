package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailDTO(
        Long id,
        String title,
        String content,
        String authorName,
        Long categoryId,
        String categoryPath,
        List<CategoryBreadcrumbDTO> categoryBreadcrumb,
        List<KeywordDTO> keywords,
        LocalDateTime createdAt
) {
}
