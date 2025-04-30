package com.bbumas.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bbumas.user.entity.User;
import com.bbumas.user.service.UserService;

@Controller
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    @GetMapping("/join")
    public String showJoinPage(Model model) {
        model.addAttribute("form", new User());  // ← 여기서 form 생성 및 전달
        return "user/join";
    }

    @PostMapping("/join")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String password_check,
                               @RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String phone,
                               Model model) {
    
        // 사용자명 중복 검사
        if (userService.findByUsername(username).isPresent()) {
            model.addAttribute("error", "이미 사용 중인 사용자명입니다.");
            return "user/join";
        }
    
        // 비밀번호 확인 일치 검사
        if (!password.equals(password_check)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "user/join";
        }
    
        // 새 사용자 생성 및 저장
        User user = User.builder()
                .username(username)
                .password(password)
                .email(email)
                .name(name)
                .phone(phone)
                .special(false)
                .build();
    
        userService.registerUser(user);
    
        return "redirect:/login?registered";
    }

    @GetMapping("/login/policy")
    public String policyPage() {
        return "user/policy";
    }
}