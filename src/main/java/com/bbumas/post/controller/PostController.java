package com.bbumas.post.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bbumas.post.entity.Post;
import com.bbumas.post.repository.PostRepository;
import com.bbumas.post.service.ImageAnalysisService;
import com.bbumas.user.entity.User;
import com.bbumas.user.repository.UserRepository;

@Controller
@RequestMapping("/post")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageAnalysisService imageAnalysisService;

    @Autowired
    public PostController(PostRepository postRepository, UserRepository userRepository, ImageAnalysisService imageAnalysisService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.imageAnalysisService = imageAnalysisService;
    }

    @GetMapping
    public String listPosts(Model model) {
        List<Post> posts = postRepository.findAll();
        model.addAttribute("posts", posts);
        return "post/post";
    }

    @GetMapping("/posting")
    public String postingForm(Model model) {
        // Form 데이터를 담을 객체를 모델에 추가
        model.addAttribute("form", new HashMap<String, Object>());
        return "post/service-details";
    }

    @PostMapping("/posting")
    public String createPost(@RequestParam(value = "post_image", required = false) MultipartFile file,
                             @RequestParam String report_type,
                             @RequestParam String post_content,
                             @RequestParam String post_title,
                             @RequestParam Double latitude,
                             @RequestParam Double longitude,
                             RedirectAttributes redirectAttributes) {
        
        logger.info("게시물 작성 요청 - 제목: {}, 신고 유형: {}", post_title, report_type);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // 이미지 저장 및 분석
        String fileName = null;
        String detectedFileName = null;
        String detectedObject = null;
        
        if (file != null && !file.isEmpty()) {
            try {
                logger.info("이미지 파일 업로드됨 - 파일명: {}, 크기: {} 바이트", file.getOriginalFilename(), file.getSize());
                
                // 이미지 분석 서비스를 통해 객체 탐지 수행
                Map<String, Object> analysisResult = imageAnalysisService.analyzeImage(file);
                
                // 분석 결과에서 정보 추출
                fileName = (String) analysisResult.get("original_image");
                detectedFileName = (String) analysisResult.get("detected_image");
                detectedObject = (String) analysisResult.get("main_object");
                
                logger.info("이미지 분석 완료 - 원본: {}, 분석결과: {}, 감지된 객체: {}", 
                           fileName, detectedFileName, detectedObject);
                
                // 감지된 객체가 없는 경우 기본 설정
                if (detectedObject == null || "Unknown".equals(detectedObject) || "Error".equals(detectedObject)) {
                    detectedObject = "미확인 객체";
                    logger.warn("감지된 객체가 없거나 오류 발생 - 기본값 설정: {}", detectedObject);
                }
                
                // 신고 유형이 설정되지 않은 경우 감지된 객체를 기반으로 설정
                if (report_type == null || report_type.isEmpty()) {
                    report_type = detectedObject;
                    logger.info("신고 유형 자동 설정: {}", report_type);
                }
            } catch (Exception e) {
                logger.error("이미지 분석 중 오류 발생", e);
                
                try {
                    // 분석 실패 시 기본 파일 저장 로직 사용
                    createDirectories();
                    fileName = saveImage(file, "uploads/post");
                    logger.info("분석 실패 후 기본 저장 - 파일명: {}", fileName);
                } catch (IOException ioe) {
                    logger.error("이미지 저장 중 오류 발생", ioe);
                    redirectAttributes.addFlashAttribute("error", "이미지 처리 중 오류가 발생했습니다: " + ioe.getMessage());
                    return "redirect:/post/posting";
                }
            }
        } else {
            logger.info("이미지 없이 게시물 작성");
        }
        
        try {
            // 포스트 엔티티 생성 및 저장
            Post post = Post.builder()
                    .postLatitude(latitude)
                    .postLongitude(longitude)
                    .reportType(report_type)
                    .postImage(fileName)
                    .detectedImage(detectedFileName)
                    .postContent(post_content)
                    .postTitle(post_title)
                    .author(username)
                    .build();
            
            Post savedPost = postRepository.save(post);
            logger.info("게시물 저장 완료 - ID: {}", savedPost.getPk());
            
            redirectAttributes.addFlashAttribute("success", "게시물이 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            logger.error("게시물 저장 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("error", "게시물 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/post";
    }

    @GetMapping("/faq")
    public String faqPage() {
        return "post/faq";
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid post Id:" + id));
        model.addAttribute("post", post);
        return "post/post_detail";
    }
    
    @PostMapping("/post/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> deletePost(@PathVariable Long id) {
        logger.info("게시물 삭제 요청 - ID: {}", id);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid post Id:" + id));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // 작성자이거나 관리자인 경우만 삭제 허용
            if (post.getAuthor().equals(username) || user.isStaff()) {
                postRepository.delete(post);
                logger.info("게시물 삭제 완료 - ID: {}", id);
                
                Map<String, Boolean> response = new HashMap<>();
                response.put("success", true);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("게시물 삭제 권한 없음 - 요청자: {}, 작성자: {}", username, post.getAuthor());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false));
            }
        } catch (Exception e) {
            logger.error("게시물 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false));
        }
    }
    
    /**
     * 시스템 정보 조회
     */
    @GetMapping("/system-info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Python 버전 확인
        String pythonVersion = imageAnalysisService.checkPythonVersion();
        info.put("pythonVersion", pythonVersion);
        
        // JVM 정보
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("osName", System.getProperty("os.name"));
        
        // 작업 디렉토리
        info.put("workingDirectory", System.getProperty("user.dir"));
        
        return ResponseEntity.ok(info);
    }

    /**
     * 필요한 디렉토리를 생성합니다.
     */
    private void createDirectories() throws IOException {
        // 기본 업로드 디렉토리 생성
        Path uploadPath = Paths.get("uploads/post");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("업로드 디렉토리 생성: {}", uploadPath);
        }
        
        // 감지된 이미지 디렉토리 생성
        Path detectedPath = Paths.get("uploads/post/detected");
        if (!Files.exists(detectedPath)) {
            Files.createDirectories(detectedPath);
            logger.info("감지된 이미지 디렉토리 생성: {}", detectedPath);
        }
    }

    /**
     * 파일을 저장하고 파일명을 반환합니다.
     */
    private String saveImage(MultipartFile file, String uploadDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 고유 파일명 생성 (UUID + 확장자만 사용)
        String fileName = UUID.randomUUID().toString() + extension;
        
        // 파일 저장
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath);
        logger.info("이미지 저장: {}", filePath);
        
        return fileName;
    }
}