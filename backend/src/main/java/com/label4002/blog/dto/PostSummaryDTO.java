package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryDTO(
        Long id,
        String title,
        String excerpt,
        String authorName,
        Long categoryId,
        String categoryPath,
        List<KeywordDTO> keywords,
        LocalDateTime createdAt
) {
}
