-- =====================================================================
-- 01_schema.sql : 테이블 정의 (DDL)
-- 프로젝트 : goodBuy — 은행 거래내역 실소비 분석 서비스
-- 대상 DB  : Supabase (PostgreSQL 15+)
-- 실행 위치 : Supabase Dashboard > SQL Editor
-- 실행 순서 : 01_schema -> 02_seed -> 03_access -> 04_verify
-- =====================================================================
-- [설계 결정] DB는 "분류 기준 데이터"만 보관한다
--
--   은행 거래내역은 민감한 개인 금융정보이므로 서버에 저장하지 않는다.
--   사용자 이름·거래 상대·거래 금액은 어느 테이블에도 남지 않으며,
--   분석 요청을 처리하는 동안 Backend 메모리에만 존재하다가 응답 후 사라진다.
--
--   DB에 저장하는 것   : 카테고리, 이상치 기준 금액, 분류 키워드, GIF 주소
--   DB에 저장하지 않는 것 : 사용자 이름, 거래 내역, 분류 결과, 집계 결과
--
--   따라서 이 DB는 런타임에 읽기 전용(read-only)으로만 사용된다.
--   Backend는 앱 시작 시 두 테이블을 읽어 캐싱하고, 이후 쓰기 작업이 없다.
-- ---------------------------------------------------------------------
-- 재실행(멱등) 가능하도록 DROP 후 재생성합니다.
-- =====================================================================

-- 구버전 정리 (거래 저장 구조에서 기준 데이터 전용 구조로 전환)
DROP TABLE IF EXISTS transactions      CASCADE;
DROP TABLE IF EXISTS analysis_sessions CASCADE;
DROP TABLE IF EXISTS category_rules    CASCADE;
DROP TABLE IF EXISTS categories        CASCADE;

-- ---------------------------------------------------------------------
-- categories : 소비 카테고리 마스터 (9행)
--   Backend가 이상치 판정과 대표 카테고리 GIF 표시에 사용한다.
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    category_id      SERIAL       PRIMARY KEY,
    category_name    VARCHAR(30)  NOT NULL UNIQUE,   -- FOOD, TRANSPORT ...
    dutch_threshold  INT          NOT NULL,          -- 이상치(더치페이 후보) 기준 금액
    gif_url          VARCHAR(255) NOT NULL,          -- 대표 카테고리 GIF 주소

    CONSTRAINT ck_categories_threshold_positive
        CHECK (dutch_threshold > 0)
);

COMMENT ON TABLE  categories                  IS '소비 카테고리 마스터. 분류·이상치 판정의 기준 데이터';
COMMENT ON COLUMN categories.category_name    IS 'FOOD, TRANSPORT, LIVING, SHOPPING, CULTURE_HOBBY, HEALTH, EDUCATION, FIXED_SUBSCRIPTION, OTHER';
COMMENT ON COLUMN categories.dutch_threshold  IS '이 금액을 초과하면 이상치 후보. Backend가 original_amount와 비교';
COMMENT ON COLUMN categories.gif_url          IS '대표 카테고리로 선정됐을 때 Frontend가 표시할 GIF 주소';

-- ---------------------------------------------------------------------
-- category_rules : 카테고리 분류 키워드 (553행, categories 1 : N)
--   Backend가 거래 상대명에 이 키워드가 포함되는지 검사해 카테고리를 결정한다.
--   하나의 카테고리에 여러 키워드가 존재하므로 별도 테이블로 분리(정규화)했다.
-- ---------------------------------------------------------------------
CREATE TABLE category_rules (
    rule_id      SERIAL      PRIMARY KEY,
    category_id  INT         NOT NULL,
    keyword      VARCHAR(50) NOT NULL,

    CONSTRAINT fk_rules_category
        FOREIGN KEY (category_id) REFERENCES categories(category_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_rules_keyword UNIQUE (keyword),      -- 키워드 중복 방지
    CONSTRAINT ck_rules_keyword_not_blank
        CHECK (btrim(keyword) <> '')
);

COMMENT ON TABLE  category_rules            IS '카테고리 분류 키워드. Backend가 문자열 포함 여부로 매칭';
COMMENT ON COLUMN category_rules.keyword    IS '정규화한 거래상대명에 이 키워드가 포함되면 해당 카테고리로 분류';

-- ---------------------------------------------------------------------
-- 인덱스 : 카테고리별 키워드 조회
-- ---------------------------------------------------------------------
CREATE INDEX idx_rules_category ON category_rules (category_id);
