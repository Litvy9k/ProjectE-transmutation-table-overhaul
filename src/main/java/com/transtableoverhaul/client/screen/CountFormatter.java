package com.transtableoverhaul.client.screen;

import java.math.BigInteger;

/**
 * AE2 style compact amounts: exact up to 9999, then 10K, 123K, 1.2M ...
 */
public final class CountFormatter {

    private static final String[] UNITS = {"K", "M", "G", "T", "P", "E", "Z", "Y"};
    private static final BigInteger EXACT_LIMIT = BigInteger.valueOf(10_000);

    private CountFormatter() {
    }

    public static String compact(BigInteger amount) {
        if (amount.compareTo(EXACT_LIMIT) < 0) {
            return amount.toString();
        }
        String digits = amount.toString();
        int group = (digits.length() - 1) / 3;
        if (group > UNITS.length) {
            return "∞";
        }
        int leading = digits.length() - group * 3;
        String unit = UNITS[group - 1];
        if (leading == 1) {
            return digits.charAt(0) + "." + digits.charAt(1) + unit;
        }
        return digits.substring(0, leading) + unit;
    }
}
