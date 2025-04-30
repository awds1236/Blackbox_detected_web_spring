package com.bbumas.video.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class VideoAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(VideoAnalysisService.class);

    @Value("${yolo.model.path}")
    private String yoloModelPath;

    @Value("${python.script.path}")
    private String pythonScriptPath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 최근 분석 결과를 저장하는 큐 (최대 5개까지 저장)
    private final ConcurrentLinkedQueue<List<Map<String, Object>>> recentAnalysisResults = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_RESULTS = 5;

    /**
     * 비디오 파일을 분석하고 결과를 반환합니다.
     *
     * @param file 업로드된 비디오 파일
     * @return 분석 결과 목록
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public Map<String, Object> analyzeVideo(MultipartFile file) throws IOException {
        // 비디오 파일 저장
        String videoPath = saveVideoFile(file);
        
        // 결과를 저장할 디렉토리
        String resultDir = "uploads/results/" + UUID.randomUUID().toString();
        Path resultPath = Paths.get(resultDir);
        Files.createDirectories(resultPath);
        
        logger.info("Starting video analysis with YOLO model: {}", yoloModelPath);
        logger.info("Video path: {}, Result directory: {}", videoPath, resultDir);
        
        // Python 스크립트 실행
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                "--video", videoPath,
                "--model", yoloModelPath,
                "--output", resultDir
        );
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        // 스크립트 출력 읽기
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
                logger.debug("Python output: {}", line);
            }
        }
        
        // 프로세스 종료 대기
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Python script execution failed with exit code: {}", exitCode);
                throw new IOException("Python script execution failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Python script execution was interrupted", e);
            throw new IOException("Python script execution was interrupted", e);
        }
        
        // JSON 결과 파싱
        Map<String, Object> rawResults = objectMapper.readValue(output.toString(), Map.class);
        
        // 분석 결과 목록 생성
        List<Map<String, Object>> resultsList = new ArrayList<>();
        
        // 현재 시간 포맷팅
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // 결과 데이터 생성
        Map<String, Object> resultItem = new HashMap<>();
        resultItem.put("id", UUID.randomUUID().toString());
        resultItem.put("time", currentTime);
        resultItem.put("class_name", rawResults.get("detected_object"));
        resultItem.put("where", "도시시설관리공단");  // 기본값 설정
        resultItem.put("latitude", 36.3504119);  // 예시 위도 (실제로는 GPS 정보나 메타데이터에서 가져올 수 있음)
        resultItem.put("longitude", 127.3845475);  // 예시 경도
        resultItem.put("file_name", rawResults.get("image_path"));
        
        resultsList.add(resultItem);
        
        // 최근 분석 결과 저장
        storeAnalysisResults(resultsList);
        
        return rawResults;
    }
    
    /**
     * 분석 결과를 저장합니다.
     *
     * @param results 분석 결과 목록
     */
    private void storeAnalysisResults(List<Map<String, Object>> results) {
        recentAnalysisResults.add(results);
        
        // 큐 크기가 최대값을 초과하면 가장 오래된 결과 제거
        while (recentAnalysisResults.size() > MAX_RECENT_RESULTS) {
            recentAnalysisResults.poll();
        }
    }
    
    /**
     * 가장 최근 분석 결과를 반환합니다.
     *
     * @return 최근 분석 결과 목록
     */
    public List<Map<String, Object>> getLastAnalysisResults() {
        if (recentAnalysisResults.isEmpty()) {
            return Collections.emptyList();
        }
        
        return recentAnalysisResults.peek();
    }

    /**
     * 비디오 파일을 파일 시스템에 저장합니다.
     *
     * @param file 업로드된 비디오 파일
     * @return 저장된 파일의 경로
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    private String saveVideoFile(MultipartFile file) throws IOException {
        // 업로드 디렉토리 생성
        String uploadDir = "uploads/video";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 고유 파일명 생성
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        
        // 파일 저장
        file.transferTo(filePath.toFile());
        logger.info("Video file saved at: {}", filePath);
        
        return filePath.toString();
    }
    
    /**
     * 특정 디렉토리의 모든 분석 결과를 가져옵니다.
     * 
     * @param resultDir 결과 디렉토리 경로
     * @return 분석 결과 목록
     */
    public List<Map<String, Object>> getAnalysisResultsFromDirectory(String resultDir) {
        // 실제 구현에서는 디렉토리에서 파일을 읽어 결과를 구성
        return Collections.emptyList();
    }
}