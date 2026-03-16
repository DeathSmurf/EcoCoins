package com.ecocoins.core;

import com.ecocoins.model.CoinDefinition;
import com.ecocoins.hud.BalanceHudService;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class CoinRedeemService {

    public static final String PERM_REDEEM_USE = "ecocoins.redeem.use";
    private static final String REDEEM_SOUND_EVENT_ID = "SFX_EcoCoins_Redeem";

    private final HytaleLogger logger;
    private final CoinManager coinManager;
    private final TheEconomyService economy;
    private final LanguageManager languageManager;
    private final BalanceHudService hudService;
    private final SuccessMessageAggregationService successAggregationService;
    private final boolean debugLogs;

    public CoinRedeemService(HytaleLogger logger, CoinManager coinManager, TheEconomyService economy, LanguageManager languageManager, BalanceHudService hudService, SuccessMessageAggregationService successAggregationService, boolean debugLogs) {
        this.logger = logger;
        this.coinManager = coinManager;
        this.economy = economy;
        this.languageManager = languageManager;
        this.hudService = hudService;
        this.successAggregationService = successAggregationService;
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

        if (!player.hasPermission(PERM_REDEEM_USE)) {
            player.sendMessage(tr(player, "common.no_permission"));
            return true;
        }

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
            player.sendMessage(tr(player, "redeem.economy_unavailable"));
            return true;
        }

        CoinDefinition coin = coinOpt.get();
        if (coin.pay <= 0) {
            debug("fallo: coin.pay <= 0 para itemId=" + itemId + " pay=" + coin.pay);
            player.sendMessage(tr(player, "redeem.invalid_pay"));
            return true;
        }

        UUID uuid = player.getPlayerRef().getUuid();

        // consumir 1 moneda física
        boolean removed = InventoryUtil.removeItemId(player.getInventory(), itemId, 1);
        if (!removed) {
            debug("fallo: no se pudo remover x1 itemId=" + itemId + " para uuid=" + uuid);
            player.sendMessage(tr(player, "redeem.no_coin"));
            return true;
        }

        // depositar dinero virtual
        String username = player.getPlayerRef().getUsername();
        boolean deposited = economy.add(uuid, username, coin.pay);
        if (!deposited) {
            // rollback best-effort
            InventoryUtil.addItemId(player.getInventory(), itemId, 1);
            debug("fallo: depósito virtual falló. rollback x1 itemId=" + itemId + " uuid=" + uuid + " username=" + username + " pay=" + coin.pay);
            player.sendMessage(tr(player, "redeem.deposit_failed"));
            return true;
        }

        playRedeemSound(player);
        debug("ok: canjeado itemId=" + itemId + " pay=" + coin.pay + " uuid=" + uuid + " (source=" + source + ")");
        successAggregationService.onRedeemSuccess(player, player.getPlayerRef(), coin.pay);
        hudService.updateBalance(player, player.getPlayerRef());
        return true;
    }

    private void playRedeemSound(Player player) {
        try {
            SoundEvent event = SoundEvent.getAssetMap().getAsset(REDEEM_SOUND_EVENT_ID);
            if (event == null) {
                debug("aviso: SoundEvent no encontrado: " + REDEEM_SOUND_EVENT_ID);
                return;
            }

            int soundEventIndex = SoundEvent.getAssetMap().getIndex(REDEEM_SOUND_EVENT_ID);
            if (soundEventIndex < 0) {
                debug("aviso: índice inválido para SoundEvent: " + REDEEM_SOUND_EVENT_ID + " idx=" + soundEventIndex);
                return;
            }

            SoundUtil.playSoundEvent2dToPlayer(player.getPlayerRef(), soundEventIndex, SoundCategory.SFX);
        } catch (Throwable t) {
            debug("aviso: no se pudo reproducir sound redeem: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static boolean isRedeemTrigger(InteractionType actionType) {
        return actionType == InteractionType.Secondary;
    }

    private Message tr(Player player, String key) {
        String lang = languageManager.resolveLang(player.getPlayerRef().getLanguage());
        return languageManager.trMsg(lang, key, java.util.Map.of());
    }

    private Message tr(Player player, String key, java.util.Map<String, Object> vars) {
        String lang = languageManager.resolveLang(player.getPlayerRef().getLanguage());
        return languageManager.trMsg(lang, key, vars);
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
