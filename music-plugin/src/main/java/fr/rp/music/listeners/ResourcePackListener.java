package fr.rp.music.listeners;

import fr.rp.music.MusicRP;
import fr.rp.music.managers.PackManager;
import org.bukkit.ChatColor;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class ResourcePackListener implements Listener {

    private final MusicRP plugin;

    public ResourcePackListener(MusicRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        var joueur = event.getPlayer();

        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> {
                float volume = (float) plugin.getConfig().getDouble("volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("pitch", 1.0);
                joueur.playSound(joueur.getLocation(), PackManager.CLE_SON, SoundCategory.MASTER, volume, pitch);
            }
            case FAILED_DOWNLOAD -> joueur.sendMessage(ChatColor.RED +
                    "Le telechargement du son a echoue (verifie que le port du serveur est bien accessible).");
            case DECLINED -> joueur.sendMessage(ChatColor.RED +
                    "Tu as refuse le resource pack, le son ne peut pas etre joue.");
            default -> {}
        }
    }
}
