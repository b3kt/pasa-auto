package com.github.b3kt.application.helper;

import java.util.regex.Pattern;

/**
 * Canonicalizes phone numbers so values that differ only by formatting still resolve to the same
 * record. The canonical form is digits only in Indonesian national format: a leading {@code 62}
 * (or the doubled {@code 620}) country prefix becomes {@code 0}, e.g.
 * {@code "+62 812-3456-7890"} -> {@code "081234567890"}.
 *
 * <p>Matches the canonical form written by the V35 migration (ADR-0011) to
 * {@code tb_pelanggan.no_hp}/{@code no_telepon} and {@code tb_customer_accounts.phone}.
 */
public final class PhoneNormalizer {

    /** Canonical national number: a leading 0 and 8-15 digits in total. */
    private static final Pattern VALID = Pattern.compile("^0[0-9]{7,14}$");

    private PhoneNormalizer() {
    }

    /**
     * @return true when {@code phone} normalizes to a plausible national number, i.e. it can serve as
     *         a login identifier.
     */
    public static boolean isValid(String phone) {
        String canonical = normalize(phone);
        return canonical != null && VALID.matcher(canonical).matches();
    }

    /**
     * @return the canonical phone number, or {@code null} when {@code phone} carries no digits.
     */
    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder(phone.length());
        for (int i = 0; i < phone.length(); i++) {
            char c = phone.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.isEmpty()) {
            return null;
        }
        String value = digits.toString();
        if (value.startsWith("620")) {
            return "0" + value.substring(3);
        }
        if (value.startsWith("62")) {
            return "0" + value.substring(2);
        }
        return value;
    }
}
