-- Nova-Agent V1 手动建表脚本
-- 使用前请先创建数据库：CREATE DATABASE nova_agent DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS resource_definition (
    id           VARCHAR(128) NOT NULL,
    type         VARCHAR(32)  NOT NULL,
    version      VARCHAR(32)  NOT NULL,
    name         VARCHAR(255),
    config_json  JSON,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS execution_record (
    execution_id VARCHAR(64)  NOT NULL,
    resource_id  VARCHAR(128) NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    context_json JSON,
    error_message TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
