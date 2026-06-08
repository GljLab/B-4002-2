package com.label4002.blog.controller;

import com.label4002.blog.dto.BatchReviewRequest;
import com.label4002.blog.dto.MessageResponse;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.dto.ReviewActionRequest;
import com.label4002.blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class ReviewController {

    private final PostService postService;

    public ReviewController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public PageResponse<PostSummaryDTO> listPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postService.listPendingPosts(page, size);
    }

    @GetMapping("/{id}")
    public PostDetailDTO getPostForReview(@PathVariable Long id) {
        return postService.getDetail(id);
    }

    @PutMapping("/{id}")
    public MessageResponse reviewAction(@PathVariable Long id,
                                        @Valid @RequestBody ReviewActionRequest request) {
        if ("APPROVE".equalsIgnoreCase(request.action())) {
            postService.approvePost(id);
            return new MessageResponse("文章已审核通过");
        } else if ("REJECT".equalsIgnoreCase(request.action())) {
            postService.rejectPost(id, request.reason());
            return new MessageResponse("文章已驳回");
        } else {
            throw new com.label4002.blog.exception.BadRequestException("无效的审核操作: " + request.action());
        }
    }

    @PutMapping("/batch")
    public MessageResponse batchReview(@Valid @RequestBody BatchReviewRequest request) {
        if ("APPROVE".equalsIgnoreCase(request.action())) {
            postService.batchApprove(request.postIds());
            return new MessageResponse("批量审核通过完成");
        } else if ("REJECT".equalsIgnoreCase(request.action())) {
            postService.batchReject(request.postIds(), request.reason());
            return new MessageResponse("批量驳回完成");
        } else {
            throw new com.label4002.blog.exception.BadRequestException("无效的审核操作: " + request.action());
        }
    }
}
