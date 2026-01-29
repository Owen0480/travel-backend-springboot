package com.travel.planner.domain.auth.service;

import com.travel.planner.domain.auth.dto.LoginRequestDto;
import com.travel.planner.domain.auth.dto.RegisterRequestDto;
import com.travel.planner.domain.auth.dto.TokenResponseDto;
import com.travel.planner.domain.auth.entity.RefreshToken;
import com.travel.planner.domain.auth.repository.RefreshTokenRepository;
import com.travel.planner.domain.user.entity.User;
import com.travel.planner.domain.user.repository.UserRepository;
import com.travel.planner.global.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Transactional
    public void register(RegisterRequestDto registerRequestDto) {
        if (userRepository.findByEmail(registerRequestDto.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .email(registerRequestDto.getEmail())
                .password(passwordEncoder.encode(registerRequestDto.getPassword()))
                .fullName(registerRequestDto.getFullName())
                .oauthProvider(User.OAuthProvider.LOCAL)
                .role(User.UserRole.ROLE_USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. Authenticate user
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 2. Generate Tokens
        String accessToken = tokenProvider.createAccessToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication);

        // 3. Store Refresh Token in Redis
        RefreshToken refreshTokenEntity = new RefreshToken(authentication.getName(), refreshToken);
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(authentication.getName())
                .build();
    }

    @Transactional
    public TokenResponseDto refresh(String refreshToken) {
        // 1. Validate Refresh Token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token is invalid");
        }

        // 2. Get Authentication from Token
        Authentication authentication = tokenProvider.getAuthentication(refreshToken);

        // 3. Check if Refresh Token exists in Redis
        RefreshToken savedToken = refreshTokenRepository.findById(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not logged in"));

        if (!savedToken.getToken().equals(refreshToken)) {
            throw new RuntimeException("Refresh Token does not match");
        }

        // 4. Generate New Tokens
        String newAccessToken = tokenProvider.createAccessToken(authentication);
        String newRefreshToken = tokenProvider.createRefreshToken(authentication);

        // 5. Update Refresh Token in Redis
        savedToken.updateToken(newRefreshToken);
        refreshTokenRepository.save(savedToken);

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(authentication.getName())
                .build();
    }

    @Transactional
    public void logout(String name, String accessToken) {
        // 1. Refresh Token 삭제 (Redis)
        refreshTokenRepository.deleteById(name);
    }

    @Transactional
    public void withdrawByOauthIdentifier(String name, String accessToken) {
        // 1. 토큰 및 세션 정리 (Access Token 블랙리스트 및 Refresh Token 삭제 로직 호출)
        logout(name, accessToken);

        // 2. DB에서 사용자 삭제 (email 또는 oauthIdentifier 중 하나로 조회하여 Local/OAuth 유저 모두 대응)
        User user = userRepository.findByEmail(name)
                .orElseGet(() -> userRepository.findByOauthIdentifier(name)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));

        userRepository.delete(user);
    }
}