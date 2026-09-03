package com.goodbuy.backend.catalog;

import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** DB를 사용할 수 없을 때 분석을 끝까지 수행하기 위한 최소 카테고리 기준입니다. */
@Component
public class FallbackCategoryCatalog {

	private static final String GIF_BASE_URL =
			"https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/";

	private static final List<CategoryDefault> DEFAULTS = List.of(
			category(1, PurposeCategory.FOOD, 30_000,
					"메가엠지씨커피", "컴포즈커피", "스타벅스", "투썸", "커피", "카페",
					"버거앤타코", "맘스터치", "버거킹", "맥도날드", "롯데리아", "칼국수", "식당", "갈비"),
			category(2, PurposeCategory.TRANSPORT, 30_000,
					"카카오택시", "카카오T", "택시", "버스", "지하철", "코레일", "주유", "충전소"),
			category(3, PurposeCategory.LIVING, 50_000,
					"세븐일레븐", "이마트24", "GS25", "CU", "다이소", "마트", "편의점"),
			category(4, PurposeCategory.SHOPPING, 100_000,
					"네이버쇼핑", "무신사", "쿠팡", "백화점", "아울렛", "쇼핑"),
			category(5, PurposeCategory.CULTURE_HOBBY, 50_000,
					"CGV", "메가박스", "롯데시네마", "교보문고", "게임", "공연", "영화"),
			category(6, PurposeCategory.HEALTH, 50_000,
					"약국", "병원", "의원", "치과", "헬스", "필라테스"),
			category(7, PurposeCategory.EDUCATION, 100_000,
					"학원", "교육", "인강", "강의", "학교"),
			category(8, PurposeCategory.FIXED_SUBSCRIPTION, 150_000,
					"넷플릭스", "유튜브", "스포티파이", "통신", "보험", "구독"),
			category(9, PurposeCategory.OTHER, 100_000));

	private final NameNormalizer normalizer;

	public FallbackCategoryCatalog(NameNormalizer normalizer) {
		this.normalizer = normalizer;
	}

	public List<CategoryMapping> mappings() {
		List<CategoryMapping> mappings = new ArrayList<>();
		int ruleId = 1;

		for (CategoryDefault category : DEFAULTS) {
			if (category.keywords().isEmpty()) {
				mappings.add(toMapping(null, category, null));
				continue;
			}
			for (String keyword : category.keywords()) {
				mappings.add(toMapping(-ruleId++, category, keyword));
			}
		}

		return mappings.stream()
				.sorted(Comparator
						.comparingInt((CategoryMapping mapping) -> keywordLength(mapping.normalizedKeyword()))
						.reversed()
						.thenComparing(CategoryMapping::categoryId))
				.toList();
	}

	private CategoryMapping toMapping(Integer ruleId, CategoryDefault category, String keyword) {
		return new CategoryMapping(
				ruleId,
				category.id(),
				keyword == null ? null : normalizer.normalize(keyword),
				category.category(),
				category.dutchThreshold(),
				GIF_BASE_URL + category.category() + ".gif");
	}

	private int keywordLength(String keyword) {
		return keyword == null ? -1 : keyword.length();
	}

	private static CategoryDefault category(
			int id,
			PurposeCategory category,
			int dutchThreshold,
			String... keywords) {
		return new CategoryDefault(id, category, dutchThreshold, List.of(keywords));
	}

	private record CategoryDefault(
			int id,
			PurposeCategory category,
			int dutchThreshold,
			List<String> keywords) {
	}
}
