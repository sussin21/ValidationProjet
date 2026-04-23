package com.example.app.services;

import com.example.app.dao.ShopAutomationEventDAO;
import com.example.app.entities.Produit;
import com.example.app.entities.ShopAutomationEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShopAutomationEventService {

    private final ShopAutomationEventDAO eventDAO = new ShopAutomationEventDAO();
    private final ShopAnalyticsService analyticsService = new ShopAnalyticsService();
    private final StockPredictionService stockPredictionService = new StockPredictionService();

    public List<ShopAutomationEvent> select() throws SQLException {
        return eventDAO.select();
    }

    public List<ShopAutomationEvent> findLatest(int limit) throws SQLException {
        return eventDAO.findLatest(limit);
    }

    public void createEvent(String eventType, String description, String status) throws SQLException {
        ShopAutomationEvent event = new ShopAutomationEvent();
        event.setEventType(eventType);
        event.setDescription(description);
        event.setStatus(status);
        eventDAO.add(event);
    }

    public List<ShopAutomationEvent> generateStockAlerts(int threshold) throws SQLException {
        List<ShopAutomationEvent> createdEvents = new ArrayList<>();
        List<Produit> lowStockProducts = analyticsService.getLowStockProducts(threshold);

        for (Produit product : lowStockProducts) {
            String description = "Produit critique: " + product.getNom() + " (stock actuel: " + product.getQuantiteDisponible() + ")";
            createEvent("STOCK_ALERT", description, "ACTIVE");

            ShopAutomationEvent event = new ShopAutomationEvent();
            event.setEventType("STOCK_ALERT");
            event.setDescription(description);
            event.setStatus("ACTIVE");
            createdEvents.add(event);
        }

        List<com.example.app.entities.StockPrediction> criticalPredictions = stockPredictionService.findCriticalProducts(20);
        for (com.example.app.entities.StockPrediction prediction : criticalPredictions) {
            String description = "Réapprovisionnement recommandé pour " + prediction.getProductName()
                    + " (stock actuel: " + prediction.getCurrentStock()
                    + ", stock recommandé: " + prediction.getRecommendedStock() + ")";
            createEvent("RESTOCK", description, "ACTIVE");

            ShopAutomationEvent event = new ShopAutomationEvent();
            event.setEventType("RESTOCK");
            event.setDescription(description);
            event.setStatus("ACTIVE");
            createdEvents.add(event);
        }

        return createdEvents;
    }
}