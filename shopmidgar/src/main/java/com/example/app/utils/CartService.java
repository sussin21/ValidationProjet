package com.example.app.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CartService {
    private static final CartService INSTANCE = new CartService();
    private final Map<Integer, Integer> cart = new LinkedHashMap<>();

    public static CartService getInstance() {
        return INSTANCE;
    }

    private CartService() {}

    public synchronized void addItem(int productId, int quantity) {
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
    }

    public synchronized void removeItem(int productId) {
        cart.remove(productId);
    }

    public synchronized void updateQuantity(int productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
        } else {
            cart.put(productId, quantity);
        }
    }

    public synchronized Map<Integer, Integer> getCart() {
        return new LinkedHashMap<>(cart);
    }

    public synchronized void clearCart() { cart.clear(); }

    public synchronized boolean isEmpty() { return cart.isEmpty(); }

    public synchronized int getCartCount() {
        return cart.values().stream().mapToInt(Integer::intValue).sum();
    }
}