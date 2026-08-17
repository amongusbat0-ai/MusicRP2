package fr.rp.music.managers;

import fr.rp.music.MusicRP;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Envoie le fichier de resource pack genere vers un depot GitHub via l'API
 * Contents, puis renvoie l'URL brute (raw.githubusercontent.com) utilisable
 * par Player#setResourcePack. Cette methode ne necessite AUCUN port ouvert
 * sur le serveur d'hebergement Minecraft : seule une requete HTTPS SORTANTE
 * est effectuee, ce qui fonctionne meme sur des hebergeurs limites (Falix
 * gratuit, Aternos, etc.) qui ne permettent pas d'ouvrir un port entrant.
 */
public class GitHubUploader {

    private final MusicRP plugin;

    public GitHubUploader(MusicRP plugin) {
        this.plugin = plugin;
    }

    /**
     * Upload le fichier vers <depot>/<chemin> et renvoie l'URL raw publique.
     */
    public String uploaderFichier(File fichier, String nomFichierDistant) throws IOException {
        String token = plugin.getConfig().getString("github-token", "");
        String depot = plugin.getConfig().getString("github-depot", "");
        String branche = plugin.getConfig().getString("github-branche", "main");

        if (token.isBlank() || depot.isBlank()) {
            throw new IOException("github-token ou github-depot non configure dans config.yml !");
        }

        byte[] contenu = readAllBytes(fichier);
        String contenuBase64 = Base64.getEncoder().encodeToString(contenu);

        String shaExistant = recupererShaExistant(depot, nomFichierDistant, token, branche);

        String urlApi = "https://api.github.com/repos/" + depot + "/contents/" + nomFichierDistant;
        HttpURLConnection connexion = (HttpURLConnection) new URL(urlApi).openConnection();
        connexion.setRequestMethod("PUT");
        connexion.setRequestProperty("Authorization", "Bearer " + token);
        connexion.setRequestProperty("Accept", "application/vnd.github+json");
        connexion.setRequestProperty("Content-Type", "application/json");
        connexion.setDoOutput(true);

        StringBuilder corpsJson = new StringBuilder();
        corpsJson.append("{");
        corpsJson.append("\"message\":\"Upload son MusicRP\",");
        corpsJson.append("\"content\":\"").append(contenuBase64).append("\",");
        corpsJson.append("\"branch\":\"").append(branche).append("\"");
        if (shaExistant != null) {
            corpsJson.append(",\"sha\":\"").append(shaExistant).append("\"");
        }
        corpsJson.append("}");

        try (OutputStream os = connexion.getOutputStream()) {
            os.write(corpsJson.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connexion.getResponseCode();
        if (code != 200 && code != 201) {
            String erreur = lireFlux(connexion.getErrorStream());
            connexion.disconnect();
            throw new IOException("Echec de l'upload GitHub (code " + code + ") : " + erreur);
        }
        connexion.disconnect();

        return "https://raw.githubusercontent.com/" + depot + "/" + branche + "/" + nomFichierDistant;
    }

    /** Recupere le SHA du fichier existant sur GitHub s'il y en a deja un (necessaire pour le remplacer) */
    private String recupererShaExistant(String depot, String chemin, String token, String branche) {
        try {
            String urlApi = "https://api.github.com/repos/" + depot + "/contents/" + chemin + "?ref=" + branche;
            HttpURLConnection connexion = (HttpURLConnection) new URL(urlApi).openConnection();
            connexion.setRequestMethod("GET");
            connexion.setRequestProperty("Authorization", "Bearer " + token);
            connexion.setRequestProperty("Accept", "application/vnd.github+json");

            int code = connexion.getResponseCode();
            if (code != 200) {
                connexion.disconnect();
                return null;
            }
            String reponse = lireFlux(connexion.getInputStream());
            connexion.disconnect();

            int index = reponse.indexOf("\"sha\":\"");
            if (index == -1) return null;
            int debut = index + 7;
            int fin = reponse.indexOf("\"", debut);
            return reponse.substring(debut, fin);
        } catch (IOException e) {
            return null;
        }
    }

    private String lireFlux(InputStream in) throws IOException {
        if (in == null) return "";
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            in.transferTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private byte[] readAllBytes(File fichier) throws IOException {
        try (InputStream in = new FileInputStream(fichier)) {
            return in.readAllBytes();
        }
    }
}
