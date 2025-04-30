package com.bbumas.map.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bbumas.post.entity.Post;
import com.bbumas.post.repository.PostRepository;
import com.bbumas.video.entity.Detected;
import com.bbumas.video.repository.DetectedRepository;

@Controller
@RequestMapping("/map")
public class MapController {

    private final PostRepository postRepository;
    private final DetectedRepository detectedRepository;

    public MapController(PostRepository postRepository, DetectedRepository detectedRepository) {
        this.postRepository = postRepository;
        this.detectedRepository = detectedRepository;
    }

    @GetMapping
    public String mapPage(@RequestParam(name = "q", required = false) String query, Model model) {
        // 모든 포스트 및 감지 데이터 가져오기
        List<Post> posts = postRepository.findAll();
        List<Detected> videos = detectedRepository.findAll();

        // JSON 형식으로 변환하여 전달
        List<List<Object>> locationPosts = posts.stream()
                .map(loc -> List.of(
                        (Object) loc.getPostLatitude(),
                        (Object) loc.getPostLongitude(),
                        (Object) loc.getReportType(),
                        (Object) loc.getPostImageUrl(),
                        (Object) loc.getPk()))
                .collect(Collectors.toList());

        List<List<Object>> locationVideos = videos.stream()
                .map(loc -> List.of(
                        (Object) loc.getLatitude(),
                        (Object) loc.getLongitude(),
                        (Object) loc.getDetectedObject(),
                        (Object) loc.getImagePath(),
                        (Object) loc.getPk()))
                .collect(Collectors.toList());

        // map.html에서 사용하는 변수명으로 모델에 추가
        model.addAttribute("locationPosts", locationPosts);
        model.addAttribute("locationVideos", locationVideos);
        model.addAttribute("searchQuery", query);

        return "map/map";
    }
}