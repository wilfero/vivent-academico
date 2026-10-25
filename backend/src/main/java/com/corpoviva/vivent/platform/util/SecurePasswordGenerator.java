package com.corpoviva.vivent.platform.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecurePasswordGenerator {

	private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
	private static final String DIGITS = "23456789";
	private static final String SPECIAL = "!@#$%&*+-=";
	private static final int DEFAULT_LENGTH = 16;

	private static final SecureRandom RANDOM = new SecureRandom();

	private SecurePasswordGenerator() {
	}

	public static String generate() {
		List<Character> characters = new ArrayList<>();

		characters.add(randomFrom(UPPER));
		characters.add(randomFrom(LOWER));
		characters.add(randomFrom(DIGITS));
		characters.add(randomFrom(SPECIAL));

		String all = UPPER + LOWER + DIGITS + SPECIAL;
		while (characters.size() < DEFAULT_LENGTH) {
			characters.add(randomFrom(all));
		}

		Collections.shuffle(characters, RANDOM);

		StringBuilder password = new StringBuilder(characters.size());
		characters.forEach(password::append);
		return password.toString();
	}

	private static char randomFrom(String source) {
		return source.charAt(RANDOM.nextInt(source.length()));
	}
}
