package fr.ivalona.bvn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class WelcomeListener implements Listener {

    private final BvnPlugin plugin;

    public WelcomeListener(BvnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // hasPlayedBefore() est false uniquement lors de la toute première connexion
        if (!player.hasPlayedBefore()) {
            plugin.startSession(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.onNewcomerQuit(event.getPlayer());
    }
}
