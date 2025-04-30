package com.bbumas.post.repository;

import com.bbumas.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 추가적인 쿼리 메소드가 필요하면 여기에 정의
}