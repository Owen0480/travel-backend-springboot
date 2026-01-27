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

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        
        // Use the correct unique identifier based on the provider (e.g., 'sub' for Google)
        String oauthId = String.valueOf(attributes.get(userNameAttributeName));

        User user = saveOrUpdate(email, name, oauthId, registrationId);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                userNameAttributeName
        );
    }

    private User saveOrUpdate(String email, String name, String oauthId, String registrationId) {
        User user = userRepository.findByEmail(email)
                .map(entity -> entity.toBuilder()
                        .fullName(name)
                        .oauthIdentifier(oauthId)
                        .build())
                .orElse(User.builder()
                        .email(email)
                        .fullName(name)
                        .oauthProvider(User.OAuthProvider.valueOf(registrationId.toUpperCase()))
                        .oauthIdentifier(oauthId)
                        .role(User.UserRole.ROLE_USER)
                        .status(User.UserStatus.ACTIVE)
                        .build());

        return userRepository.save(user);
    }
}
