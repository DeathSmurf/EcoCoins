package com.ecocoins.commands;

import com.ecocoins.core.CoinManager;
import com.ecocoins.core.InventoryUtil;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.TheEconomyService;
import com.ecocoins.model.CoinDefinition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChangeAllMoneyCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGEALL_USE = "ecocoins.command.changeall.use";

    private final LanguageManager lang;
    private final CoinManager coins;
    private final TheEconomyService economy;

    public ChangeAllMoneyCommand(LanguageManager lang, CoinManager coins, TheEconomyService economy) {
        super("changeall", "Convierte todas tus monedas físicas EcoCoins a dinero virtual.", false);
        this.lang = lang;
        this.coins = coins;
        this.economy = economy;

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
            ctx.sendMessage(lang.trMsg(pLang, "command.changeall.no_coins", Map.of()));
            return;
        }

        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        boolean deposited = economy.add(uuid, username, totalPay);
        if (!deposited) {
            for (RemovedCoinBatch batch : removedBatches) {
                InventoryUtil.addItemId(player.getInventory(), batch.itemId(), batch.amount());
            }

            ctx.sendMessage(lang.trMsg(pLang, "command.changeall.deposit_failed", Map.of()));
            return;
        }

        ctx.sendMessage(lang.trMsg(pLang, "command.changeall.success", Map.of(
                "coins", totalCoins,
                "pay", totalPay
        )));
    }

    private record RemovedCoinBatch(String itemId, int amount) {
    }
}
