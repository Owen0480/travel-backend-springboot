package com.travel.planner.domain.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return "로그인이 필요합니다.";
        }
        String userName = oauth2User.getAttribute("name");
        String userEmail = oauth2User.getAttribute("email");

        return userName + "님 반갑습니다! (이메일: " + userEmail + ") 로그인이 완벽하게 확인되었습니다.";
    }
}