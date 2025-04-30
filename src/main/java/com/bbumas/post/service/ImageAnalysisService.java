package com.bbumas.post.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class ImageAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);

    @Value("${yolo.model.path}")
    private String yoloModelPath;

    @Value("${python.image.script.path}")
    private String pythonScriptPath;
    
    @Value("${python.command:python}")
    private String pythonCommand;
    
    @Value("${python.timeout:60}")
    private int pythonTimeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        // 시작 시 환경 테스트 실행
        testEnvironment();
        
        // 필요한 디렉토리 생성
        try {
            createDirectories();
        } catch (IOException e) {
            logger.error("디렉토리 생성 중 오류 발생", e);
        }
    }
    
    /**
     * 환경 설정을 테스트합니다.
     */
    public void testEnvironment() {
        logger.info("이미지 분석 환경 테스트 시작");
        
        // Python 스크립트 존재 여부 확인
        Path scriptPath = Paths.get(pythonScriptPath);
        if (!Files.exists(scriptPath)) {
            logger.error("Python 스크립트를 찾을 수 없습니다: {}", pythonScriptPath);
        } else {
            logger.info("Python 스크립트 확인: {}", pythonScriptPath);
        }
        
        // YOLO 모델 존재 여부 확인
        Path modelPath = Paths.get(yoloModelPath);
        if (!Files.exists(modelPath)) {
            logger.error("YOLO 모델을 찾을 수 없습니다: {}", yoloModelPath);
        } else {
            logger.info("YOLO 모델 확인: {}", yoloModelPath);
        }
        
        // Python 테스트 실행
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand, pythonScriptPath, "--test"
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 출력 읽기
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Python 환경 테스트: {}", line);
                }
            }
            
            // 프로세스 종료 대기
            boolean completed = process.waitFor(pythonTimeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.error("Python 환경 테스트 시간 초과");
            } else {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    logger.error("Python 환경 테스트 실패 (종료 코드: {})", exitCode);
                } else {
                    logger.info("Python 환경 테스트 성공");
                }
            }
        } catch (Exception e) {
            logger.error("Python 환경 테스트 중 오류 발생", e);
        }
        
        logger.info("이미지 분석 환경 테스트 완료");
    }
    
    /**
     * 필요한 디렉토리를 생성합니다.
     */
    private void createDirectories() throws IOException {
        // 이미지 업로드 디렉토리
        Path uploadPath = Paths.get("uploads/post");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("업로드 디렉토리 생성: {}", uploadPath);
        }
        
        // 감지된 이미지 디렉토리
        Path detectedPath = Paths.get("uploads/post/detected");
        if (!Files.exists(detectedPath)) {
            Files.createDirectories(detectedPath);
            logger.info("감지된 이미지 디렉토리 생성: {}", detectedPath);
        }
    }

    /**
     * 이미지를 분석하고 결과를 반환합니다.
     *
     * @param imagePath 업로드된 이미지 파일 경로
     * @param outputDir 결과 이미지를 저장할 디렉토리
     * @return 분석 결과
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public Map<String, Object> analyzeImage(String imagePath, String outputDir) throws IOException {
        logger.info("이미지 분석 시작 - 이미지: {}, 출력 디렉토리: {}", imagePath, outputDir);
        
        // 파일 존재 여부 확인
        Path imageFilePath = Paths.get(imagePath);
        if (!Files.exists(imageFilePath)) {
            logger.error("이미지 파일을 찾을 수 없습니다: {}", imagePath);
            throw new IOException("이미지 파일을 찾을 수 없습니다: " + imagePath);
        }
        
        // 출력 디렉토리 생성
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            logger.info("출력 디렉토리 생성: {}", outputDir);
        }
        
        // Python 명령어 구성
        String[] command = {
            pythonCommand,
            pythonScriptPath,
            "--image", imagePath,
            "--model", yoloModelPath,
            "--output", outputDir
        };
        
        logger.info("실행할 명령어: {}", String.join(" ", command));
        
        // Python 스크립트 실행
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // 출력 및 오류 읽기
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        
        // 표준 출력과 오류 읽기
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[ERROR]")) {
                    errors.append(line).append("\n");
                    logger.error("Python 오류: {}", line);
                } else {
                    output.append(line).append("\n");
                    if (line.startsWith("[INFO]")) {
                        logger.info("Python 로그: {}", line);
                    }
                }
            }
        }
        
        // 프로세스 종료 대기
        boolean completed = false;
        try {
            completed = process.waitFor(pythonTimeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.error("Python 스크립트 실행 시간 초과 ({}초)", pythonTimeoutSeconds);
                throw new IOException("Python 스크립트 실행 시간 초과 (" + pythonTimeoutSeconds + "초)");
            }
            
            int exitCode = process.exitValue();
            logger.info("Python 스크립트 종료 코드: {}", exitCode);
            
            if (exitCode != 0) {
                logger.error("Python 스크립트 실행 실패 (종료 코드: {})", exitCode);
                logger.error("오류 메시지: {}", errors.toString());
                throw new IOException("Python 스크립트 실행 실패 (종료 코드: " + exitCode + ")\n" + errors.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Python 스크립트 실행이 중단되었습니다", e);
            throw new IOException("Python 스크립트 실행이 중단되었습니다", e);
        }
        
        // JSON 결과 파싱
        String outputStr = output.toString().trim();
        if (outputStr.isEmpty()) {
            logger.error("Python 스크립트에서 출력이 없습니다");
            return Collections.emptyMap();
        }
        
        try {
            Map<String, Object> result = objectMapper.readValue(outputStr, Map.class);
            logger.info("이미지 분석 완료 - 감지된 객체: {}", result.get("main_object"));
            return result;
        } catch (Exception e) {
            logger.error("Python 스크립트 출력 파싱 실패: {}", outputStr, e);
            throw new IOException("Python 스크립트 출력 파싱 실패", e);
        }
    }

    /**
     * 이미지 파일을 분석하고 결과를 반환합니다.
     *
     * @param file 업로드된 이미지 파일
     * @return 분석 결과와 저장된 파일 정보가 포함된 맵
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public Map<String, Object> analyzeImage(MultipartFile file) throws IOException {
        // 파일 유효성 검사
        if (file == null || file.isEmpty()) {
            logger.error("업로드된 파일이 비어 있습니다");
            throw new IOException("업로드된 파일이 비어 있습니다");
        }
        
        String originalFilename = file.getOriginalFilename();
        logger.info("이미지 파일 업로드: {}, 크기: {} 바이트", originalFilename, file.getSize());
        
        // 필요한 디렉토리 생성
        createDirectories();
        
        // 이미지 저장 경로
        String uploadDir = "uploads/post";
        String detectedDir = "uploads/post/detected";
        
        // 원본 파일명에서 확장자 추출
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 고유 파일명 생성
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadDir, fileName);
        
        // 원본 이미지 저장
        Files.copy(file.getInputStream(), filePath);
        logger.info("원본 이미지 저장: {}", filePath);
        
        try {
            // 이미지 분석
            Map<String, Object> analysisResult = analyzeImage(filePath.toString(), detectedDir);
            
            // 분석 결과와 파일 정보를 합쳐서 반환
            analysisResult.put("original_image", fileName);
            return analysisResult;
        } catch (Exception e) {
            logger.error("이미지 분석 중 오류 발생", e);
            
            // 오류 발생 시 최소한의 정보 반환
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("error", e.getMessage());
            fallbackResult.put("original_image", fileName);
            fallbackResult.put("detected_image", null);
            fallbackResult.put("main_object", "Error");
            fallbackResult.put("main_confidence", 0);
            fallbackResult.put("object_count", 0);
            
            return fallbackResult;
        }
    }
    
    /**
     * Python 설치 여부 및 버전을 확인합니다.
     * 
     * @return Python 버전 문자열 또는 오류 메시지
     */
    public String checkPythonVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            return output.toString();
        } catch (Exception e) {
            return "Python 확인 오류: " + e.getMessage();
        }
    }
}