package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.MerchantType;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 업종별 1인 기준 금액을 이용해 단체 결제 가능성이 큰 단일 거래를 찾습니다. */
@Component
public class GroupPaymentPolicy {

	private static final long MINIMUM_ESTIMATED_PEOPLE = 3;
	private static final Map<MerchantType, Long> REFERENCE_AMOUNT_PER_PERSON = Map.of(
			MerchantType.CAFE, 7_000L,
			MerchantType.FAST_FOOD, 12_000L,
			MerchantType.RESTAURANT, 18_000L,
			MerchantType.MEAT_RESTAURANT, 35_000L);

	public boolean isCandidate(MerchantType merchantType, long amount) {
		Long referenceAmount = referenceAmountPerPerson(merchantType);
		// 현재 기준은 해당 업종의 예상 1인 금액 3배 이상입니다.
		return referenceAmount != null
				&& amount >= Math.multiplyExact(referenceAmount, MINIMUM_ESTIMATED_PEOPLE);
	}

	public Long referenceAmountPerPerson(MerchantType merchantType) {
		return REFERENCE_AMOUNT_PER_PERSON.get(merchantType);
	}

	public long candidateThreshold(MerchantType merchantType) {
		Long referenceAmount = referenceAmountPerPerson(merchantType);
		return referenceAmount == null
				? 0
				: Math.multiplyExact(referenceAmount, MINIMUM_ESTIMATED_PEOPLE);
	}
}
