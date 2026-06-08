package com.label4002.blog.service;

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
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.exception.ForbiddenException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CategoryRepository;
import com.label4002.blog.repository.KeywordRepository;
import com.label4002.blog.repository.PostKeywordRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public PageResponse<PostSummaryDTO> listPublic(int page, int size, Long categoryId, Long keywordId) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize);

        Page<PostEntity> postPage;

        if (categoryId != null && keywordId != null) {
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
            postPage = postRepository.findByCategoryIdsAndEnabled(catIds.stream().toList(), pageable);
        } else if (keywordId != null) {
            postPage = postRepository.findByKeywordIdAndCategoryEnabled(keywordId, pageable);
        } else {
            postPage = postRepository.findAllWithEnabledCategory(pageable);
        }

        List<PostSummaryDTO> items = postPage.getContent().stream()
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

        keywordService.decrementKeywordCountsForPost(postId);
        postRepository.delete(post);
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
                post.getAuthor().getUsername(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                categoryPath,
                keywordDTOs,
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
                post.getAuthor().getUsername(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                categoryPath,
                breadcrumbs,
                keywordDTOs,
                post.getCreatedAt()
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
