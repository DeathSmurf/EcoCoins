package com.ecocoins.core;

import com.ecocoins.model.CoinDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class CoinManager {
    private final HytaleLogger logger;
    private final Path coinsDir;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<CoinDefinition> coins = new ArrayList<>();
    private final Set<String> coinItemIds = new HashSet<>();

    public CoinManager(HytaleLogger logger, Path coinsDir) {
        this.logger = logger;
        this.coinsDir = coinsDir;
    }

    public void loadAll() {
        coins.clear();
        coinItemIds.clear();
        if (!Files.isDirectory(coinsDir)) {
            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] Coins dir no existe: " + coinsDir);
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(coinsDir, "*.json")) {
            for (Path p : ds) {
                CoinDefinition def = mapper.readValue(p.toFile(), CoinDefinition.class);
                if (def == null || def.money_name == null || def.money_name.primary == null) {
                    logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] Coin inválida (falta money_name.primary): " + p.getFileName());
                    continue;
                }
                coins.add(def);
                if (def.name_item != null && !def.name_item.isBlank()) {
                    coinItemIds.add(def.name_item.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("EcoCoins: error leyendo Coins/*.json", e);
        }
    }

    public int countCoins() { return coins.size(); }

    public Optional<CoinDefinition> findByMoneyName(String name) {
        if (name == null) return Optional.empty();
        String n = name.trim().toLowerCase(Locale.ROOT);

        for (CoinDefinition c : coins) {
            if (c.money_name == null) continue;
            if (c.money_name.primary != null && n.equals(c.money_name.primary.toLowerCase(Locale.ROOT))) return Optional.of(c);
            if (c.money_name.aliases != null) {
                for (String a : c.money_name.aliases) {
                    if (a != null && n.equals(a.toLowerCase(Locale.ROOT))) return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<CoinDefinition> findByItemId(String itemId) {
        if (itemId == null) return Optional.empty();

        String id = itemId.trim();
        String normalizedId = normalizeItemId(id);

        for (CoinDefinition c : coins) {
            if (c.name_item == null) continue;

            String configured = c.name_item.trim();
            if (configured.isEmpty()) continue;

            if (id.equals(configured)) return Optional.of(c);
            if (normalizedId.equals(normalizeItemId(configured))) return Optional.of(c);
        }

        return Optional.empty();
    }

    public boolean isEcoCoinItemId(String itemId) {
        if (itemId == null) return false;

        String id = itemId.trim();
        if (coinItemIds.contains(id)) return true;

        return coinItemIds.contains(normalizeItemId(id));
    }

    private static String normalizeItemId(String itemId) {
        int namespaceSeparator = itemId.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < itemId.length()) {
            return itemId.substring(namespaceSeparator + 1).trim();
        }
        return itemId;
    }
}
