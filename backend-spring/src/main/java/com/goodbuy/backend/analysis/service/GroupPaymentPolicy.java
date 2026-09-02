package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.MerchantType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GroupPaymentPolicy {

	private static final long MINIMUM_ESTIMATED_PEOPLE = 3;
	private static final Map<MerchantType, Long> REFERENCE_AMOUNT_PER_PERSON = Map.of(
			MerchantType.CAFE, 7_000L,
			MerchantType.FAST_FOOD, 12_000L,
			MerchantType.RESTAURANT, 18_000L,
			MerchantType.MEAT_RESTAURANT, 35_000L);

	public boolean isCandidate(MerchantType merchantType, long amount) {
		Long referenceAmount = REFERENCE_AMOUNT_PER_PERSON.get(merchantType);
		return referenceAmount != null
				&& amount >= Math.multiplyExact(referenceAmount, MINIMUM_ESTIMATED_PEOPLE);
	}
}
