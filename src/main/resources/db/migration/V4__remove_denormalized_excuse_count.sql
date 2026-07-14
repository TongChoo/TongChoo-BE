-- 변명 개수는 원본 변명 행에서 정확히 계산할 수 있다. users.excuse_count를 함께
-- 갱신하면 동시 생성/실패 롤백 때 실제 행 수와 달라질 수 있어 단일 원천만 유지한다.
ALTER TABLE users DROP COLUMN excuse_count;
