package fr.ivalona.bvn;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ajoute le tag MiniMessage {@code <glyph:id>} (ex. {@code <glyph:dollars1>}) en s'appuyant sur Nexo.
 * <p>
 * Nexo est appelé par réflexion : le plugin se compile sans dépendance à Nexo et fonctionne
 * aussi (sans glyphs) si Nexo n'est pas installé.
 */
final class NexoGlyphs {

    private final BvnPlugin plugin;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    NexoGlyphs(BvnPlugin plugin) {
        this.plugin = plugin;
    }

    TagResolver resolver() {
        return TagResolver.resolver("glyph", (args, ctx) -> {
            String id = args.popOr("Le tag glyph demande un identifiant : <glyph:id>").value();
            Component component = lookup(id);
            if (component == null) {
                return Tag.inserting(Component.empty());
            }
            return Tag.inserting(component);
        });
    }

    private Component lookup(String id) {
        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
        if (nexo == null || !nexo.isEnabled()) {
            warnOnce("nexo", "Nexo n'est pas chargé : les glyphs (<glyph:...>) sont ignorés.");
            return null;
        }
        try {
            Object fontManager = invoke(nexo, new String[]{"fontManager", "getFontManager"});
            if (fontManager == null) {
                warnOnce("api", "API Nexo introuvable (fontManager) : les glyphs sont ignorés.");
                return null;
            }
            Object glyph = invoke(fontManager, new String[]{"glyphFromID", "getGlyphFromID"}, id);
            if (glyph == null) {
                warnOnce("id:" + id, "Glyph Nexo inconnu : '" + id + "'.");
                return null;
            }
            Object character = invoke(glyph, new String[]{"character", "getCharacter"});
            if (character == null) {
                warnOnce("char:" + id, "Impossible de lire le caractère du glyph '" + id + "'.");
                return null;
            }
            Component component = Component.text(String.valueOf(character));

            // Police personnalisée éventuelle (on garde la police par défaut sinon)
            Object font = invoke(glyph, new String[]{"font", "getFont"});
            if (font != null) {
                String fontName = font.toString();
                if (!fontName.isBlank() && !fontName.equals("default") && !fontName.equals("minecraft:default")) {
                    try {
                        component = component.font(Key.key(fontName));
                    } catch (RuntimeException ignored) {
                        // police invalide : on affiche le caractère tel quel
                    }
                }
            }
            return component;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("err:" + id, "Erreur en lisant le glyph Nexo '" + id + "' : " + e);
            return null;
        }
    }

    /** Appelle la première méthode publique portant l'un des noms donnés et acceptant les arguments fournis. */
    private static Object invoke(Object target, String[] names, Object... args) throws ReflectiveOperationException {
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                boolean ok = true;
                for (int i = 0; i < types.length; i++) {
                    if (!types[i].isInstance(args[i])) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return method.invoke(target, args);
                }
            }
        }
        return null;
    }

    private void warnOnce(String key, String message) {
        if (warned.add(key)) {
            plugin.getLogger().warning(message);
        }
    }
}
