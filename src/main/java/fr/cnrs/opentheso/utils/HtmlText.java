package fr.cnrs.opentheso.utils;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rend le HTML de mise en forme des notes et des traductions.
 * Les autres balises sont retirées, leur texte est conservé.
 */
public final class HtmlText {

    private static final Set<String> TAGS = Set.of(
            "a", "b", "strong", "i", "em", "u", "s", "strike", "sup", "sub",
            "br", "p", "span", "div", "ul", "ol", "li", "small");
    private static final Set<String> INLINE = Set.of(
            "b", "strong", "i", "em", "u", "s", "strike", "sup", "sub", "br", "span", "small");
    private static final Set<String> STYLED = Set.of("span", "p", "div");
    private static final Set<String> STYLE_PROPS = Set.of(
            "color", "background-color", "font-weight", "font-style", "font-size", "text-decoration", "text-align");
    private static final Pattern COLOR = Pattern.compile(
            "#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})"
                    + "|rgb\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*\\)"
                    + "|rgba\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*(?:0|1|0?\\.\\d+)\\s*\\)"
                    + "|[a-zA-Z]{3,20}");
    private static final Pattern FONT_SIZE = Pattern.compile("\\d+(?:\\.\\d+)?(?:px|em|rem|%)");

    private HtmlText() {
    }

    public static String format(String value) {
        return render(value, false);
    }

    public static String formatInline(String value) {
        return render(value, true);
    }

    private static String render(String value, boolean inline) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String source = value;
        if (source.indexOf('<') < 0 && source.contains("&lt;")) {
            source = StringEscapeUtils.unescapeHtml4(source);
        }
        if (source.indexOf('<') < 0) {
            source = source.replace("\r\n", "\n").replace("\n", "<br>");
        }
        Document doc = Jsoup.parseBodyFragment(source);
        doc.select("script, style, iframe, object, embed, link, meta").remove();
        scrub(doc.body(), inline);
        doc.outputSettings()
                .prettyPrint(false)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8);
        return doc.body().html();
    }

    private static void scrub(Element parent, boolean inline) {
        boolean again = true;
        while (again) {
            again = false;
            for (Node node : new ArrayList<>(parent.childNodes())) {
                if (node instanceof TextNode) {
                    continue;
                }
                if (!(node instanceof Element element)) {
                    node.remove();
                    continue;
                }
                if (!allowed(element.normalName(), inline)) {
                    element.unwrap();
                    again = true;
                    break;
                }
            }
        }
        for (Element child : parent.children()) {
            scrub(child, inline);
            cleanAttributes(child);
        }
    }

    private static boolean allowed(String tag, boolean inline) {
        return inline ? INLINE.contains(tag) : TAGS.contains(tag);
    }

    private static void cleanAttributes(Element element) {
        for (String name : element.attributes().asList().stream().map(attr -> attr.getKey()).toList()) {
            if ("a".equals(element.normalName()) && ("href".equals(name) || "title".equals(name))) {
                continue;
            }
            if ("style".equals(name) && STYLED.contains(element.normalName())) {
                continue;
            }
            element.removeAttr(name);
        }
        if ("a".equals(element.normalName())) {
            if (!safeHref(element.attr("href"))) {
                element.removeAttr("href");
            } else {
                element.attr("rel", "noopener noreferrer");
                element.attr("target", "_blank");
            }
        }
        if (element.hasAttr("style")) {
            String safe = safeStyle(element.attr("style"));
            if (safe.isEmpty()) {
                element.removeAttr("style");
            } else {
                element.attr("style", safe);
            }
        }
    }

    private static boolean safeHref(String href) {
        if (href == null || href.isBlank() || href.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        try {
            URI uri = URI.create(href);
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https")
                    || scheme.equalsIgnoreCase("mailto"));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String safeStyle(String style) {
        StringBuilder out = new StringBuilder();
        for (String part : style.split(";")) {
            int colon = part.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String property = part.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String cssValue = part.substring(colon + 1).trim();
            if (!STYLE_PROPS.contains(property) || !safeCssValue(property, cssValue)) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append("; ");
            }
            out.append(property).append(": ").append(cssValue);
        }
        return out.toString();
    }

    private static boolean safeCssValue(String property, String cssValue) {
        if (cssValue.isEmpty() || cssValue.indexOf('!') >= 0 || cssValue.indexOf('(') >= 0 && !cssValue.regionMatches(true, 0, "rgb", 0, 3)) {
            return false;
        }
        String lower = cssValue.toLowerCase(Locale.ROOT);
        if (lower.contains("url") || lower.contains("expression") || lower.contains("javascript") || lower.contains("@")
                || cssValue.indexOf('<') >= 0 || cssValue.indexOf('>') >= 0 || cssValue.indexOf('\\') >= 0) {
            return false;
        }
        return switch (property) {
            case "color", "background-color" -> COLOR.matcher(cssValue).matches();
            case "font-size" -> FONT_SIZE.matcher(cssValue).matches();
            case "font-weight" -> cssValue.equals("normal") || cssValue.equals("bold") || cssValue.matches("[1-9]00");
            case "font-style" -> cssValue.equals("normal") || cssValue.equals("italic");
            case "text-decoration" -> cssValue.equals("none") || cssValue.equals("underline") || cssValue.equals("line-through");
            case "text-align" -> cssValue.equals("left") || cssValue.equals("right") || cssValue.equals("center") || cssValue.equals("justify");
            default -> false;
        };
    }
}
