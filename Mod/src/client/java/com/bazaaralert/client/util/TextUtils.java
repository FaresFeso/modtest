package com.bazaaralert.client.util;

import java.util.regex.Pattern;

public final class TextUtils {
	private static final Pattern STRIP_COLOR = Pattern.compile("§.");

	private TextUtils() {
	}

	public static String stripColor(String text) {
		if (text == null) {
			return "";
		}
		return STRIP_COLOR.matcher(text).replaceAll("");
	}

	public static String displayNameToProductId(String displayName) {
		return stripColor(displayName)
			.trim()
			.toUpperCase()
			.replace(' ', '_')
			.replace('-', '_');
	}

	public static double parseDouble(String value) {
		return Double.parseDouble(value.replace(",", ""));
	}

	public static long parseLong(String value) {
		return Long.parseLong(value.replace(",", ""));
	}
}
