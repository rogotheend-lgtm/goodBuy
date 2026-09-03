-- =====================================================================
-- 02_seed.sql : 마스터 데이터 (categories / category_rules)
-- ---------------------------------------------------------------------
-- gif_url : Supabase Storage 공개 버킷 'gif' 의 실제 공개 URL
--   업로드 위치 : Storage > gif (Public bucket)
--   원본 파일   : goodB/gif/*.gif  (파일명 = category_name)
--   ⚠️ 이 URL은 프로젝트 ID가 박혀 있습니다. 다른 Supabase 프로젝트로 옮기면
--      파일 하단의 UPDATE 문으로 일괄 교체하세요.
-- =====================================================================

TRUNCATE category_rules RESTART IDENTITY CASCADE;
TRUNCATE categories     RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- 카테고리 9종
--   dutch_threshold : 1인 결제로 보기엔 과한 금액 = 더치페이(이상치) 후보 기준
-- ---------------------------------------------------------------------
INSERT INTO categories (category_name, dutch_threshold, gif_url) VALUES
    ('FOOD',               30000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/FOOD.gif'),               -- 식비
    ('TRANSPORT',          30000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/TRANSPORT.gif'),          -- 교통
    ('LIVING',             50000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/LIVING.gif'),             -- 생활
    ('SHOPPING',          100000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/SHOPPING.gif'),           -- 쇼핑
    ('CULTURE_HOBBY',      50000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/CULTURE_HOBBY.gif'),      -- 문화·취미
    ('HEALTH',             50000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/HEALTH.gif'),             -- 건강
    ('EDUCATION',         100000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/EDUCATION.gif'),          -- 교육
    ('FIXED_SUBSCRIPTION',150000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/FIXED_SUBSCRIPTION.gif'), -- 고정비·구독
    ('OTHER',             100000,  'https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/OTHER.gif');              -- 기타·확인 필요

-- ---------------------------------------------------------------------
-- 분류 키워드
--
--   Backend가 거래 상대명 안에 아래 키워드가 "들어있는지" 검사해서 카테고리를 정합니다.
--
--     거래 상대명 : 세븐일레븐 광주산정고려점
--     키워드      : 세븐일레븐              → 들어있음 → LIVING 으로 분류
--
--   그래서 키워드에는 상호명 전체가 아니라, 상호명 안에 들어갈 만한 짧은 조각을 넣습니다.
--     O  '세븐일레븐'                  (지점명이 뭐든 다 걸림)
--     X  '세븐일레븐 광주산정고려점'   (이 지점 하나만 걸림)
--
--   비교하기 전에 양쪽 모두 공백을 없애고 영문은 대문자로 바꿉니다.
--     '세븐일레븐 광주산정고려점' → '세븐일레븐광주산정고려점'
--     'Coffee'                    → 'COFFEE'
--
--   키워드가 여러 개 걸리면 더 긴 키워드가 이깁니다.
--     '쿠팡이츠 주문'  →  '쿠팡'(SHOPPING) 과 '쿠팡이츠'(FOOD) 둘 다 걸리지만
--                        더 긴 '쿠팡이츠'가 이겨서 FOOD 로 분류됩니다.
--
--   어떤 키워드에도 안 걸리면 OTHER 로 분류됩니다.
-- ---------------------------------------------------------------------
INSERT INTO category_rules (category_id, keyword)
SELECT c.category_id, k.keyword
FROM (VALUES
    -- FOOD : 음식점 · 카페 · 배달
    ('FOOD','음식점'),('FOOD','식당'),('FOOD','한식'),('FOOD','중식'),('FOOD','일식'),
    ('FOOD','양식'),('FOOD','레스토랑'),('FOOD','다이닝'),('FOOD','푸드코트'),('FOOD','키친'),
    ('FOOD','구내식당'),('FOOD','도시락'),('FOOD','반찬'),('FOOD','반찬가게'),('FOOD','백반'),
    ('FOOD','정식'),('FOOD','죽'),('FOOD','덮밥'),('FOOD','비빔밥'),('FOOD','볶음밥'),
    ('FOOD','솥밥'),('FOOD','김밥'),('FOOD','분식'),('FOOD','칼국수'),('FOOD','국밥'),
    ('FOOD','설렁탕'),('FOOD','곰탕'),('FOOD','감자탕'),('FOOD','해장국'),('FOOD','순댓국'),
    ('FOOD','찌개'),('FOOD','전골'),('FOOD','샤브샤브'),('FOOD','국수'),('FOOD','잔치국수'),
    ('FOOD','냉면'),('FOOD','밀면'),('FOOD','막국수'),('FOOD','쌀국수'),('FOOD','우동'),
    ('FOOD','라멘'),('FOOD','라면'),('FOOD','짜장면'),('FOOD','짬뽕'),('FOOD','마라탕'),
    ('FOOD','훠궈'),('FOOD','돈까스'),('FOOD','돈가스'),('FOOD','초밥'),('FOOD','스시'),
    ('FOOD','사시미'),('FOOD','오마카세'),('FOOD','떡볶이'),('FOOD','순대'),('FOOD','튀김'),
    ('FOOD','만두'),('FOOD','족발'),('FOOD','보쌈'),('FOOD','삼겹살'),('FOOD','갈비'),
    ('FOOD','고기'),('FOOD','횟집'),('FOOD','곱창'),('FOOD','막창'),('FOOD','닭발'),
    ('FOOD','치킨'),('FOOD','닭갈비'),('FOOD','찜닭'),('FOOD','닭강정'),('FOOD','피자'),
    ('FOOD','버거'),('FOOD','파스타'),('FOOD','스테이크'),('FOOD','샌드위치'),('FOOD','샐러드'),
    ('FOOD','브런치'),('FOOD','토스트'),('FOOD','핫도그'),('FOOD','타코'),('FOOD','케밥'),
    ('FOOD','카페'),('FOOD','커피'),('FOOD','COFFEE'),('FOOD','로스터리'),('FOOD','에스프레소'),
    ('FOOD','아메리카노'),('FOOD','카페라떼'),('FOOD','카페라테'),('FOOD','라떼'),('FOOD','라테'),
    ('FOOD','카푸치노'),('FOOD','마키아토'),('FOOD','콜드브루'),('FOOD','디저트'),('FOOD','베이커리'),
    ('FOOD','제과'),('FOOD','제빵'),('FOOD','케이크'),('FOOD','도넛'),('FOOD','와플'),
    ('FOOD','크로플'),('FOOD','쿠키'),('FOOD','마카롱'),('FOOD','아이스크림'),('FOOD','젤라또'),
    ('FOOD','빙수'),('FOOD','떡'),('FOOD','떡집'),('FOOD','방앗간'),('FOOD','한과'),
    ('FOOD','호두과자'),('FOOD','붕어빵'),('FOOD','스타벅스'),('FOOD','STARBUCKS'),('FOOD','투썸'),
    ('FOOD','투썸플레이스'),('FOOD','이디야'),('FOOD','이디야커피'),('FOOD','메가커피'),('FOOD','메가엠지씨커피'),
    ('FOOD','컴포즈'),('FOOD','컴포즈커피'),('FOOD','빽다방'),('FOOD','할리스'),('FOOD','탐앤탐스'),
    ('FOOD','커피빈'),('FOOD','폴바셋'),('FOOD','엔제리너스'),('FOOD','파스쿠찌'),('FOOD','더벤티'),
    ('FOOD','매머드커피'),('FOOD','공차'),('FOOD','카페베네'),('FOOD','블루보틀'),('FOOD','테라로사'),
    ('FOOD','파리바게뜨'),('FOOD','파리바게트'),('FOOD','뚜레쥬르'),('FOOD','던킨'),('FOOD','배스킨라빈스'),
    ('FOOD','베스킨라빈스'),('FOOD','설빙'),('FOOD','성심당'),('FOOD','이성당'),('FOOD','노티드'),
    ('FOOD','크리스피크림'),('FOOD','앤티앤스'),('FOOD','맘스터치'),('FOOD','맥도날드'),('FOOD','롯데리아'),
    ('FOOD','버거킹'),('FOOD','KFC'),('FOOD','노브랜드버거'),('FOOD','프랭크버거'),('FOOD','쉐이크쉑'),
    ('FOOD','써브웨이'),('FOOD','서브웨이'),('FOOD','이삭토스트'),('FOOD','명랑핫도그'),('FOOD','BHC'),
    ('FOOD','BBQ'),('FOOD','교촌'),('FOOD','굽네'),('FOOD','네네치킨'),('FOOD','처갓집'),
    ('FOOD','노랑통닭'),('FOOD','푸라닭'),('FOOD','자담치킨'),('FOOD','페리카나'),('FOOD','멕시카나'),
    ('FOOD','60계'),('FOOD','도미노피자'),('FOOD','피자헛'),('FOOD','미스터피자'),('FOOD','피자알볼로'),
    ('FOOD','본죽'),('FOOD','본도시락'),('FOOD','한솥'),('FOOD','홍콩반점'),('FOOD','역전우동'),
    ('FOOD','엽기떡볶이'),('FOOD','신전떡볶이'),('FOOD','죠스떡볶이'),('FOOD','김가네'),('FOOD','고봉민김밥'),
    ('FOOD','명륜진사갈비'),('FOOD','원할머니보쌈'),('FOOD','놀부'),('FOOD','두끼'),('FOOD','포차'),
    ('FOOD','주점'),('FOOD','술집'),('FOOD','호프'),('FOOD','이자카야'),('FOOD','와인바'),
    ('FOOD','맥주집'),('FOOD','배달의민족'),('FOOD','배민'),('FOOD','요기요'),('FOOD','쿠팡이츠'),
    ('FOOD','땡겨요'),
    -- TRANSPORT : 대중교통 · 택시 · 주유
    ('TRANSPORT','지하철'),('TRANSPORT','도시철도'),('TRANSPORT','서울교통공사'),('TRANSPORT','버스'),('TRANSPORT','교통카드'),
    ('TRANSPORT','티머니'),('TRANSPORT','캐시비'),('TRANSPORT','레일플러스'),('TRANSPORT','코레일'),('TRANSPORT','한국철도공사'),
    ('TRANSPORT','KTX'),('TRANSPORT','SRT'),('TRANSPORT','고속버스'),('TRANSPORT','공항철도'),('TRANSPORT','신분당선'),
    ('TRANSPORT','경전철'),('TRANSPORT','택시'),('TRANSPORT','카카오T'),('TRANSPORT','KAKAOT'),('TRANSPORT','카카오모빌리티'),
    ('TRANSPORT','우버'),('TRANSPORT','우티'),('TRANSPORT','타다'),('TRANSPORT','아이엠택시'),('TRANSPORT','쏘카'),
    ('TRANSPORT','그린카'),('TRANSPORT','투루카'),('TRANSPORT','킥고잉'),('TRANSPORT','씽씽'),('TRANSPORT','지쿠'),
    ('TRANSPORT','스윙'),('TRANSPORT','주유'),('TRANSPORT','주유소'),('TRANSPORT','SK에너지'),('TRANSPORT','GS칼텍스'),
    ('TRANSPORT','현대오일뱅크'),('TRANSPORT','에쓰오일'),('TRANSPORT','S-OIL'),('TRANSPORT','SOIL'),('TRANSPORT','알뜰주유소'),
    ('TRANSPORT','주차'),('TRANSPORT','주차장'),('TRANSPORT','아이파킹'),('TRANSPORT','모두의주차장'),('TRANSPORT','하이패스'),
    ('TRANSPORT','톨게이트'),('TRANSPORT','한국도로공사'),('TRANSPORT','대한항공'),('TRANSPORT','아시아나항공'),('TRANSPORT','제주항공'),
    ('TRANSPORT','진에어'),('TRANSPORT','티웨이항공'),('TRANSPORT','에어부산'),('TRANSPORT','에어서울'),
    -- LIVING : 편의점 · 마트 · 생활용품
    ('LIVING','편의점'),('LIVING','세븐일레븐'),('LIVING','GS25'),('LIVING','CU'),('LIVING','이마트24'),
    ('LIVING','미니스톱'),('LIVING','스토리웨이'),('LIVING','마트'),('LIVING','이마트'),('LIVING','홈플러스'),
    ('LIVING','롯데마트'),('LIVING','코스트코'),('LIVING','트레이더스'),('LIVING','하나로마트'),('LIVING','농협하나로마트'),
    ('LIVING','롯데슈퍼'),('LIVING','GS더프레시'),('LIVING','홈플러스익스프레스'),('LIVING','이마트에브리데이'),('LIVING','메가마트'),
    ('LIVING','킴스클럽'),('LIVING','노브랜드'),('LIVING','다이소'),('LIVING','생활용품'),('LIVING','모던하우스'),
    ('LIVING','JAJU'),('LIVING','무인양품'),('LIVING','이케아'),('LIVING','세탁'),('LIVING','세탁소'),
    ('LIVING','크린토피아'),('LIVING','워시엔조이'),('LIVING','런드리고'),('LIVING','문구'),('LIVING','알파문구'),
    ('LIVING','모닝글로리'),('LIVING','핫트랙스'),('LIVING','아트박스'),
    -- SHOPPING : 의류 · 온라인 쇼핑 · 잡화
    ('SHOPPING','쿠팡'),('SHOPPING','COUPANG'),('SHOPPING','11번가'),('SHOPPING','G마켓'),('SHOPPING','GMARKET'),
    ('SHOPPING','옥션'),('SHOPPING','SSG'),('SHOPPING','SSG닷컴'),('SHOPPING','네이버쇼핑'),('SHOPPING','롯데온'),
    ('SHOPPING','마켓컬리'),('SHOPPING','컬리'),('SHOPPING','무신사'),('SHOPPING','29CM'),('SHOPPING','W컨셉'),
    ('SHOPPING','에이블리'),('SHOPPING','지그재그'),('SHOPPING','브랜디'),('SHOPPING','KREAM'),('SHOPPING','유니클로'),
    ('SHOPPING','ZARA'),('SHOPPING','H&M'),('SHOPPING','에이치앤엠'),('SHOPPING','스파오'),('SHOPPING','탑텐'),
    ('SHOPPING','에잇세컨즈'),('SHOPPING','나이키'),('SHOPPING','아디다스'),('SHOPPING','뉴발란스'),('SHOPPING','ABC마트'),
    ('SHOPPING','백화점'),('SHOPPING','신세계백화점'),('SHOPPING','롯데백화점'),('SHOPPING','현대백화점'),('SHOPPING','갤러리아'),
    ('SHOPPING','NC백화점'),('SHOPPING','스타필드'),('SHOPPING','아울렛'),('SHOPPING','롯데아울렛'),('SHOPPING','신세계사이먼'),
    ('SHOPPING','면세점'),('SHOPPING','롯데면세점'),('SHOPPING','신라면세점'),('SHOPPING','올리브영'),('SHOPPING','시코르'),
    ('SHOPPING','아리따움'),('SHOPPING','이니스프리'),('SHOPPING','에뛰드'),('SHOPPING','미샤'),('SHOPPING','토니모리'),
    ('SHOPPING','하이마트'),('SHOPPING','전자랜드'),('SHOPPING','삼성스토어'),('SHOPPING','LG베스트샵'),('SHOPPING','애플스토어'),
    ('SHOPPING','프리스비'),('SHOPPING','의류'),('SHOPPING','잡화'),
    -- CULTURE_HOBBY : 영화 · 게임 · 운동 · 공연 · 전시 · 컨벤션
    ('CULTURE_HOBBY','CGV'),('CULTURE_HOBBY','메가박스'),('CULTURE_HOBBY','롯데시네마'),('CULTURE_HOBBY','영화'),
    ('CULTURE_HOBBY','인터파크'),('CULTURE_HOBBY','인터파크티켓'),('CULTURE_HOBBY','예스24공연'),('CULTURE_HOBBY','예스24티켓'),
    ('CULTURE_HOBBY','티켓링크'),('CULTURE_HOBBY','멜론티켓'),('CULTURE_HOBBY','공연'),('CULTURE_HOBBY','콘서트'),
    ('CULTURE_HOBBY','뮤지컬'),('CULTURE_HOBBY','연극'),('CULTURE_HOBBY','페스티벌'),('CULTURE_HOBBY','전시'),
    ('CULTURE_HOBBY','미술관'),('CULTURE_HOBBY','박물관'),('CULTURE_HOBBY','갤러리'),('CULTURE_HOBBY','아트센터'),
    ('CULTURE_HOBBY','아트뮤지엄'),('CULTURE_HOBBY','아트페어'),('CULTURE_HOBBY','예술의전당'),('CULTURE_HOBBY','세종문화회관'),
    ('CULTURE_HOBBY','국립현대미술관'),('CULTURE_HOBBY','코엑스'),('CULTURE_HOBBY','킨텍스'),('CULTURE_HOBBY','벡스코'),
    ('CULTURE_HOBBY','세텍'),('CULTURE_HOBBY','컨벤션'),('CULTURE_HOBBY','엑스포'),('CULTURE_HOBBY','박람회'),
    ('CULTURE_HOBBY','게임'),('CULTURE_HOBBY','STEAM'),('CULTURE_HOBBY','넥슨'),('CULTURE_HOBBY','넷마블'),
    ('CULTURE_HOBBY','엔씨소프트'),('CULTURE_HOBBY','블리자드'),('CULTURE_HOBBY','라이엇게임즈'),('CULTURE_HOBBY','플레이스테이션'),
    ('CULTURE_HOBBY','닌텐도'),('CULTURE_HOBBY','PC방'),('CULTURE_HOBBY','방탈출'),('CULTURE_HOBBY','보드게임카페'),
    ('CULTURE_HOBBY','만화카페'),('CULTURE_HOBBY','노래방'),('CULTURE_HOBBY','코인노래방'),('CULTURE_HOBBY','노래연습장'),
    ('CULTURE_HOBBY','헬스'),('CULTURE_HOBBY','피트니스'),('CULTURE_HOBBY','스포애니'),('CULTURE_HOBBY','요가'),
    ('CULTURE_HOBBY','필라테스'),('CULTURE_HOBBY','볼링'),('CULTURE_HOBBY','골프'),('CULTURE_HOBBY','스크린골프'),
    ('CULTURE_HOBBY','골프존'),('CULTURE_HOBBY','수영장'),('CULTURE_HOBBY','클라이밍'),('CULTURE_HOBBY','테니스'),
    ('CULTURE_HOBBY','배드민턴'),('CULTURE_HOBBY','당구'),('CULTURE_HOBBY','스키장'),('CULTURE_HOBBY','캠핑장'),
    ('CULTURE_HOBBY','워터파크'),('CULTURE_HOBBY','놀이공원'),('CULTURE_HOBBY','에버랜드'),('CULTURE_HOBBY','롯데월드'),
    ('CULTURE_HOBBY','서울랜드'),
    -- HEALTH : 병원 · 약국
    ('HEALTH','병원'),('HEALTH','종합병원'),('HEALTH','대학병원'),('HEALTH','의원'),('HEALTH','의료원'),
    ('HEALTH','치과'),('HEALTH','한의원'),('HEALTH','피부과'),('HEALTH','정형외과'),('HEALTH','내과'),
    ('HEALTH','안과'),('HEALTH','이비인후과'),('HEALTH','산부인과'),('HEALTH','소아과'),('HEALTH','정신건강의학과'),
    ('HEALTH','비뇨의학과'),('HEALTH','성형외과'),('HEALTH','영상의학과'),('HEALTH','통증의학과'),('HEALTH','재활의학과'),
    ('HEALTH','약국'),('HEALTH','건강검진'),('HEALTH','검진센터'),('HEALTH','물리치료'),('HEALTH','재활치료'),
    ('HEALTH','안경원'),('HEALTH','안경점'),('HEALTH','서울대병원'),('HEALTH','세브란스'),('HEALTH','삼성서울병원'),
    ('HEALTH','서울아산병원'),('HEALTH','서울성모병원'),('HEALTH','고려대병원'),('HEALTH','경희의료원'),
    -- EDUCATION : 서점 · 강의 · 학원
    ('EDUCATION','서점'),('EDUCATION','교보문고'),('EDUCATION','예스24'),('EDUCATION','알라딘'),('EDUCATION','영풍문고'),
    ('EDUCATION','리디북스'),('EDUCATION','학원'),('EDUCATION','교습소'),('EDUCATION','어학원'),('EDUCATION','교육원'),
    ('EDUCATION','직업학교'),('EDUCATION','대학교'),('EDUCATION','등록금'),('EDUCATION','수강료'),('EDUCATION','과외'),
    ('EDUCATION','인강'),('EDUCATION','인프런'),('EDUCATION','패스트캠퍼스'),('EDUCATION','클래스101'),('EDUCATION','UDEMY'),
    ('EDUCATION','COURSERA'),('EDUCATION','EDX'),('EDUCATION','EBS'),('EDUCATION','메가스터디'),('EDUCATION','대성마이맥'),
    ('EDUCATION','이투스'),('EDUCATION','교육'),('EDUCATION','독서실'),('EDUCATION','스터디카페'),
    -- FIXED_SUBSCRIPTION : 통신 · 보험 · 정기구독
    ('FIXED_SUBSCRIPTION','SKT'),('FIXED_SUBSCRIPTION','SK텔레콤'),('FIXED_SUBSCRIPTION','KT'),('FIXED_SUBSCRIPTION','LGU+'),
    ('FIXED_SUBSCRIPTION','LG유플러스'),('FIXED_SUBSCRIPTION','통신'),('FIXED_SUBSCRIPTION','요금제'),('FIXED_SUBSCRIPTION','알뜰폰'),
    ('FIXED_SUBSCRIPTION','헬로모바일'),('FIXED_SUBSCRIPTION','리브모바일'),('FIXED_SUBSCRIPTION','토스모바일'),('FIXED_SUBSCRIPTION','세븐모바일'),
    ('FIXED_SUBSCRIPTION','KT엠모바일'),('FIXED_SUBSCRIPTION','보험'),('FIXED_SUBSCRIPTION','보험료'),('FIXED_SUBSCRIPTION','생명'),
    ('FIXED_SUBSCRIPTION','생명보험'),('FIXED_SUBSCRIPTION','손해보험'),('FIXED_SUBSCRIPTION','화재해상'),('FIXED_SUBSCRIPTION','삼성생명'),
    ('FIXED_SUBSCRIPTION','교보생명'),('FIXED_SUBSCRIPTION','한화생명'),('FIXED_SUBSCRIPTION','신한라이프'),('FIXED_SUBSCRIPTION','삼성화재'),
    ('FIXED_SUBSCRIPTION','현대해상'),('FIXED_SUBSCRIPTION','DB손해보험'),('FIXED_SUBSCRIPTION','KB손해보험'),('FIXED_SUBSCRIPTION','메리츠화재'),
    ('FIXED_SUBSCRIPTION','넷플릭스'),('FIXED_SUBSCRIPTION','NETFLIX'),('FIXED_SUBSCRIPTION','유튜브프리미엄'),
    ('FIXED_SUBSCRIPTION','YOUTUBEPREMIUM'),('FIXED_SUBSCRIPTION','디즈니플러스'),('FIXED_SUBSCRIPTION','DISNEYPLUS'),
    ('FIXED_SUBSCRIPTION','스포티파이'),('FIXED_SUBSCRIPTION','멜론'),('FIXED_SUBSCRIPTION','지니뮤직'),('FIXED_SUBSCRIPTION','FLO'),
    ('FIXED_SUBSCRIPTION','왓챠'),('FIXED_SUBSCRIPTION','티빙'),('FIXED_SUBSCRIPTION','웨이브'),('FIXED_SUBSCRIPTION','쿠팡플레이'),
    ('FIXED_SUBSCRIPTION','애플뮤직'),('FIXED_SUBSCRIPTION','APPLEMUSIC'),('FIXED_SUBSCRIPTION','밀리의서재'),
    ('FIXED_SUBSCRIPTION','ICLOUD'),('FIXED_SUBSCRIPTION','구글원'),('FIXED_SUBSCRIPTION','GOOGLEONE'),('FIXED_SUBSCRIPTION','네이버플러스'),
    ('FIXED_SUBSCRIPTION','쿠팡와우'),('FIXED_SUBSCRIPTION','MICROSOFT365'),('FIXED_SUBSCRIPTION','OFFICE365'),
    ('FIXED_SUBSCRIPTION','ADOBE'),('FIXED_SUBSCRIPTION','NOTION'),('FIXED_SUBSCRIPTION','CHATGPT'),('FIXED_SUBSCRIPTION','관리비'),
    ('FIXED_SUBSCRIPTION','도시가스'),('FIXED_SUBSCRIPTION','서울도시가스'),('FIXED_SUBSCRIPTION','예스코'),('FIXED_SUBSCRIPTION','삼천리'),
    ('FIXED_SUBSCRIPTION','코원에너지서비스'),('FIXED_SUBSCRIPTION','한국전력'),('FIXED_SUBSCRIPTION','전기요금'),
    ('FIXED_SUBSCRIPTION','가스요금'),('FIXED_SUBSCRIPTION','수도요금'),('FIXED_SUBSCRIPTION','월세'),
    ('FIXED_SUBSCRIPTION','임대료'),('FIXED_SUBSCRIPTION','인터넷요금'),('FIXED_SUBSCRIPTION','IPTV'),('FIXED_SUBSCRIPTION','정수기')
) AS k(category_name, keyword)
JOIN categories c ON c.category_name = k.category_name;

-- ---------------------------------------------------------------------
-- 확인
-- ---------------------------------------------------------------------
SELECT c.category_name, c.dutch_threshold, COUNT(r.rule_id) AS keyword_count
FROM categories c
LEFT JOIN category_rules r ON r.category_id = c.category_id
GROUP BY c.category_id, c.category_name, c.dutch_threshold
ORDER BY c.category_id;
