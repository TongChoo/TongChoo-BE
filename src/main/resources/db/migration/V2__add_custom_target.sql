ALTER TABLE excuses
    MODIFY COLUMN target ENUM(
        'TEACHER',
        'PARENT',
        'FRIEND',
        'LOVER',
        'TEAM_LEAD',
        'TEAM_MEMBER',
        'CUSTOM'
    ) NOT NULL,
    ADD COLUMN target_description VARCHAR(100) NULL AFTER target;

ALTER TABLE excuses
    ADD CONSTRAINT chk_excuses_custom_target_description
    CHECK (
        target <> 'CUSTOM'
        OR (
            target_description IS NOT NULL
            AND CHAR_LENGTH(TRIM(target_description)) BETWEEN 1 AND 100
        )
    );
