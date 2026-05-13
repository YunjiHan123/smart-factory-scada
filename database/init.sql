CREATE DATABASE IF NOT EXISTS scada
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE scada;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO users (email, password_hash, nickname)
VALUES (
    'test@scada.com',
    '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W',
    'test-user'
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname);
