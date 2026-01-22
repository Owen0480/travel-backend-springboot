CREATE TABCREATE TABLE users (
                       user_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '사용자 고유 일련번호',
                       oauth_provider varchar(20) NOT NULL COMMENT 'OAuth 제공자 (예: kakao, naver, google)',
                       oauth_identifier varchar(255) NOT NULL COMMENT 'OAuth에서 제공하는 사용자 고유 식별값',
                       full_name varchar(50) NOT NULL COMMENT '사용자 실명 또는 닉네임',
                       email varchar(100) DEFAULT NULL COMMENT '사용자 이메일 주소',
                       created_at timestamp NULL DEFAULT current_timestamp() COMMENT '계정 생성 일시',
                       updated_at timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '회원 정보 수정 일시',
                       PRIMARY KEY (user_id),
                       UNIQUE KEY uk_oauth_account (oauth_provider,oauth_identifier),
                       UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 계정 및 OAuth 인증 정보 테이블';