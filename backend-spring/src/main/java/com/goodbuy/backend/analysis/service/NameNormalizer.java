package com.goodbuy.backend.analysis.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class NameNormalizer {

	public String normalize(String value) {
		if (value == null) {
			return "";
		}

		return Normalizer.normalize(value, Normalizer.Form.NFKC)
				.codePoints()
				.filter(codePoint -> !Character.isWhitespace(codePoint))
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString()
				.toUpperCase(Locale.ROOT);
	}
}
