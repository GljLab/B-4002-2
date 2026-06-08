package com.label4002.blog.service;

import com.label4002.blog.dto.AuthorStatsDTO;
import com.label4002.blog.dto.CategoryBreadcrumbDTO;
import com.label4002.blog.dto.CreatePostRequest;
import com.label4002.blog.dto.KeywordDTO;
import com.label4002.blog.dto.PageResponse;
import com.label4002.blog.dto.PostDetailDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.dto.UpdatePostRequest;
import com.label4002.blog.entity.CategoryEntity;
import com.label4002.blog.entity.KeywordEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.ForbiddenException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CategoryRepository;
import com.label4002.blog.repository.KeywordRepository;
import com.label4002.blog.repository.PostKeywordRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final KeywordService keywordService;
    private final PostKeywordRepository postKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final CategoryService categoryService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       KeywordService keywordService,
                       PostKeywordRepository postKeywordRepository,
                       KeywordRepository keywordRepository,
                       CategoryService categoryService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.keywordService = keywordService;
        this.postKeywordRepository = postKeywordRepository;
        this.keywordRepository = keywordRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listPublic(int page, int size, Long categoryId, Long keywordId, Long authorId) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> postPage;

        if (authorId != null && categoryId != null) {
            postPage = postRepository.findByAuthorIdAndStatusAndCategoryId(authorId, PostStatus.PUBLISHED, categoryId, pageable);
        } else if (authorId != null) {
            postPage = postRepository.findByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED, pageable);
        } else if (categoryId != null && keywordId != null) {
            Set<Long> catIds = categoryService.getEnabledCategoryIdsWithDescendants(categoryId);
            if (catIds.isEmpty()) {
                return new PageResponse<>(Collections.emptyList(), 0, normalizedPage, normalizedSize);
            }
            postPage = postRepository.findByCategoryIdsAndKeywordId(catIds.stream().toList(), keywordId, pageable);
        } else if (categoryId != null) {
            Set<Long> catIds = categoryService.getEnabledCategoryIdsWithDescendants(categoryId);
            if (catIds.isEmpty()) {
                return new PageResponse<>(Collections.emptyList(), 0, normalizedPage, normalizedSize);
            }
            postPage = postRepository.findByStatusAndCategoryId(PostStatus.PUBLISHED, categoryId, pageable);
        } else if (keywordId != null) {
            postPage = postRepository.findByKeywordIdAndCategoryEnabled(keywordId, pageable);
        } else {
            postPage = postRepository.findByStatus(PostStatus.PUBLISHED, pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public PostDetailDTO getDetail(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));
        return toDetail(post);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDTO> listMine(Long userId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public PostDetailDTO createPost(Long userId, CreatePostRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        PostEntity post = new PostEntity();
        post.setTitle(normalizeTitle(request.title()));
        post.setContent(request.content().trim());
        post.setAuthor(user);
        post.setStatus(PostStatus.DRAFT);
        post.setViewCount(0);

        if (request.categoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("分类不存在"));
            if (!category.isEnabled()) {
                throw new NotFoundException("分类不存在或已禁用");
            }
            post.setCategory(category);
        } else {
            CategoryEntity defaultCat = categoryRepository.findBySlug("default")
                    .orElseThrow(() -> new NotFoundException("默认分类不存在"));
            post.setCategory(defaultCat);
        }

        PostEntity saved = postRepository.save(post);

        if (request.keywords() != null && !request.keywords().isEmpty()) {
            List<KeywordEntity> keywordEntities = keywordService.resolveOrCreate(request.keywords());
            List<Long> keywordIds = keywordEntities.stream().map(KeywordEntity::getId).toList();
            for (Long kwId : keywordIds) {
                postKeywordRepository.insertPostKeyword(saved.getId(), kwId);
            }
            keywordService.addKeywordCounts(keywordIds);
        }

        return toDetail(saved);
    }

    @Transactional
    public PostDetailDTO updatePost(Long userId, Long postId, UpdatePostRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限编辑他人的文章");
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅草稿或已驳回的文章可以编辑");
        }

        post.setTitle(normalizeTitle(request.title()));
        post.setContent(request.content().trim());

        if (request.categoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("分类不存在"));
            post.setCategory(category);
        }

        PostEntity saved = postRepository.save(post);

        if (request.keywords() != null) {
            List<KeywordEntity> keywordEntities = keywordService.resolveOrCreate(request.keywords());
            List<Long> newKeywordIds = keywordEntities.stream().map(KeywordEntity::getId).toList();
            keywordService.syncKeywordCounts(saved.getId(), newKeywordIds);
        }

        return toDetail(saved);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限删除他人的文章");
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅草稿或已驳回的文章可以删除");
        }

        keywordService.decrementKeywordCountsForPost(postId);
        postRepository.delete(post);
    }

    @Transactional
    public void submitForReview(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权限提交他人的文章");
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new BadRequestException("仅草稿或已驳回的文章可以提交审核");
        }

        post.setStatus(PostStatus.PENDING);
        post.setRejectionReason(null);
        postRepository.save(post);
    }

    @Transactional
    public void approvePost(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以审核通过");
        }

        post.setStatus(PostStatus.PUBLISHED);
        post.setRejectionReason(null);
        postRepository.save(post);
    }

    @Transactional
    public void rejectPost(Long postId, String reason) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PENDING) {
            throw new BadRequestException("仅待审核的文章可以驳回");
        }

        post.setStatus(PostStatus.REJECTED);
        post.setRejectionReason(reason);
        postRepository.save(post);
    }

    @Transactional
    public void unpublishPost(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BadRequestException("仅已发布的文章可以取消发布");
        }

        post.setStatus(PostStatus.DRAFT);
        post.setRejectionReason(null);
        postRepository.save(post);
    }

    @Transactional
    public void batchApprove(List<Long> postIds) {
        postRepository.updateStatusByIds(PostStatus.PUBLISHED, postIds);
    }

    @Transactional
    public void batchReject(List<Long> postIds, String reason) {
        List<PostEntity> posts = postRepository.findAllById(postIds);
        for (PostEntity post : posts) {
            post.setStatus(PostStatus.REJECTED);
            post.setRejectionReason(reason);
        }
        postRepository.saveAll(posts);
    }

    @Transactional
    public void incrementViewCount(Long postId) {
        postRepository.incrementViewCount(postId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listAuthorPosts(Long authorId, String status, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> postPage;
        if (status != null && !status.isBlank()) {
            PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
            postPage = postRepository.findByAuthorIdAndStatus(authorId, postStatus, pageable);
        } else {
            postPage = postRepository.findByAuthorId(authorId, pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public AuthorStatsDTO getAuthorStats(Long authorId) {
        long totalPublished = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED);
        long totalDraft = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.DRAFT);
        long totalViews = postRepository.sumViewCountByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED);
        long avgViews = totalPublished > 0 ? totalViews / totalPublished : 0;

        List<PostEntity> topPosts = postRepository.findTop10ByAuthorIdAndStatusOrderByViewCountDesc(authorId, PostStatus.PUBLISHED);
        List<PostSummaryDTO> topPostDTOs = topPosts.stream()
                .map(this::toSummary)
                .toList();

        return new AuthorStatsDTO(totalPublished, totalDraft, totalViews, avgViews, topPostDTOs);
    }

    @Transactional
    public void transferAuthor(Long oldAuthorId, Long newAuthorId) {
        postRepository.transferAuthor(oldAuthorId, newAuthorId);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return postRepository.countByStatus(PostStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDTO> listAllPosts(String status) {
        if (status != null && !status.isBlank()) {
            PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
            return postRepository.findByStatus(postStatus, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent().stream()
                    .map(this::toSummary)
                    .toList();
        }
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listPendingPosts(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<PostEntity> postPage = postRepository.findByStatus(PostStatus.PENDING, pageable);

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDTO> listPublicByAuthor(Long authorId, Long categoryId, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> postPage;
        if (categoryId != null) {
            postPage = postRepository.findByAuthorIdAndStatusAndCategoryId(authorId, PostStatus.PUBLISHED, categoryId, pageable);
        } else {
            postPage = postRepository.findByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED, pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(items, postPage.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional
    public void batchUpdateCategory(List<Long> postIds, Long categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("分类不存在"));
        for (Long postId : postIds) {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("文章不存在 id=" + postId));
            post.setCategory(category);
            postRepository.save(post);
        }
    }

    @Transactional
    public void batchAddKeywords(List<Long> postIds, List<String> keywordNames) {
        List<KeywordEntity> keywords = keywordService.resolveOrCreate(keywordNames);
        List<Long> keywordIds = keywords.stream().map(KeywordEntity::getId).toList();
        for (Long postId : postIds) {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("文章不存在 id=" + postId));
            keywordService.syncKeywordCounts(postId, keywordIds);
        }
    }

    private String normalizeTitle(String title) {
        return title.trim().replaceAll("\\s+", " ");
    }

    private PostSummaryDTO toSummary(PostEntity post) {
        List<KeywordDTO> keywordDTOs = postKeywordRepository.findKeywordsByPostId(post.getId())
                .stream()
                .map(k -> new KeywordDTO(k.getId(), k.getName(), k.getUsageCount(), k.getLastUsedAt(), k.isArchived(), k.getCreatedAt()))
                .toList();

        String categoryPath = "";
        if (post.getCategory() != null) {
            List<CategoryBreadcrumbDTO> breadcrumbs = categoryService.getBreadcrumb(post.getCategory().getId());
            categoryPath = CategoryBreadcrumbDTO.buildPath(breadcrumbs);
        }

        return new PostSummaryDTO(
                post.getId(),
                post.getTitle(),
                excerpt(post.getContent(), 100),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getStatus().name(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                categoryPath,
                keywordDTOs,
                post.getViewCount(),
                post.getCreatedAt()
        );
    }

    private PostDetailDTO toDetail(PostEntity post) {
        List<KeywordDTO> keywordDTOs = postKeywordRepository.findKeywordsByPostId(post.getId())
                .stream()
                .map(k -> new KeywordDTO(k.getId(), k.getName(), k.getUsageCount(), k.getLastUsedAt(), k.isArchived(), k.getCreatedAt()))
                .toList();

        List<CategoryBreadcrumbDTO> breadcrumbs = Collections.emptyList();
        String categoryPath = "";
        if (post.getCategory() != null) {
            breadcrumbs = categoryService.getBreadcrumb(post.getCategory().getId());
            categoryPath = CategoryBreadcrumbDTO.buildPath(breadcrumbs);
        }

        return new PostDetailDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getStatus().name(),
                post.getRejectionReason(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                categoryPath,
                breadcrumbs,
                keywordDTOs,
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private String excerpt(String content, int length) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= length) {
            return normalized;
        }
        return normalized.substring(0, length) + "...";
    }
}
