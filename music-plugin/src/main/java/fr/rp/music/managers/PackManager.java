package fr.rp.music.managers;

import fr.rp.music.MusicRP;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackManager {

    private final MusicRP plugin;
    private final GitHubUploader gitHubUploader;
    private File dossierPacks;

    public static final String CLE_SON = "musicrp:custom_sound";
    private static final String NOM_FICHIER_DISTANT = "musicrp-pack.zip";

    public PackManager(MusicRP plugin) {
        this.plugin = plugin;
        this.gitHubUploader = new GitHubUploader(plugin);
    }

    public void demarrer() {
        dossierPacks = new File(plugin.getDataFolder(), "packs");
        if (!dossierPacks.exists()) dossierPacks.mkdirs();
    }

    public ResultatPack construirePackDepuisUrl(String urlAudio) throws IOException, java.security.NoSuchAlgorithmException {
        File fichierAudio = telechargerFichier(urlAudio);
        File fichierZip = new File(dossierPacks, "pack.zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fichierZip))) {
            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            String mcmeta = """
                {
                  "pack": {
                    "pack_format": 34,
                    "description": "MusicRP - son personnalise"
                  }
                }
                """;
            zos.write(mcmeta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("assets/musicrp/sounds.json"));
            String soundsJson = """
                {
                  "custom_sound": {
                    "sounds": ["custom_sound"]
                  }
                }
                """;
            zos.write(soundsJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("assets/musicrp/sounds/custom_sound.ogg"));
            Files.copy(fichierAudio.toPath(), zos);
            zos.closeEntry();
        }

        String urlPublique = gitHubUploader.uploaderFichier(fichierZip, NOM_FICHIER_DISTANT);
        String hash = calculerSha1(fichierZip);

        return new ResultatPack(urlPublique, hash);
    }

    private File telechargerFichier(String urlSource) throws IOException {
        File destination = new File(dossierPacks, "source_audio.ogg");
        URL url = new URL(urlSource);
        HttpURLConnection connexion = (HttpURLConnection) url.openConnection();
        connexion.setInstanceFollowRedirects(true);
        connexion.setConnectTimeout(10_000);
        connexion.setReadTimeout(20_000);
        connexion.setRequestProperty("User-Agent", "MusicRP-Plugin");

        try (InputStream in = connexion.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            in.transferTo(out);
        } finally {
            connexion.disconnect();
        }
        return destination;
    }

    private String calculerSha1(File fichier) throws IOException, java.security.NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream in = new FileInputStream(fichier)) {
            byte[] tampon = new byte[8192];
            int lus;
            while ((lus = in.read(tampon)) != -1) {
                digest.update(tampon, 0, lus);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public record ResultatPack(String urlPublique, String hash) {}
}
