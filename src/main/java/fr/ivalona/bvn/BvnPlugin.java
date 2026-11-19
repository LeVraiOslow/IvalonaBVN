package fr.ivalona.bvn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BvnPlugin extends JavaPlugin {

    /** MiniMessage standard + tag {@code <glyph:id>} de Nexo. */
    private final MiniMessage mm = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.defaults())
                    .resolver(new NexoGlyphs(this).resolver())
                    .build())
            .build();

    /** Accueils en cours, par UUID du nouveau joueur. Uniquement accédé depuis le thread principal. */
    private final Map<UUID, WelcomeSession> sessions = new HashMap<>();
    private Economy economy;
    private Chat chat;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new WelcomeListener(this), this);

        BvnCommand command = new BvnCommand(this);
        var pluginCommand = getCommand("bvn");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // On vérifie Vault une fois que tous les plugins (et leur économie) sont chargés
        getServer().getScheduler().runTask(this, () -> {
            if (economy() == null) {
                getLogger().warning("Aucune économie Vault trouvée ! Installe un plugin d'économie "
                        + "(ex. EssentialsX) : sans lui, aucune récompense ne sera versée.");
            } else {
                getLogger().info("Économie Vault détectée : " + economy.getName());
            }
        });
    }

    @Override
    public void onDisable() {
        sessions.clear();
    }

    /** Récupère l'économie Vault (de façon paresseuse, pour ne pas dépendre de l'ordre de chargement). */
    private Economy economy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        return economy;
    }

    /** Récupère le service Vault Chat (grades / préfixes, ex. via LuckPerms), de façon paresseuse. */
    private Chat chat() {
        if (chat == null) {
            RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                chat = rsp.getProvider();
            }
        }
        return chat;
    }

    private String prefixOf(Player player) {
        Chat c = chat();
        String value = c == null ? null : c.getPlayerPrefix(player);
        return value == null ? "" : value;
    }

    private String suffixOf(Player player) {
        Chat c = chat();
        String value = c == null ? null : c.getPlayerSuffix(player);
        return value == null ? "" : value;
    }

    /**
     * Construit un message depuis la config. Supporte :
     * les couleurs classiques (&amp;a, &amp;l, &amp;#rrggbb...), MiniMessage, les glyphs Nexo (&lt;glyph:id&gt;)
     * et les variables %player%, %seconds%, %reward%.
     */
    public Component msg(String path, TagResolver... resolvers) {
        return msg(path, null, resolvers);
    }

    /**
     * Idem, avec en plus les variables du joueur « sender » : %sender% (son pseudo),
     * %prefix% et %suffix% (son grade, via Vault/LuckPerms).
     */
    public Component msg(String path, Player sender, TagResolver... resolvers) {
        String raw = getConfig().getString("messages." + path, "<red>Message manquant : " + path);
        if (sender != null) {
            raw = raw.replace("%prefix%", prefixOf(sender))
                    .replace("%suffix%", suffixOf(sender))
                    .replace("%sender%", sender.getName());
        }
        raw = LegacyColors.toMiniMessage(raw)
                .replace("%player%", "<player>")
                .replace("%seconds%", "<seconds>")
                .replace("%reward%", "<reward>");
        return mm.deserialize(raw, resolvers);
    }

    /** Ouvre la fenêtre d'accueil pour un nouveau joueur et prévient tous les autres. */
    public void startSession(Player newcomer) {
        int seconds = Math.max(1, getConfig().getInt("duration-seconds", 20));
        WelcomeSession session = new WelcomeSession(
                newcomer.getUniqueId(), newcomer.getName(),
                System.currentTimeMillis() + seconds * 1000L);
        sessions.put(newcomer.getUniqueId(), session);

        Component announce = msg("announce",
                Placeholder.unparsed("player", newcomer.getName()),
                Placeholder.unparsed("seconds", String.valueOf(seconds)));
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(newcomer.getUniqueId())) {
                other.sendMessage(announce);
            }
        }

        // Fermeture de la fenêtre après X secondes (le contrôle par horodatage reste la sécurité principale)
        getServer().getScheduler().runTaskLater(this,
                () -> sessions.remove(newcomer.getUniqueId(), session), seconds * 20L);
    }

    /** Annule l'accueil si le nouveau joueur se déconnecte (option de config). */
    public void onNewcomerQuit(Player player) {
        if (getConfig().getBoolean("cancel-if-newcomer-leaves", true)) {
            sessions.remove(player.getUniqueId());
        }
    }

    /** Logique de /bvn. */
    public void handleBvn(Player player) {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> s.expired(now));

        if (sessions.isEmpty()) {
            player.sendMessage(msg("nobody"));
            return;
        }

        List<WelcomeSession> others = sessions.values().stream()
                .filter(s -> !s.newcomer().equals(player.getUniqueId()))
                .toList();
        if (others.isEmpty()) {
            player.sendMessage(msg("self"));
            return;
        }

        List<WelcomeSession> claimable = others.stream()
                .filter(s -> !s.claimed().contains(player.getUniqueId()))
                .toList();
        if (claimable.isEmpty()) {
            player.sendMessage(msg("already"));
            return;
        }

        double amount = Math.max(0, getConfig().getDouble("reward", 100.0));
        Economy eco = economy();
        if (amount > 0 && eco == null) {
            getLogger().warning("Récompense impossible : aucune économie Vault disponible.");
            player.sendMessage(msg("economy-error"));
            return;
        }

        for (WelcomeSession session : claimable) {
            if (amount > 0) {
                EconomyResponse response = eco.depositPlayer(player, amount);
                if (!response.transactionSuccess()) {
                    getLogger().warning("Échec du versement à " + player.getName() + " : " + response.errorMessage);
                    player.sendMessage(msg("economy-error"));
                    continue; // pas marqué comme réclamé : il pourra réessayer tant que c'est ouvert
                }
            }

            // Un seul /bvn par joueur et par nouvel arrivant
            session.claimed().add(player.getUniqueId());

            Bukkit.broadcast(msg("welcome", player, Placeholder.unparsed("player", session.name())));

            if (amount > 0) {
                player.sendMessage(msg("reward",
                        Placeholder.unparsed("reward", eco.format(amount))));
            }
        }
    }
}
