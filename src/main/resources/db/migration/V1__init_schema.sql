-- ============================================================
-- V1__init_schema.sql — 初始化数据库表结构
-- ============================================================

-- 1. 系统用户表
CREATE TABLE sys_user
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username   VARCHAR(50)  NOT NULL COMMENT '用户名',
    password   VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    nickname   VARCHAR(50)  NULL     COMMENT '昵称',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '系统用户表_系统使用者账号；创建→正常使用→禁用/软删除';

-- 2. 系统角色表
CREATE TABLE sys_role
(
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code       VARCHAR(30) NOT NULL COMMENT '角色编码（ADMIN/USER 等）',
    name       VARCHAR(50) NOT NULL COMMENT '角色名称',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '系统角色表_RBAC角色定义；预置 ADMIN/USER，后续可扩展';

-- 3. 用户角色关联表
CREATE TABLE sys_user_role
(
    id         BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id    BIGINT     NOT NULL COMMENT '用户ID（关联 sys_user.id）',
    role_id    BIGINT     NOT NULL COMMENT '角色ID（关联 sys_role.id）',
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME   NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '用户角色关联表_多对多关联；分配角色时创建记录，移除角色时软删除';

-- 4. 刷新令牌表
CREATE TABLE sys_refresh_token
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id    BIGINT       NOT NULL COMMENT '用户ID（关联 sys_user.id）',
    token_hash VARCHAR(255) NOT NULL COMMENT '刷新令牌 SHA-256 哈希值',
    expires_at DATETIME     NOT NULL COMMENT '过期时间',
    revoked    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已撤销：0=有效 1=已撤销',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_hash (token_hash),
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '刷新令牌表_签发时写入 SHA-256 哈希；有效→刷新时旧记录撤销→过期自动失效。物理保留 7 天后清理';

-- 5. 系统权限表
CREATE TABLE sys_permission
(
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code       VARCHAR(50) NOT NULL COMMENT '权限编码',
    name       VARCHAR(50) NOT NULL COMMENT '权限名称',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '系统权限表_细粒度操作许可；Phase 1 开始填充数据';

-- 6. 角色权限关联表
CREATE TABLE sys_role_permission
(
    id            BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id       BIGINT     NOT NULL COMMENT '角色ID（关联 sys_role.id）',
    permission_id BIGINT     NOT NULL COMMENT '权限ID（关联 sys_permission.id）',
    created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME   NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '角色权限关联表_多对多关联；Phase 1 开始填充数据';
