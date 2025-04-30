package com.bbumas.main.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bbumas.post.entity.Post;
import com.bbumas.post.repository.PostRepository;
import com.bbumas.video.entity.Detected;
import com.bbumas.video.repository.DetectedRepository;

@Controller
public class MainController {

        private final PostRepository postRepository;
        private final DetectedRepository detectedRepository;

        public MainController(PostRepository postRepository, DetectedRepository detectedRepository) {
                this.postRepository = postRepository;
                this.detectedRepository = detectedRepository;
        }

        @GetMapping("/")
        public String index(Model model) {
                // 포스트 데이터 가져오기
                List<Post> locationsPost = postRepository.findAll();
                List<List<Object>> locationPosts = locationsPost.stream()
                                .map(loc -> {
                                        List<Object> list = new ArrayList<>();
                                        list.add(loc.getPostLatitude());
                                        list.add(loc.getPostLongitude());
                                        list.add(loc.getReportType());
                                        list.add(loc.getPostImageUrl());
                                        list.add(loc.getPk());
                                        return list;
                                })
                                .collect(Collectors.toList());

                // 비디오 감지 데이터 가져오기
                List<Detected> locationsVideo = detectedRepository.findAll();
                List<List<Object>> locationVideos = locationsVideo.stream()
                                .map(loc -> {
                                        List<Object> list = new ArrayList<>();
                                        list.add(loc.getLatitude());
                                        list.add(loc.getLongitude());
                                        list.add(loc.getDetectedObject());
                                        list.add(loc.getImagePath());
                                        list.add(loc.getPk());
                                        return list;
                                })
                                .collect(Collectors.toList());

                // 모델에 데이터 추가
                model.addAttribute("locationPosts", locationPosts);
                model.addAttribute("locationVideos", locationVideos);

                return "main/index";
        }
}