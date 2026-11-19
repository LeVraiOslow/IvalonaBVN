package fr.ivalona.bvn;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convertit les codes couleur « classiques » de Minecraft (&amp;a, &amp;l, &amp;#ff8800, &amp;x&amp;f&amp;f..., et leur
 * variante avec §) en tags MiniMessage, pour pouvoir mélanger les deux syntaxes dans la config.
 */
final class LegacyColors {

    private static final Pattern PATTERN = Pattern.compile(
            "[&§](?:#([0-9a-fA-F]{6})|[xX](?:[&§][0-9a-fA-F]){6}|([0-9a-fA-Fk-oK-OrR]))");

    private static final Map<Character, String> TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private LegacyColors() {
    }

    static String toMiniMessage(String input) {
        if (input == null || input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }
        Matcher matcher = PATTERN.matcher(input);
        StringBuilder out = new StringBuilder(input.length() + 16);
        while (matcher.find()) {
            String replacement;
            if (matcher.group(1) != null) {
                // &#rrggbb
                replacement = "<#" + matcher.group(1) + ">";
            } else if (matcher.group(2) != null) {
                // &a, &l, &r...
                replacement = TAGS.get(Character.toLowerCase(matcher.group(2).charAt(0)));
            } else {
                // &x&r&r&g&g&b&b (format hexadécimal de Bukkit)
                String hex = matcher.group().substring(2).replace("&", "").replace("§", "");
                replacement = "<#" + hex + ">";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
