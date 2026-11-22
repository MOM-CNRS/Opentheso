package fr.cnrs.opentheso.concept.support;

import org.apache.commons.lang3.StringUtils;

/**
 * Code-barres Code 128 (jeu B) pour la fiche concept.
 */
public final class ConceptBarcodeSvgSupport {

    private static final int QUIET = 10;
    private static final int BAR_HEIGHT = 70;
    private static final String[] PATTERNS = {
            "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
            "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
            "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
            "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
            "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
            "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
            "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
            "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
            "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
            "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
            "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    };
    private static final int START_B = 104;
    private static final int STOP = 106;

    private ConceptBarcodeSvgSupport() {
    }

    public static String toSvg(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (text.isEmpty() || !isCodeSetB(text)) {
            return "";
        }
        int[] codes = new int[text.length() + 3];
        codes[0] = START_B;
        int checksum = START_B;
        for (int i = 0; i < text.length(); i++) {
            int code = text.charAt(i) - 32;
            codes[i + 1] = code;
            checksum += code * (i + 1);
        }
        codes[codes.length - 2] = checksum % 103;
        codes[codes.length - 1] = STOP;

        int modules = QUIET * 2;
        for (int code : codes) {
            modules += widthOf(PATTERNS[code]);
        }
        int height = BAR_HEIGHT + 16;
        StringBuilder svg = new StringBuilder(256 + modules * 24);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(modules).append(' ').append(height)
                .append("\" class=\"cv-barcode\" role=\"img\" aria-label=\"Code-barres\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>");
        int x = QUIET;
        for (int code : codes) {
            x = appendPattern(svg, PATTERNS[code], x);
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private static boolean isCodeSetB(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < 32 || ch > 126) {
                return false;
            }
        }
        return true;
    }

    private static int widthOf(String pattern) {
        int width = 0;
        for (int i = 0; i < pattern.length(); i++) {
            width += pattern.charAt(i) - '0';
        }
        return width;
    }

    private static int appendPattern(StringBuilder svg, String pattern, int x) {
        boolean bar = true;
        for (int i = 0; i < pattern.length(); i++) {
            int width = pattern.charAt(i) - '0';
            if (bar) {
                svg.append("<rect x=\"").append(x)
                        .append("\" y=\"8\" width=\"").append(width)
                        .append("\" height=\"").append(BAR_HEIGHT)
                        .append("\" fill=\"#111\"/>");
            }
            x += width;
            bar = !bar;
        }
        return x;
    }
}
