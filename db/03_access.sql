-- =====================================================================
-- 03_access.sql : RLS(행 수준 보안) + 권한 설정
-- ---------------------------------------------------------------------
-- 이 DB는 분류 기준 데이터만 보관하며 런타임에는 읽기 전용으로 사용된다.
--   Backend  : JDBC 직접 연결(postgres 역할) -> 두 테이블 조회
--   Frontend : anon 키 -> 두 테이블 조회 가능 (민감 정보가 없으므로 허용)
--   DA       : 대시보드에서 기준 데이터 관리 (키워드·기준금액 수정)
--
-- 거래 데이터를 저장하지 않으므로 보호해야 할 개인정보가 DB에 없다.
-- 다만 기준 데이터가 임의로 변경되면 분석 결과가 달라지므로
-- INSERT/UPDATE/DELETE는 차단하고 SELECT만 허용한다.
-- =====================================================================

-- 구버전 잔재 정리 (거래 저장 구조에서 사용하던 객체)
DROP VIEW     IF EXISTS v_representative_category;
DROP VIEW     IF EXISTS v_category_summary;
DROP VIEW     IF EXISTS v_transaction_detail;
DROP FUNCTION IF EXISTS get_analysis_result(INT);
DROP FUNCTION IF EXISTS process_analysis(INT);
DROP FUNCTION IF EXISTS process_analysis(TEXT);
DROP FUNCTION IF EXISTS mark_self_transfers(INT);
DROP FUNCTION IF EXISTS mark_self_transfers(TEXT);
DROP FUNCTION IF EXISTS classify_transactions(INT);
DROP FUNCTION IF EXISTS classify_transactions();
DROP FUNCTION IF EXISTS start_analysis();
DROP FUNCTION IF EXISTS normalize_text(TEXT);
DROP TABLE    IF EXISTS transactions      CASCADE;
DROP TABLE    IF EXISTS analysis_sessions CASCADE;

ALTER TABLE categories     ENABLE ROW LEVEL SECURITY;
ALTER TABLE category_rules ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------
-- 조회만 허용 (INSERT/UPDATE/DELETE 정책 없음 = 차단)
-- ---------------------------------------------------------------------
DROP POLICY IF EXISTS p_categories_read ON categories;
CREATE POLICY p_categories_read ON categories
    FOR SELECT TO anon, authenticated USING (true);

DROP POLICY IF EXISTS p_category_rules_read ON category_rules;
CREATE POLICY p_category_rules_read ON category_rules
    FOR SELECT TO anon, authenticated USING (true);

-- ---------------------------------------------------------------------
-- 확인 1 : RLS 상태 (2개 모두 true 여야 정상)
-- ---------------------------------------------------------------------
SELECT relname AS table_name, relrowsecurity AS rls_enabled
FROM pg_class
WHERE relname IN ('categories','category_rules')
ORDER BY relname;

-- ---------------------------------------------------------------------
-- 확인 2 : 남아있는 테이블 (categories, category_rules 2개만 있어야 정상)
-- ---------------------------------------------------------------------
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
