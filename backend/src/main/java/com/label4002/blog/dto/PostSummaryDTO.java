package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryDTO(
        Long id,
        String title,
        String excerpt,
        Long authorId,
        String authorName,
        String authorAvatar,
        String status,
        Long categoryId,
        String categoryPath,
        List<KeywordDTO> keywords,
        long viewCount,
        LocalDateTime createdAt
) {
}
