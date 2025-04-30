package com.bbumas.video.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bbumas.video.entity.Detected;
import com.bbumas.video.repository.DetectedRepository;
import com.bbumas.video.service.VideoAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/video")
public class VideoController {

    private final DetectedRepository detectedRepository;
    private final VideoAnalysisService videoAnalysisService;
    private final ObjectMapper objectMapper;

    public VideoController(DetectedRepository detectedRepository, VideoAnalysisService videoAnalysisService, ObjectMapper objectMapper) {
        this.detectedRepository = detectedRepository;
        this.videoAnalysisService = videoAnalysisService;
        this.objectMapper = objectMapper;
    }

    /**
     * 비디오 업로드 페이지 표시
     */
    @GetMapping
    public String videoPage(Model model) {
        model.addAttribute("detections", detectedRepository.findAll());
        return "video/video";
    }

    /**
     * 비디오 파일 업로드 및 분석 처리
     */
    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("videoFile") MultipartFile file) throws IOException {
        // 비디오 분석 서비스를 통해 객체 탐지 수행
        Map<String, Object> analysisResults = videoAnalysisService.analyzeVideo(file);
        
        // 분석 결과를 모델에 추가
        Map<String, Object> results = new HashMap<>();
        results.put("results", analysisResults);
        
        // 세션에 결과 저장 (다음 페이지로 전달)
        return "redirect:/video/video-result";
    }

    /**
     * 분석 결과 페이지 표시
     */
    @GetMapping("/result")
    public String showResults(Model model) {
        // 여기서는 VideoAnalysisService에서 저장된 결과를 가져와서 모델에 추가
        List<Map<String, Object>> results = videoAnalysisService.getLastAnalysisResults();
        model.addAttribute("results", results);
        return "video/video-result";
    }

    /**
     * 선택된 감지 결과 저장
     */
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveDetections(@RequestBody List<Map<String, Object>> detections) {
        try {
            // 선택된 감지 결과를 데이터베이스에 저장
            for (Map<String, Object> detection : detections) {
                String time = (String) detection.get("time");
                String className = (String) detection.get("class_name");
                String where = (String) detection.get("where");
                Double latitude = Double.parseDouble(detection.get("latitude").toString());
                Double longitude = Double.parseDouble(detection.get("longitude").toString());
                String filePath = (String) detection.get("file_path");
                String videoRegion = (String) detection.get("video_region");
                
                // 현재 시간으로 설명 구성
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String description = videoRegion + "에서 " + LocalDateTime.now().format(formatter) + "에 발견된 " + className;
                
                // Detected 엔티티 생성 및 저장
                Detected detected = Detected.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .detectedObject(className)
                        .imagePath(filePath)
                        .description(description)
                        .build();
                
                detectedRepository.save(detected);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "성공적으로 저장되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "저장 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 감지 결과 상세 보기
     */
    @GetMapping("/{id}")
    public String viewDetection(@PathVariable Long id, Model model) {
        Detected detected = detectedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid detection Id:" + id));
        model.addAttribute("detection", detected);
        return "video/view";
    }
}