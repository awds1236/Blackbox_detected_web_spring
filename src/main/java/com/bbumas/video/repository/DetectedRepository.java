package com.bbumas.video.repository;

import com.bbumas.video.entity.Detected;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectedRepository extends JpaRepository<Detected, Long> {
    // 추가적인 쿼리 메소드가 필요하면 여기에 정의
}