package com.ecocoins.core;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class CoinPickupSoundService {

    private static final String COIN_PICKUP_SOUND_EVENT_ID = "SE_Coin_Pickup";

    private final HytaleLogger logger;
    private final CoinManager coinManager;

    public CoinPickupSoundService(HytaleLogger logger, CoinManager coinManager) {
        this.logger = logger;
        this.coinManager = coinManager;
    }

    public void onInventoryChanged(LivingEntityInventoryChangeEvent event) {
        if (event == null || !event.getTransaction().succeeded()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!containsNetCoinGain(event.getTransaction())) {
            return;
        }

        playCoinPickupSound(player);
    }

    private boolean containsNetCoinGain(Transaction transaction) {
        Map<String, Integer> netCoinDeltaByItem = new HashMap<>();
        collectCoinDelta(transaction, netCoinDeltaByItem);

        for (int delta : netCoinDeltaByItem.values()) {
            if (delta > 0) {
                return true;
            }
        }
        return false;
    }

    private void collectCoinDelta(Transaction transaction, Map<String, Integer> netCoinDeltaByItem) {
        if (transaction == null) {
            return;
        }

        if (transaction instanceof SlotTransaction slotTransaction) {
            addStackDelta(netCoinDeltaByItem, slotTransaction.getSlotBefore(), -1);
            addStackDelta(netCoinDeltaByItem, slotTransaction.getSlotAfter(), +1);
            return;
        }

        if (transaction instanceof ItemStackTransaction itemStackTransaction) {
            for (SlotTransaction slotTransaction : itemStackTransaction.getSlotTransactions()) {
                collectCoinDelta(slotTransaction, netCoinDeltaByItem);
            }
            return;
        }

        if (transaction instanceof MoveTransaction<?> moveTransaction) {
            collectCoinDelta(moveTransaction.getRemoveTransaction(), netCoinDeltaByItem);
            collectCoinDelta(moveTransaction.getAddTransaction(), netCoinDeltaByItem);
            return;
        }

        if (transaction instanceof ListTransaction<?> listTransaction) {
            for (Transaction nested : listTransaction.getList()) {
                collectCoinDelta(nested, netCoinDeltaByItem);
            }
        }
    }

    private void addStackDelta(Map<String, Integer> netCoinDeltaByItem, ItemStack stack, int sign) {
        if (stack == null || stack.isEmpty() || sign == 0) {
            return;
        }

        String itemId = resolveItemId(stack);
        if (!coinManager.isEcoCoinItemId(itemId)) {
            return;
        }

        String key = normalizeItemId(itemId);
        netCoinDeltaByItem.merge(key, sign * stack.getQuantity(), Integer::sum);
    }

    private void playCoinPickupSound(Player player) {
        try {
            SoundEvent soundEvent = SoundEvent.getAssetMap().getAsset(COIN_PICKUP_SOUND_EVENT_ID);
            if (soundEvent == null) {
                logger.at(Level.WARNING).log("[EcoCoins] SoundEvent no encontrado: " + COIN_PICKUP_SOUND_EVENT_ID);
                return;
            }

            int soundEventIndex = SoundEvent.getAssetMap().getIndex(COIN_PICKUP_SOUND_EVENT_ID);
            if (soundEventIndex < 0) {
                logger.at(Level.WARNING).log("[EcoCoins] Índice inválido para SoundEvent: " + COIN_PICKUP_SOUND_EVENT_ID + " idx=" + soundEventIndex);
                return;
            }

            SoundUtil.playSoundEvent2dToPlayer(player.getPlayerRef(), soundEventIndex, SoundCategory.SFX);
        } catch (Throwable t) {
            logger.at(Level.WARNING).log("[EcoCoins] No se pudo reproducir sonido de pickup de moneda: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static String resolveItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        String direct = stack.getItemId();
        if (direct != null && !direct.isBlank()) {
            return direct;
        }

        if (stack.getItem() != null && stack.getItem().getId() != null && !stack.getItem().getId().isBlank()) {
            return stack.getItem().getId();
        }

        return null;
    }

    private static String normalizeItemId(String itemId) {
        if (itemId == null) {
            return "";
        }

        int namespaceSeparator = itemId.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < itemId.length()) {
            return itemId.substring(namespaceSeparator + 1).trim();
        }

        return itemId.trim();
    }
}
