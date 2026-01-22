package com.travel.planner.domain.user.service;

import com.travel.planner.domain.user.entity.User;
import com.travel.planner.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 1. 구글에서 준 정보를 꺼냅니다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google
        Map<String, Object> attributes = oAuth2User.getAttributes(); // 이름, 이메일 등이 담긴 주머니

        // 2. 우리 프로젝트에 맞게 데이터를 정리합니다.
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String sub = (String) attributes.get("sub"); // 구글 고유 ID

        // 3. DB에 있으면 업데이트, 없으면 신규 가입! (이게 핵심)
        User user = userRepository.findByOauthIdentifier(sub)
                .map(entity -> {
                    entity.updateProfile(name); // 이름 바뀌었으면 업데이트
                    return entity;
                })
                .orElseGet(() -> User.builder() // 없으면 새로 생성
                        .email(email)
                        .fullName(name)
                        .oauthProvider(User.OAuthProvider.GOOGLE)
                        .oauthIdentifier(sub)
                        .role(User.UserRole.ROLE_USER)
                        .status(User.UserStatus.ACTIVE)
                        .build());

        userRepository.save(user);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                "email" // 구글의 식별자 기준을 email로 설정
        );
    }
}