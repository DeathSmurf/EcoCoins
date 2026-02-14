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
    private Method addBalanceByName;
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
        return add(playerId, null, amount);
    }

    public boolean add(UUID playerId, String username, double amount) {
        if (!available) return false;

        // addBalance(UUID, amount) en TheEconomy es void: si no lanza excepción,
        // tratamos el depósito como exitoso para evitar falsos negativos por lectura
        // de balance desfasada en el mismo tick.
        try {
            double before = getBalance(playerId);
            addBalance.invoke(apiInstance, playerId, amount);
            return true;
        } catch (Exception uuidError) {
            // Fallback opcional por nombre de usuario (algunas instalaciones lo resuelven mejor).
            if (username != null && !username.isBlank() && addBalanceByName != null) {
                try {
                    Object r = addBalanceByName.invoke(apiInstance, username, amount);
                    return !(r instanceof Boolean b) || b;
                } catch (Exception nameError) {
                    logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy addBalance error UUID="
                            + uuidError.getMessage() + " | username=" + nameError.getMessage());
                    return false;
                }
            }

            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy addBalance error: " + uuidError.getMessage());
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
            addBalanceByName = apiClass.getMethod("addBalance", String.class, double.class);
            removeBalance = apiClass.getMethod("removeBalance", UUID.class, double.class);

            available = apiInstance != null;
        } catch (Throwable t) {
            available = false;
        }

        if (available) logger.at(java.util.logging.Level.INFO).log("[EcoCoins] TheEconomy detectado: integración activa.");
        else logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] TheEconomy NO detectado: /change money no podrá depositar dinero virtual.");
    }
}
