package com.label4002.blog.dto;

import java.util.List;

public record AdminStatsDTO(
        long totalPosts,
        long pendingPosts,
        long totalAuthors,
        long monthlyNewPosts,
        long totalViews,
        List<AuthorRankItem> authorRanking
) {

    public record AuthorRankItem(
            Long id,
            String nickname,
            String avatarUrl,
            long postCount,
            long totalViews
    ) {
    }
}
