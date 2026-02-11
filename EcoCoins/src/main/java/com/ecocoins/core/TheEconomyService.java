package com.ecocoins.core;

import com.hypixel.hytale.logger.HytaleLogger;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Conexión opcional a TheEconomy vía reflection (para que EcoCoins no crashee si no está).
 * La página del mod documenta com.economy.api.EconomyAPI con getInstance() y métodos de balance. 
 */
public final class TheEconomyService {
    private final HytaleLogger logger;

    private boolean available = false;
    private Object apiInstance;
    private Method addBalance;
    private Method getBalance;
    private Method hasBalance;
    private Method removeBalance;

    public TheEconomyService(HytaleLogger logger) {
        this.logger = logger;
        tryInit();
    }

    public boolean isAvailable() {
        return available;
    }

    public double getBalance(UUID playerId) {
        if (!available) return 0.0;
        try {
            Object r = getBalance.invoke(apiInstance, playerId);
            return (r instanceof Number n) ? n.doubleValue() : 0.0;
        } catch (Exception e) {
            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy getBalance error: " + e.getMessage());
            return 0.0;
        }
    }

    public boolean has(UUID playerId, double amount) {
        if (!available) return false;
        try {
            Object r = hasBalance.invoke(apiInstance, playerId, amount);
            return (r instanceof Boolean b) && b;
        } catch (Exception e) {
            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy hasBalance error: " + e.getMessage());
            return false;
        }
    }

    public boolean add(UUID playerId, double amount) {
        if (!available) return false;
        try {
            addBalance.invoke(apiInstance, playerId, amount);
            return true;
        } catch (Exception e) {
            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy addBalance error: " + e.getMessage());
            return false;
        }
    }

    public boolean remove(UUID playerId, double amount) {
        if (!available) return false;
        try {
            Object r = removeBalance.invoke(apiInstance, playerId, amount);
            // doc indica boolean, pero lo toleramos
            return !(r instanceof Boolean b) || b;
        } catch (Exception e) {
            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy removeBalance error: " + e.getMessage());
            return false;
        }
    }

    private void tryInit() {
        try {
            Class<?> apiClass = Class.forName("com.economy.api.EconomyAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            apiInstance = getInstance.invoke(null);

            getBalance = apiClass.getMethod("getBalance", UUID.class);
            hasBalance = apiClass.getMethod("hasBalance", UUID.class, double.class);
            addBalance = apiClass.getMethod("addBalance", UUID.class, double.class);
            removeBalance = apiClass.getMethod("removeBalance", UUID.class, double.class);

            available = apiInstance != null;
        } catch (Throwable t) {
            available = false;
        }

        if (available) logger.at(java.util.logging.Level.INFO).log("[EcoCoins] TheEconomy detectado: integración activa.");
        else logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy NO detectado: /change money no podrá depositar dinero virtual.");
    }
}
