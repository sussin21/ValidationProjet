package com.example.app.services;

import com.example.app.entities.Produit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatbotService {

    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent";
    private static final Pattern TEXT_PATTERN = Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ProduitService produitService = new ProduitService();

    public boolean isConfigured() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    public String healthCheck() {
        return isConfigured() ? "Gemini chatbot prêt" : "Mode support local prêt";
    }

    public String chat(String message, Map<String, String> customerInfo) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return buildLocalSupportReply(message, customerInfo);
        }

        try {
            String prompt = buildPrompt(message, customerInfo);
            String requestBody = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_ENDPOINT + "?key=" + apiKey))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "Le service d'assistance est temporairement indisponible. Essayez à nouveau plus tard.";
            }

            String text = extractText(response.body());
            if (text.isBlank()) {
                return "Je n'ai pas pu générer de réponse utile pour le moment.";
            }

            return text;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "La demande a été interrompue avant de pouvoir obtenir une réponse.";
        } catch (IOException | java.sql.SQLException e) {
            return buildLocalSupportReply(message, customerInfo);
        }
    }

    public Map<String, String> buildDefaultCustomerInfo(String username, String orderNumber, String issueType) {
        Map<String, String> info = new LinkedHashMap<>();
        if (username != null && !username.isBlank()) {
            info.put("name", username.trim());
        }
        if (orderNumber != null && !orderNumber.isBlank()) {
            info.put("order_number", orderNumber.trim());
        }
        if (issueType != null && !issueType.isBlank()) {
            info.put("issue_type", issueType.trim());
        }
        return info;
    }

    private String buildPrompt(String message, Map<String, String> customerInfo) throws java.sql.SQLException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es l'assistant boutique Midgar. Réponds en français, de façon courte, utile et polie. ");
        prompt.append("Tu aides pour les produits, commandes et le support. Si tu ne sais pas, recommande de contacter un humain.\n\n");

        if (customerInfo != null && !customerInfo.isEmpty()) {
            prompt.append("Contexte client:\n");
            customerInfo.forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append('\n'));
            prompt.append('\n');
        }

        prompt.append("Contexte produits:\n");
        List<Produit> products = produitService.select();
        int limit = Math.min(products.size(), 5);
        for (int i = 0; i < limit; i++) {
            Produit product = products.get(i);
            prompt.append("- ")
                    .append(product.getNom())
                    .append(" | type: ")
                    .append(product.getType())
                    .append(" | prix: ")
                    .append(product.getPrix())
                    .append(" | stock: ")
                    .append(product.getQuantiteDisponible())
                    .append('\n');
        }

        prompt.append("\nMessage utilisateur: ").append(message == null ? "" : message.trim());
        return prompt.toString();
    }

    private String buildRequestBody(String prompt) {
        String escapedPrompt = escapeJson(prompt);
        return "{"
                + "\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.7,\"maxOutputTokens\":512}"
                + "}";
    }

    private String buildLocalSupportReply(String message, Map<String, String> customerInfo) {
        String normalized = message == null ? "" : message.toLowerCase();
        String customerName = customerInfo != null ? customerInfo.getOrDefault("name", "client") : "client";

        if (normalized.contains("commande") || normalized.contains("order")) {
            return "Bonjour " + customerName + ", je peux vous aider avec votre commande. Vérifiez le suivi dans Mes commandes ou donnez-moi votre numéro de commande.";
        }
        if (normalized.contains("livraison") || normalized.contains("shipping") || normalized.contains("expédition")) {
            return "Je peux vous aider pour la livraison. Indiquez votre numéro de commande et je vous dirai quoi vérifier ensuite.";
        }
        if (normalized.contains("retour") || normalized.contains("rembourse") || normalized.contains("refund")) {
            return "Pour un retour ou un remboursement, gardez votre preuve d'achat et contactez le support avec votre numéro de commande.";
        }
        if (normalized.contains("stock") || normalized.contains("disponible") || normalized.contains("disponibilité")) {
            return "Je peux vérifier les disponibilités des produits. Dites-moi le nom du produit qui vous intéresse.";
        }
        if (normalized.contains("prix") || normalized.contains("tarif")) {
            return "Dites-moi le produit qui vous intéresse et je vous aiderai à trouver son prix ou une alternative.";
        }
        return "Bonjour " + customerName + ", je suis le support boutique. Je peux aider pour les produits, les commandes, la livraison et les retours.";
    }

    private String extractText(String responseBody) {
        Matcher matcher = TEXT_PATTERN.matcher(responseBody == null ? "" : responseBody);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return "";
    }

    private String getApiKey() {
        return System.getenv("GEMINI_API_KEY");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}