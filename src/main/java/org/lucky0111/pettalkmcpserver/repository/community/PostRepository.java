package org.lucky0111.pettalkmcpserver.repository.community;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lucky0111.pettalkmcpserver.domain.common.BaseTimeEntity;
import org.lucky0111.pettalkmcpserver.domain.common.PetCategory;
import org.lucky0111.pettalkmcpserver.domain.common.PostCategory;
import org.lucky0111.pettalkmcpserver.domain.entity.common.Tag;
import org.lucky0111.pettalkmcpserver.domain.entity.community.Post;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 카테고리별 조회 메서드
    Page<Post> findByPostCategory(PostCategory postCategory, Pageable pageable);
    Page<Post> findByPetCategory(PetCategory petCategory, Pageable pageable);
    Page<Post> findByPostCategoryAndPetCategory(
            PostCategory postCategory, PetCategory petCategory, Pageable pageable);

    // 사용자별 조회
    List<Post> findByUser_UserId(UUID userId);

    @Query("SELECT p FROM Post p JOIN PostTagRelation ptr ON p.postId = ptr.post.postId WHERE ptr.tag.tagName IN :tags")
    List<Post> findByTags(List<String> tags);
}
