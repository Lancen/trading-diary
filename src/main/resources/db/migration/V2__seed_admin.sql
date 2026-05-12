-- ============================================================
-- V2__seed_admin.sql — 预置管理员角色与用户（幂等）
-- ============================================================

-- 预置 ADMIN 角色
INSERT INTO sys_role (code, name, created_at)
SELECT 'ADMIN', '管理员', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1
                  FROM sys_role
                  WHERE code = 'ADMIN');

-- 预置 USER 角色
INSERT INTO sys_role (code, name, created_at)
SELECT 'USER', '普通用户', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1
                  FROM sys_role
                  WHERE code = 'USER');

-- 预置管理员用户（密码占位符，由 AdminInitializer 在 T037 启动时替换）
INSERT INTO sys_user (username, password, nickname, status, created_at)
SELECT 'admin', '{BCRYPT_PLACEHOLDER}', '管理员', 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1
                  FROM sys_user
                  WHERE username = 'admin');

-- 绑定 admin 用户 → ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM sys_user u
         JOIN sys_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (SELECT 1
                  FROM sys_user_role ur
                  WHERE ur.user_id = u.id
                    AND ur.role_id = r.id);
