package com.travel.planner.global.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class TokenProvider {

    // 1. 위조 방지 도장
    private final String secretKey = "your-very-secret-key-make-it-long-and-strong";

    // 2. 출입증 유효 시간 1시간
    private final Long tokenValidityInMilliseconds = 1000L * 60 * 60;

    public String createToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);    // 토큰 주인은 이메일
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)  // 내용
                .setIssuedAt(now)  // 만든시간
                .setExpiration(validity)    // 끝나는 시간
                .signWith(SignatureAlgorithm.HS256, secretKey)  // 서버 도장
                .compact();                 // 한줄 문자열로 압축
    }

    // 토큰 유효 확인 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey) // 찍은 키 가져옴
                    .build()
                    .parseClaimsJws(token);  // 토큰 열어봄
            return true;
        } catch (Exception e) {
            return false;   // 위조 및 만료 false
        }
    }
}
