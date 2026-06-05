-- =============================================================================
-- ORA-01950: no privileges on tablespace 'USERS'
-- 원인: c##lol_scrim 에 테이블스페이스 쿼터가 없으면 INSERT 시 발생
-- 실행: SYS 또는 DBA 계정으로 SQL Developer / SQL*Plus 에서 1회 실행
-- =============================================================================

-- 따옴표 없이 (Oracle에 등록된 이름은 보통 C##LOL_SCRIM)
ALTER USER c##lol_scrim QUOTA UNLIMITED ON USERS CONTAINER=ALL;

-- (선택) 쿼터 확인
-- SELECT username, tablespace_name, bytes, max_bytes
-- FROM dba_ts_quota
-- WHERE username = 'C##LOL_SCRIM';
