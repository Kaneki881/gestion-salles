package com.example.web;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.StatutReservation;
import com.example.model.Utilisateur;
import com.example.service.ReservationService;
import com.example.service.SalleService;
import com.example.service.UtilisateurService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class WebApp {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final EntityManagerFactory emf;
    private final UtilisateurService utilisateurs;
    private final SalleService salles;
    private final ReservationService reservations;

    public WebApp(EntityManagerFactory emf) {
        this.emf = emf;
        utilisateurs = new UtilisateurService(emf);
        salles = new SalleService(emf);
        reservations = new ReservationService(emf);
    }

    public static void main(String[] args) throws IOException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-salles");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        WebApp app = new WebApp(emf);
        server.createContext("/", app::handle);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            emf.close();
        }));
        server.start();
        System.out.println("Gestion des salles : http://127.0.0.1:8080/");
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/".equals(path)) {
                String query = exchange.getRequestURI().getRawQuery();
                String message = query == null ? "" : formValues(query).getOrDefault("message", "");
                send(exchange, 200, page(message, false));
                return;
            }
            if ("POST".equals(method)) {
                Map<String, String> values = readForm(exchange);
                String message;
                switch (path) {
                    case "/utilisateurs" -> {
                        utilisateurs.save(new Utilisateur(required(values, "nom"),
                                required(values, "prenom"), required(values, "email")));
                        message = "Utilisateur créé.";
                    }
                    case "/salles" -> {
                        Salle salle = new Salle(required(values, "nom"),
                                Integer.parseInt(required(values, "capacite")));
                        salle.setEtage(optionalInt(values, "etage"));
                        salle.setDescription(values.getOrDefault("description", "").trim());
                        salles.save(salle);
                        message = "Salle créée.";
                    }
                    case "/reservations" -> {
                        reservations.reserver(Long.parseLong(required(values, "salleId")),
                                Long.parseLong(required(values, "utilisateurId")),
                                LocalDateTime.parse(required(values, "debut")),
                                LocalDateTime.parse(required(values, "fin")));
                        message = "Réservation confirmée.";
                    }
                    case "/reservations/annuler" -> {
                        reservations.annuler(Long.parseLong(required(values, "id")));
                        message = "Réservation annulée.";
                    }
                    default -> {
                        send(exchange, 404, page("Page introuvable.", true));
                        return;
                    }
                }
                exchange.getResponseHeaders().set("Location", "/?message=" +
                        URLEncoder.encode(message, StandardCharsets.UTF_8));
    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                } else {
    send(exchange, 404, page("Page introuvable.", true));
            }
            } catch (IllegalArgumentException e) {
    send(exchange, 400, page(e.getMessage(), true));
            } catch (RuntimeException e) {
    send(exchange, 400, page("Opération impossible. Vérifiez les données saisies.", true));
            }
            }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Format de formulaire invalide.");
        }
        byte[] body = exchange.getRequestBody().readNBytes(8193);
        if (body.length > 8192) throw new IllegalArgumentException("Formulaire trop volumineux.");
        return formValues(new String(body, StandardCharsets.UTF_8));
    }

    private static Map<String, String> formValues(String encoded) {
        Map<String, String> result = new HashMap<>();
        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(parts.length == 2 ? parts[1] : "", StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.getOrDefault(key, "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Le champ " + key + " est obligatoire.");
        return value;
    }

    private static Integer optionalInt(Map<String, String> values, String key) {
        String value = values.getOrDefault(key, "").trim();
        return value.isEmpty() ? null : Integer.parseInt(value);
    }

    private String page(String message, boolean error) {
        StringBuilder html = new StringBuilder("<!doctype html><html lang='fr'><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<title>Gestion des salles</title><style>")
                .append("body{font:16px system-ui,sans-serif;background:#f3f5f7;color:#17212b;margin:0}")
                .append("header{background:#17324d;color:white;padding:24px max(20px,calc((100vw - 1100px)/2))}")
                .append("main{max-width:1100px;margin:26px auto;padding:0 20px}")
                .append("section{background:white;border-radius:12px;padding:20px;margin:20px 0;box-shadow:0 2px 12px #0001}")
                .append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:20px}")
                .append(".grid section{margin:0}form{display:grid;gap:10px}label{font-weight:600}")
                .append("input,select,textarea,button{font:inherit;padding:9px;border:1px solid #bec8d2;border-radius:6px}")
                .append("button{background:#116aa3;color:white;border:0;cursor:pointer}button:hover{background:#0d4f7a}")
                .append("table{width:100%;border-collapse:collapse}td,th{text-align:left;padding:9px;border-bottom:1px solid #ddd}")
                .append(".scroll{overflow-x:auto}.notice{padding:12px;border-radius:7px;background:#def4e4}")
                .append(".error{background:#fde2e2}.muted{color:#5c6873}.cancel{background:#a33333}")
                .append("</style></head><body><header><h1>Gestion des salles</h1>")
                .append("<p>Utilisateurs, salles et réservations</p></header><main>");
        if (message != null && !message.isBlank()) {
            html.append("<p class='notice").append(error ? " error" : "")
                    .append("'>").append(escape(message)).append("</p>");
        }
        html.append("<div class='grid'><section><h2>Nouvel utilisateur</h2><form method='post' action='/utilisateurs'>")
                .append("<label>Nom<input name='nom' required minlength='2' maxlength='50'></label>")
                .append("<label>Prénom<input name='prenom' required minlength='2' maxlength='50'></label>")
                .append("<label>Email<input name='email' type='email' required></label><button>Créer</button></form></section>")
                .append("<section><h2>Nouvelle salle</h2><form method='post' action='/salles'>")
                .append("<label>Nom<input name='nom' required minlength='2' maxlength='100'></label>")
                .append("<label>Capacité<input name='capacite' type='number' min='1' max='1000' required></label>")
                .append("<label>Étage<input name='etage' type='number' min='0'></label>")
                .append("<label>Description<textarea name='description' maxlength='500'></textarea></label>")
                .append("<button>Créer</button></form></section></div>");
        html.append("<section><h2>Nouvelle réservation</h2><form method='post' action='/reservations'>")
                .append("<label>Salle<select name='salleId' required><option value=''>Choisir une salle</option>");
        for (Salle salle : salles.findByDisponible(true)) {
            html.append("<option value='").append(salle.getId()).append("'>")
                    .append(escape(salle.getNom())).append(" (capacité ").append(salle.getCapacite())
                    .append(")</option>");
        }
        html.append("</select></label><label>Utilisateur<select name='utilisateurId' required>")
                .append("<option value=''>Choisir un utilisateur</option>");
        for (Utilisateur utilisateur : utilisateurs.findAll()) {
            html.append("<option value='").append(utilisateur.getId()).append("'>")
                    .append(escape(utilisateur.getPrenom() + " " + utilisateur.getNom())).append("</option>");
        }
        html.append("</select></label><label>Début<input name='debut' type='datetime-local' required></label>")
                .append("<label>Fin<input name='fin' type='datetime-local' required></label>")
                .append("<p class='muted'>Créneau futur, de 8 heures au maximum. Deux réservations d'une même salle ne peuvent pas se chevaucher.</p>")
                .append("<button>Réserver</button></form></section>")
                .append("<section><h2>Réservations</h2><div class='scroll'><table><thead><tr>")
                .append("<th>Salle</th><th>Utilisateur</th><th>Début</th><th>Fin</th><th>Statut</th><th></th>")
                .append("</tr></thead><tbody>");
        for (Reservation reservation : reservations.lister()) {
            html.append("<tr><td>").append(escape(reservation.getSalle().getNom()))
                    .append("</td><td>").append(escape(reservation.getUtilisateur().getPrenom() + " " +
                            reservation.getUtilisateur().getNom()))
                    .append("</td><td>").append(reservation.getDebut().format(DATE))
                    .append("</td><td>").append(reservation.getFin().format(DATE))
                    .append("</td><td>").append(reservation.getStatut() == StatutReservation.CONFIRMEE ? "Confirmée" : "Annulée")
                    .append("</td><td>");
            if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
                html.append("<form method='post' action='/reservations/annuler'>")
                        .append("<input type='hidden' name='id' value='").append(reservation.getId())
                        .append("'><button class='cancel'>Annuler</button></form>");
            }
            html.append("</td></tr>");
        }
        return html.append("</tbody></table></div></section></main></body></html>").toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void send(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }
    }