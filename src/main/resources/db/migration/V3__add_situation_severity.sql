ALTER TABLE excuses
    ADD COLUMN situation_severity ENUM('LIGHT', 'NORMAL', 'SERIOUS')
        NOT NULL DEFAULT 'NORMAL' AFTER tone;
