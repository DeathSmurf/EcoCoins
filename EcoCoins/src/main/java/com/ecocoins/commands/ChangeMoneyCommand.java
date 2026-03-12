package com.ecocoins.commands;

import com.ecocoins.core.CoinManager;
import com.ecocoins.core.InventoryUtil;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.CommandTimeoutService;
import com.ecocoins.core.TheEconomyService;
import com.ecocoins.hud.BalanceHudService;
import com.ecocoins.model.CoinDefinition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ChangeMoneyCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGE_USE = "ecocoins.command.change.use";
    public static final String PERM_CHANGE_LIST = "ecocoins.command.change.list";

    private final LanguageManager lang;
    private final CoinManager coins;
    private final TheEconomyService economy;
    private final BalanceHudService hudService;
    private final CommandTimeoutService timeoutService;

    private final RequiredArg<String> moneyNameArg;
    @SuppressWarnings("unused")
    private final DefaultArg<Integer> amountDefaultArg;

    public ChangeMoneyCommand(LanguageManager lang, CoinManager coins, TheEconomyService economy, BalanceHudService hudService, CommandTimeoutService timeoutService) {
        super("change", "Convierte dinero virtual (TheEconomy) en monedas físicas (EcoCoins).", false);

        this.lang = lang;
        this.coins = coins;
        this.economy = economy;
        this.hudService = hudService;
        this.timeoutService = timeoutService;

        this.requirePermission(PERM_CHANGE_USE);

        this.moneyNameArg = withRequiredArg("money_name", "Nombre de moneda (primary o alias).", ArgTypes.STRING);
        this.amountDefaultArg = withDefaultArg("amount", "Cantidad a comprar.", ArgTypes.INTEGER, 1, "1");

        addUsageVariant(new ChangeMoneyAmountVariant());
        addUsageVariant(new ChangeMoneyListVariant());
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {
        String moneyName = ctx.get(moneyNameArg);

        if (moneyName != null && moneyName.equalsIgnoreCase("list")) {
            executeList(ctx, store, playerEntityRef, playerRef);
            return;
        }

        executePurchaseWithTimeout(ctx, store, playerEntityRef, playerRef, moneyName, 1);
    }

    private void executePurchaseWithTimeout(CommandContext ctx,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> playerEntityRef,
                                 PlayerRef playerRef,
                                 String moneyName,
                                 int amount) {

        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("[EcoCoins] No pude resolver el Player entity."));
            return;
        }

        String pLang = lang.resolveLang(playerRef.getLanguage());

        if (amount <= 0) {
            ctx.sendMessage(lang.trMsg(pLang, "command.change.invalid_amount", Map.of("amount", amount)));
            return;
        }

        timeoutService.executeWithProfile(
                player,
                store,
                playerEntityRef,
                playerRef,
                CommandTimeoutService.TimeoutProfile.CHANGE,
                () -> executePurchaseNow(store, playerEntityRef, playerRef, moneyName, amount)
        );
    }

    private void executePurchaseNow(Store<EntityStore> store,
                                    Ref<EntityStore> playerEntityRef,
                                    PlayerRef playerRef,
                                    String moneyName,
                                    int amount) {
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        String pLang = lang.resolveLang(playerRef.getLanguage());

        Optional<CoinDefinition> coinOpt = coins.findByMoneyName(moneyName);
        if (coinOpt.isEmpty()) {
            player.sendMessage(lang.trMsg(pLang, "command.change.not_found", Map.of("moneyName", moneyName)));
            return;
        }

        CoinDefinition coin = coinOpt.get();
        if (coin.name_item == null || coin.name_item.isBlank()) {
            player.sendMessage(Message.raw("[EcoCoins] Coin inválida: falta name_item en JSON."));
            return;
        }

        double cost = coin.pay * (double) amount;

        if (!economy.isAvailable()) {
            player.sendMessage(lang.trMsg(pLang, "command.change.economy_unavailable", Map.of()));
            return;
        }

        if (cost <= 0.0) {
            player.sendMessage(lang.trMsg(pLang, "command.change.invalid_pay", Map.of()));
            return;
        }

        UUID uuid = playerRef.getUuid();

        if (!InventoryUtil.canAddItemId(player.getInventory(), coin.name_item, amount)) {
            player.sendMessage(lang.trMsg(pLang, "command.change.inventory_full", Map.of()));
            return;
        }

        if (!economy.has(uuid, cost)) {
            player.sendMessage(lang.trMsg(pLang, "command.change.not_enough_money", Map.of("amount", amount, "cost", cost)));
            return;
        }

        boolean withdrawn = economy.remove(uuid, cost);
        if (!withdrawn) {
            player.sendMessage(lang.trMsg(pLang, "command.change.withdraw_failed", Map.of()));
            return;
        }

        boolean added = InventoryUtil.addItemId(player.getInventory(), coin.name_item, amount);
        if (!added) {
            economy.add(uuid, cost);
            player.sendMessage(lang.trMsg(pLang, "command.change.inventory_full", Map.of()));
            return;
        }

        player.sendMessage(lang.trMsg(pLang, "command.change.success", Map.of(
                "amount", amount,
                "moneyName", coin.money_name != null ? coin.money_name.primary : moneyName,
                "cost", cost
        )));

        hudService.updateBalance(player, playerRef);
    }

    private void executeList(CommandContext ctx,
                             Store<EntityStore> store,
                             Ref<EntityStore> playerEntityRef,
                             PlayerRef playerRef) {
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("[EcoCoins] No pude resolver el Player entity."));
            return;
        }

        if (!player.hasPermission(PERM_CHANGE_LIST)) {
            String pLang = lang.resolveLang(playerRef.getLanguage());
            ctx.sendMessage(lang.trMsg(pLang, "common.no_permission", Map.of()));
            return;
        }

        String pLang = lang.resolveLang(playerRef.getLanguage());
        ctx.sendMessage(lang.trMsg(pLang, "command.change.list.usage", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.change.list.header", Map.of()));

        List<CoinDefinition> defs = new ArrayList<>(coins.getCoinsSnapshot());
        defs.sort(Comparator.comparingDouble(c -> c.pay));

        for (CoinDefinition c : defs) {
            String itemId = (c.name_item == null || c.name_item.isBlank()) ? "?" : c.name_item;
            String primary = (c.money_name != null && c.money_name.primary != null) ? c.money_name.primary : "?";
            String aliasJoined = joinAliases(c.money_name != null ? c.money_name.aliases : null, pLang);

            Message linePrefix = lang.trMsg(pLang, "command.change.list.entry", Map.of(
                    "pay", c.pay,
                    "primary", primary,
                    "aliases", aliasJoined
            ));

            Message itemName = Message.translation("server.items." + itemId + ".name");
            ctx.sendMessage(Message.join(linePrefix, Message.raw(" "), itemName));
        }
    }

    private String joinAliases(List<String> aliases, String pLang) {
        if (aliases == null || aliases.isEmpty()) {
            return lang.tr(pLang, "command.change.list.no_aliases", Map.of());
        }
        if (aliases.size() == 1) return aliases.get(0);

        String orWord = lang.tr(pLang, "common.or", Map.of());
        if (aliases.size() == 2) {
            return aliases.get(0) + " " + orWord + " " + aliases.get(1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aliases.size(); i++) {
            String a = aliases.get(i);
            if (i == 0) sb.append(a);
            else if (i == aliases.size() - 1) sb.append(" ").append(orWord).append(" ").append(a);
            else sb.append(", ").append(a);
        }
        return sb.toString();
    }

    private final class ChangeMoneyAmountVariant extends AbstractPlayerCommand {
        private final RequiredArg<String> moneyNameArg2;
        private final RequiredArg<Integer> amountArg2;

        private ChangeMoneyAmountVariant() {
            super("Compra una cantidad específica: /change <money_name> <amount>");
            this.requirePermission(PERM_CHANGE_USE);
            this.moneyNameArg2 = withRequiredArg("money_name", "Nombre de moneda (primary o alias).", ArgTypes.STRING);
            this.amountArg2 = withRequiredArg("amount", "Cantidad a comprar.", ArgTypes.INTEGER);
        }

        @Override
        protected void execute(CommandContext ctx,
                               Store<EntityStore> store,
                               Ref<EntityStore> playerEntityRef,
                               PlayerRef playerRef,
                               World world) {
            String moneyName = ctx.get(moneyNameArg2);
            int amount = ctx.get(amountArg2);

            if (moneyName != null && moneyName.equalsIgnoreCase("list")) {
                executeList(ctx, store, playerEntityRef, playerRef);
                return;
            }

            ChangeMoneyCommand.this.executePurchaseWithTimeout(ctx, store, playerEntityRef, playerRef, moneyName, amount);
        }
    }

    private final class ChangeMoneyListVariant extends AbstractPlayerCommand {
        private ChangeMoneyListVariant() {
            super("Muestra la lista de monedas: /change");
            this.requirePermission(PERM_CHANGE_LIST);
        }

        @Override
        protected void execute(CommandContext ctx,
                               Store<EntityStore> store,
                               Ref<EntityStore> playerEntityRef,
                               PlayerRef playerRef,
                               World world) {
            executeList(ctx, store, playerEntityRef, playerRef);
        }
    }
}
