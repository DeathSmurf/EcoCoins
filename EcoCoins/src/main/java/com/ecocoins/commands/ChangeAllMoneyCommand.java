package com.ecocoins.commands;

import com.ecocoins.core.CoinManager;
import com.ecocoins.core.CommandTimeoutService;
import com.ecocoins.core.InventoryUtil;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.TheEconomyService;
import com.ecocoins.hud.BalanceHudService;
import com.ecocoins.model.CoinDefinition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChangeAllMoneyCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGEALL_USE = "ecocoins.command.changeall.use";
    private static final String REDEEM_SOUND_EVENT_ID = "SFX_EcoCoins_Redeem";

    private final LanguageManager lang;
    private final CoinManager coins;
    private final TheEconomyService economy;
    private final BalanceHudService hudService;
    private final CommandTimeoutService timeoutService;

    public ChangeAllMoneyCommand(LanguageManager lang, CoinManager coins, TheEconomyService economy, BalanceHudService hudService, CommandTimeoutService timeoutService) {
        super("changeall", "Convierte todas tus monedas físicas EcoCoins a dinero virtual.", false);
        this.lang = lang;
        this.coins = coins;
        this.economy = economy;
        this.hudService = hudService;
        this.timeoutService = timeoutService;

        this.requirePermission(PERM_CHANGEALL_USE);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {

        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("[EcoCoins] No pude resolver el Player entity."));
            return;
        }

        String pLang = lang.resolveLang(playerRef.getLanguage());

        if (!economy.isAvailable()) {
            ctx.sendMessage(lang.trMsg(pLang, "command.changeall.economy_unavailable", Map.of()));
            return;
        }

        timeoutService.executeWithProfile(
                player,
                store,
                playerEntityRef,
                playerRef,
                CommandTimeoutService.TimeoutProfile.CHANGE_ALL,
                () -> executeNow(store, playerEntityRef, playerRef)
        );
    }

    private void executeNow(Store<EntityStore> store,
                            Ref<EntityStore> playerEntityRef,
                            PlayerRef playerRef) {
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        String pLang = lang.resolveLang(playerRef.getLanguage());

        if (!economy.isAvailable()) {
            player.sendMessage(lang.trMsg(pLang, "command.changeall.economy_unavailable", Map.of()));
            return;
        }

        List<RemovedCoinBatch> removedBatches = new ArrayList<>();

        double totalPay = 0.0;
        int totalCoins = 0;

        for (CoinDefinition coin : coins.getCoinsSnapshot()) {
            if (coin == null || coin.name_item == null || coin.name_item.isBlank() || coin.pay <= 0) {
                continue;
            }

            int amount = InventoryUtil.countItemId(player.getInventory(), coin.name_item);
            if (amount <= 0) {
                continue;
            }

            boolean removed = InventoryUtil.removeItemId(player.getInventory(), coin.name_item, amount);
            if (!removed) {
                continue;
            }

            removedBatches.add(new RemovedCoinBatch(coin.name_item, amount));
            totalCoins += amount;
            totalPay += coin.pay * (double) amount;
        }

        if (totalCoins <= 0 || totalPay <= 0.0) {
            player.sendMessage(lang.trMsg(pLang, "command.changeall.no_coins", Map.of()));
            return;
        }

        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        boolean deposited = economy.add(uuid, username, totalPay);
        if (!deposited) {
            for (RemovedCoinBatch batch : removedBatches) {
                InventoryUtil.addItemId(player.getInventory(), batch.itemId(), batch.amount());
            }

            player.sendMessage(lang.trMsg(pLang, "command.changeall.deposit_failed", Map.of()));
            return;
        }

        playRedeemSound(player);

        player.sendMessage(lang.trMsg(pLang, "command.changeall.success", Map.of(
                "coins", totalCoins,
                "pay", totalPay
        )));

        hudService.updateBalance(player, playerRef);
    }


    private void playRedeemSound(Player player) {
        try {
            SoundEvent event = SoundEvent.getAssetMap().getAsset(REDEEM_SOUND_EVENT_ID);
            if (event == null) {
                return;
            }

            int soundEventIndex = SoundEvent.getAssetMap().getIndex(REDEEM_SOUND_EVENT_ID);
            if (soundEventIndex < 0) {
                return;
            }

            SoundUtil.playSoundEvent2dToPlayer(player.getPlayerRef(), soundEventIndex, SoundCategory.SFX);
        } catch (Throwable ignored) {
            // El sonido es best-effort: no debe romper /changeall.
        }
    }

    private record RemovedCoinBatch(String itemId, int amount) {
    }
}
