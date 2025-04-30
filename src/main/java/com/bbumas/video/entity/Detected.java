package com.bbumas.video.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Detected {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;
    
    private Double latitude;
    private Double longitude;
    private String detectedObject;
    private String imagePath;
    private String description;
    
    // 필요한 getter 메소드들 추가
    public Double getLatitude() {
        return this.latitude;
    }
    
    public Double getLongitude() {
        return this.longitude;
    }
    
    public String getDetectedObject() {
        return this.detectedObject;
    }
    
    public String getImagePath() {
        return this.imagePath;
    }
    
    public Long getPk() {
        return this.pk;
    }

    public String getDescription() {
        return this.description;
    }
}