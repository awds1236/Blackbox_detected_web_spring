package com.bbumas.post.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    private Double postLatitude;
    private Double postLongitude;
    private String reportType;
    private String postImage;
    private String detectedImage; // 감지된 이미지 필드 추가

    @Column(length = 1000)
    private String postContent;   // content → postContent로 변경

    private String postTitle;     
    private String author;        

    private LocalDateTime createdAt;

    // 기본 생성자
    public Post() {}

    // 필요한 필드를 포함한 생성자
    public Post(Double postLatitude, Double postLongitude, String reportType, 
                String postImage, String detectedImage, String postContent, 
                String postTitle, String author) {
        this.postLatitude = postLatitude;
        this.postLongitude = postLongitude;
        this.reportType = reportType;
        this.postImage = postImage;
        this.detectedImage = detectedImage;
        this.postContent = postContent;
        this.postTitle = postTitle;
        this.author = author;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getPostImageUrl() {
        if (postImage == null) return null;
        return "/uploads/post/" + postImage;
    }
    
    public String getDetectedImageUrl() {
        if (detectedImage == null) return null;
        return "/uploads/post/detected/" + detectedImage;
    }

    // Getter 메소드들
    public Double getPostLatitude() { return postLatitude; }
    public Double getPostLongitude() { return postLongitude; }
    public String getReportType() { return reportType; }
    public Long getPk() { return pk; }
    public String getPostImage() { return postImage; }
    public String getDetectedImage() { return detectedImage; }
    public String getPostContent() { return postContent; }
    public String getPostTitle() { return postTitle; }
    public String getAuthor() { return author; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setter 메소드들
    public void setPostLatitude(Double postLatitude) { this.postLatitude = postLatitude; }
    public void setPostLongitude(Double postLongitude) { this.postLongitude = postLongitude; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public void setPk(Long pk) { this.pk = pk; }
    public void setPostImage(String postImage) { this.postImage = postImage; }
    public void setDetectedImage(String detectedImage) { this.detectedImage = detectedImage; }
    public void setPostContent(String postContent) { this.postContent = postContent; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public void setAuthor(String author) { this.author = author; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder 클래스
    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public static class PostBuilder {
        private Double postLatitude;
        private Double postLongitude;
        private String reportType;
        private String postImage;
        private String detectedImage;
        private String postContent;
        private String postTitle;
        private String author;

        public PostBuilder postLatitude(Double postLatitude) {
            this.postLatitude = postLatitude;
            return this;
        }

        public PostBuilder postLongitude(Double postLongitude) {
            this.postLongitude = postLongitude;
            return this;
        }

        public PostBuilder reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        public PostBuilder postImage(String postImage) {
            this.postImage = postImage;
            return this;
        }
        
        public PostBuilder detectedImage(String detectedImage) {
            this.detectedImage = detectedImage;
            return this;
        }

        public PostBuilder postContent(String postContent) {
            this.postContent = postContent;
            return this;
        }

        public PostBuilder postTitle(String postTitle) {
            this.postTitle = postTitle;
            return this;
        }

        public PostBuilder author(String author) {
            this.author = author;
            return this;
        }

        public Post build() {
            return new Post(postLatitude, postLongitude, reportType, 
                           postImage, detectedImage, postContent, 
                           postTitle, author);
        }
    }
}