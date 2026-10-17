package com.github.b3kt.application.helper;

/**
 * Canonicalizes vehicle plate numbers (nopol) so values that differ only by whitespace or case
 * still resolve to the same record. The canonical form is UPPERCASE with all whitespace removed,
 * e.g. {@code "B 1019 CYM"} -> {@code "B1019CYM"}.
 *
 * <p>This matches the canonical form written to {@code tb_pelanggan_kendaraan.nopol} by the Phase 0
 * reconciliation migration, and is used by the ownership-based compatibility read layer while
 * {@code tb_pelanggan.nopol} is being phased out.
 */
public final class NopolNormalizer {

    private NopolNormalizer() {
    }

    /**
     * @return the canonical plate, or an empty string when {@code nopol} is null/blank.
     */
    public static String normalize(String nopol) {
        if (nopol == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(nopol.length());
        for (int i = 0; i < nopol.length(); i++) {
            char c = nopol.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * @return true when the plate carries real data (not null/blank and not the {@code "-"} placeholder).
     */
    public static boolean isMeaningful(String nopol) {
        String norm = normalize(nopol);
        return !norm.isEmpty() && !"-".equals(norm);
    }
}
