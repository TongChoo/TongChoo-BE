CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    total_xp INT NOT NULL DEFAULT 0,
    grade VARCHAR(20) NOT NULL DEFAULT 'NOVICE',
    excuse_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS excuses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reply_to_excuse_id BIGINT NULL,
    situation VARCHAR(500) NOT NULL,
    target VARCHAR(20) NOT NULL,
    tone VARCHAR(20) NOT NULL,
    excuse_text VARCHAR(1000) NOT NULL,
    incoming_message VARCHAR(500) NULL,
    round_number INT NOT NULL DEFAULT 1,
    success_rate INT NOT NULL,
    realism INT NOT NULL,
    persuasion INT NOT NULL,
    suspicion_level VARCHAR(10) NOT NULL,
    earned_xp INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_excuses_user_created (user_id, created_at),
    CONSTRAINT fk_excuses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_excuses_reply FOREIGN KEY (reply_to_excuse_id) REFERENCES excuses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS excuse_risk_factors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    excuse_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_risk_excuse FOREIGN KEY (excuse_id) REFERENCES excuses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS excuse_remember_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    excuse_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_remember_excuse FOREIGN KEY (excuse_id) REFERENCES excuses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS excuse_aftermaths (
    id BIGINT NOT NULL AUTO_INCREMENT,
    excuse_id BIGINT NOT NULL,
    when_label VARCHAR(20) NOT NULL,
    day_offset INT NOT NULL,
    question VARCHAR(300) NOT NULL,
    collapse_rate INT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_aftermath_excuse FOREIGN KEY (excuse_id) REFERENCES excuses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS excuse_reply_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    excuse_id BIGINT NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    selected BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_excuse_reply_option_order (excuse_id, sort_order),
    CONSTRAINT fk_reply_option_excuse FOREIGN KEY (excuse_id) REFERENCES excuses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO excuse_reply_options (excuse_id, option_text, sort_order, selected)
SELECT e.id, e.excuse_text, 0, b'1'
FROM excuses e
WHERE NOT EXISTS (
    SELECT 1 FROM excuse_reply_options option_row WHERE option_row.excuse_id = e.id
);

SET @parent_fk = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'excuses'
      AND column_name = 'parent_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @drop_parent_fk = IF(
    @parent_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE excuses DROP FOREIGN KEY `', @parent_fk, '`')
);
PREPARE drop_parent_fk_statement FROM @drop_parent_fk;
EXECUTE drop_parent_fk_statement;
DEALLOCATE PREPARE drop_parent_fk_statement;

SET @parent_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excuses'
      AND column_name = 'parent_id'
);
SET @drop_parent_column = IF(
    @parent_column_exists > 0,
    'ALTER TABLE excuses DROP COLUMN parent_id',
    'SELECT 1'
);
PREPARE drop_parent_column_statement FROM @drop_parent_column;
EXECUTE drop_parent_column_statement;
DEALLOCATE PREPARE drop_parent_column_statement;

SET @reply_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'excuses'
      AND index_name = 'uk_excuses_reply_to_excuse_id'
);
SET @add_reply_unique = IF(
    @reply_unique_exists > 0,
    'SELECT 1',
    'ALTER TABLE excuses ADD CONSTRAINT uk_excuses_reply_to_excuse_id UNIQUE (reply_to_excuse_id)'
);
PREPARE add_reply_unique_statement FROM @add_reply_unique;
EXECUTE add_reply_unique_statement;
DEALLOCATE PREPARE add_reply_unique_statement;
