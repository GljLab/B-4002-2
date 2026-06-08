package com.label4002.blog.controller;

import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@Validated
public class PublicPostController {

    private final PostService postService;

    public PublicPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public PageResponse<PostSummaryDTO> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(required = false) Long authorId
    ) {
        return postService.listPublic(page, size, categoryId, keywordId, authorId);
    }

    @GetMapping("/{id}")
    public PostDetailDTO detail(@PathVariable Long id) {
        postService.incrementViewCount(id);
        return postService.getDetail(id);
    }
}
