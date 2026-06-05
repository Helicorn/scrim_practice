-- =============================================================================
-- c##lol_scrim 계정 생성 (최초 1회)
-- 실행: SYS AS SYSDBA (SQL*Plus / SQL Developer)
-- 연결: localhost:1521/xe (앱과 동일 인스턴스인지 확인)
-- =============================================================================

-- 이미 있으면 ORA-01920 — 그때는 grant-tablespace-quota.sql 만 실행
CREATE USER c##lol_scrim IDENTIFIED BY "여기에_비밀번호"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON USERS
  CONTAINER=ALL;

GRANT CONNECT, RESOURCE TO c##lol_scrim CONTAINER=ALL;

-- 확인
-- SELECT username, common FROM dba_users WHERE username = 'C##LOL_SCRIM';
