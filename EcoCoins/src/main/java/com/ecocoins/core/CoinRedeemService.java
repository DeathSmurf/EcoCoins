package com.ecocoins.core;

import com.ecocoins.model.CoinDefinition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class CoinRedeemService {

    private final HytaleLogger logger;
    private final CoinManager coinManager;
    private final TheEconomyService economy;
    private final boolean debugLogs;

    public CoinRedeemService(HytaleLogger logger, CoinManager coinManager, TheEconomyService economy, boolean debugLogs) {
        this.logger = logger;
        this.coinManager = coinManager;
        this.economy = economy;
        this.debugLogs = debugLogs;
    }

    /**
     * Retorna true solo cuando el evento corresponde a una moneda EcoCoins
     * y se intentó procesar (éxito o fallo).
     */
    public boolean redeemFromHandIfEcoCoin(Player player, InteractionType actionType, ItemStack itemInHand) {
        if (!isRedeemTrigger(actionType)) return false;
        if (player == null || itemInHand == null || itemInHand.isEmpty()) {
            debug("ignorado: mano vacía para trigger=" + actionType);
            return false;
        }

        String itemId = resolveItemId(itemInHand);
        return redeemByItemId(player, itemId, "PlayerInteractEvent:" + actionType);
    }

    public boolean redeemByMouseRightIfEcoCoin(Player player, String itemId) {
        return redeemByItemId(player, itemId, "PlayerMouseButtonEvent:RightPressed");
    }

    private boolean redeemByItemId(Player player, String itemId, String source) {
        if (player == null) return false;

        if (itemId == null || itemId.isBlank()) {
            debug("ignorado: itemId vacío (source=" + source + ")");
            return false;
        }

        Optional<CoinDefinition> coinOpt = coinManager.findByItemId(itemId);
        if (coinOpt.isEmpty()) {
            debug("ignorado: itemId no mapeado en Coins JSON -> " + itemId + " (source=" + source + ")");
            return false;
        }

        if (!economy.isAvailable()) {
            debug("fallo: TheEconomy no disponible para itemId=" + itemId);
            player.sendMessage(Message.raw("[EcoCoins] TheEconomy no está disponible."));
            return true;
        }

        CoinDefinition coin = coinOpt.get();
        if (coin.pay <= 0) {
            debug("fallo: coin.pay <= 0 para itemId=" + itemId + " pay=" + coin.pay);
            player.sendMessage(Message.raw("[EcoCoins] Coin inválida: pay debe ser > 0 en JSON."));
            return true;
        }

        UUID uuid = player.getPlayerRef().getUuid();

        // consumir 1 moneda física
        boolean removed = InventoryUtil.removeItemId(player.getInventory(), itemId, 1);
        if (!removed) {
            debug("fallo: no se pudo remover x1 itemId=" + itemId + " para uuid=" + uuid);
            player.sendMessage(Message.raw("[EcoCoins] No tienes suficientes monedas."));
            return true;
        }

        // depositar dinero virtual
        String username = player.getPlayerRef().getUsername();
        boolean deposited = economy.add(uuid, username, coin.pay);
        if (!deposited) {
            // rollback best-effort
            InventoryUtil.addItemId(player.getInventory(), itemId, 1);
            debug("fallo: depósito virtual falló. rollback x1 itemId=" + itemId + " uuid=" + uuid + " username=" + username + " pay=" + coin.pay);
            player.sendMessage(Message.raw("[EcoCoins] No pude depositar dinero en TheEconomy."));
            return true;
        }

        debug("ok: canjeado itemId=" + itemId + " pay=" + coin.pay + " uuid=" + uuid + " (source=" + source + ")");
        player.sendMessage(Message.raw("[EcoCoins] +" + coin.pay));
        return true;
    }

    private static boolean isRedeemTrigger(InteractionType actionType) {
        return actionType == InteractionType.Secondary || actionType == InteractionType.Use;
    }

    private void debug(String message) {
        if (!debugLogs) return;
        logger.at(Level.INFO).log("[EcoCoins][Redeem] " + message);
    }

    private static String resolveItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String direct = stack.getItemId();
        if (direct != null && !direct.isBlank()) return direct;

        if (stack.getItem() != null && stack.getItem().getId() != null && !stack.getItem().getId().isBlank()) {
            return stack.getItem().getId();
        }

        return null;
    }
}
