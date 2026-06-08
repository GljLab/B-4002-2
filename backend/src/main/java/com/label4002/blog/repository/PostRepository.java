package com.label4002.blog.repository;

import com.label4002.blog.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<PostEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    Optional<PostEntity> findByIdAndAuthorId(Long id, Long authorId);

    @Query("SELECT p FROM PostEntity p JOIN p.category c WHERE c.id IN :categoryIds AND c.enabled = true ORDER BY p.createdAt DESC")
    Page<PostEntity> findByCategoryIdsAndEnabled(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

    @Query("SELECT DISTINCT p FROM PostEntity p JOIN p.postKeywords pk JOIN pk.keyword k JOIN p.category c WHERE k.id = :keywordId AND c.enabled = true ORDER BY p.createdAt DESC")
    Page<PostEntity> findByKeywordIdAndCategoryEnabled(@Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM PostEntity p JOIN p.category c JOIN p.postKeywords pk JOIN pk.keyword k WHERE c.id IN :categoryIds AND c.enabled = true AND k.id = :keywordId ORDER BY p.createdAt DESC")
    Page<PostEntity> findByCategoryIdsAndKeywordId(@Param("categoryIds") List<Long> categoryIds, @Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p JOIN p.category c WHERE c.enabled = true ORDER BY p.createdAt DESC")
    Page<PostEntity> findAllWithEnabledCategory(Pageable pageable);

    long countByCategoryId(Long categoryId);

    @Query("SELECT p.category.id, COUNT(p) FROM PostEntity p WHERE p.category.id IS NOT NULL GROUP BY p.category.id")
    List<Object[]> countByCategoryIdGrouped();

    @Modifying
    @Query("UPDATE PostEntity p SET p.category.id = :newCategoryId WHERE p.category.id = :oldCategoryId")
    void reassignCategory(@Param("oldCategoryId") Long oldCategoryId, @Param("newCategoryId") Long newCategoryId);
}
