USE scada;

-- Demo account password for all seeded users: Password123!

INSERT INTO users (
    id,
    email,
    password_hash,
    name,
    phone,
    role,
    plant_id,
    status,
    note,
    last_login_at
) VALUES
    (1001, 'admin@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '통합 관리자', '010-1000-0001', 'ADMIN', NULL, 'ACTIVE', '전체 사업장 열람 계정', NOW()),
    (1002, 'manager.hwaseong@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '기아 화성 관리자', '010-1000-0002', 'MANAGER', 1, 'ACTIVE', '기아 화성 사업장 담당 계정', NOW()),
    (1003, 'manager.gwangmyeong@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '기아 광명 관리자', '010-1000-0003', 'MANAGER', 2, 'ACTIVE', '기아 광명 사업장 담당 계정', NOW()),
    (1004, 'manager.gwangju@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '기아 광주 관리자', '010-1000-0004', 'MANAGER', 3, 'ACTIVE', '기아 광주 사업장 담당 계정', NOW()),
    (1005, 'manager.ulsan@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '현대 울산 관리자', '010-1000-0005', 'MANAGER', 4, 'ACTIVE', '현대 울산 사업장 담당 계정', NOW()),
    (1006, 'manager.asan@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '현대 아산 관리자', '010-1000-0006', 'MANAGER', 5, 'ACTIVE', '현대 아산 사업장 담당 계정', NOW()),
    (1007, 'manager.jeonju@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '현대 전주 관리자', '010-1000-0007', 'MANAGER', 6, 'ACTIVE', '현대 전주 사업장 담당 계정', NOW())
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    phone = VALUES(phone),
    role = VALUES(role),
    plant_id = VALUES(plant_id),
    status = VALUES(status),
    note = VALUES(note);
