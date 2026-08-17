package fr.rp.music;

import fr.rp.music.commands.PlaySoundCommand;
import fr.rp.music.listeners.ResourcePackListener;
import fr.rp.music.managers.PackManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MusicRP extends JavaPlugin {

    private PackManager packManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        packManager = new PackManager(this);
        packManager.demarrer();

        getCommand("playsound").setExecutor(new PlaySoundCommand(this));
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);

        getLogger().info("MusicRP a ete active avec succes !");
        if (getConfig().getString("github-token", "").isBlank() || getConfig().getString("github-depot", "").isBlank()) {
            getLogger().warning("⚠ github-token ou github-depot non configure dans config.yml ! La commande /playsound ne fonctionnera pas tant que ce n'est pas fait.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("MusicRP a ete desactive.");
    }

    public PackManager getPackManager() { return packManager; }
}
