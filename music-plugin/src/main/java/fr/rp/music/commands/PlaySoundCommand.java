package fr.rp.music.commands;

import fr.rp.music.MusicRP;
import fr.rp.music.managers.PackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlaySoundCommand implements CommandExecutor {

    private final MusicRP plugin;

    public PlaySoundCommand(MusicRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("musicrp.use")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /playsound <url_directe_vers_ogg> [joueur|all]");
            return true;
        }

        String url = args[0];
        String cible = args.length >= 2 ? args[1] : null;

        sender.sendMessage(ChatColor.YELLOW + "Telechargement et preparation du son en cours...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PackManager.ResultatPack resultat = plugin.getPackManager().construirePackDepuisUrl(url);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (cible == null || cible.equalsIgnoreCase("all")) {
                        for (Player joueur : Bukkit.getOnlinePlayers()) {
                            envoyerPack(joueur, resultat);
                        }
                        sender.sendMessage(ChatColor.GREEN + "✔ Son envoye a tous les joueurs en ligne.");
                    } else {
                        Player joueur = Bukkit.getPlayer(cible);
                        if (joueur == null) {
                            sender.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne.");
                            return;
                        }
                        envoyerPack(joueur, resultat);
                        sender.sendMessage(ChatColor.GREEN + "✔ Son envoye a " + joueur.getName() + ".");
                    }
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(ChatColor.RED + "Erreur : " + e.getMessage()));
                plugin.getLogger().warning("Erreur playsound: " + e.getMessage());
            }
        });

        return true;
    }

    private void envoyerPack(Player joueur, PackManager.ResultatPack resultat) {
        UUID uuidPack = UUID.nameUUIDFromBytes(resultat.hash().getBytes());
        joueur.setResourcePack(uuidPack, resultat.urlPublique(), resultat.hash(), null, false);
    }
}
