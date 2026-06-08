package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailDTO(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorName,
        String authorAvatar,
        String status,
        String rejectionReason,
        Long categoryId,
        String categoryPath,
        List<CategoryBreadcrumbDTO> categoryBreadcrumb,
        List<KeywordDTO> keywords,
        long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
