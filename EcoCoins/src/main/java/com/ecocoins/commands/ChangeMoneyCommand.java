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
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * /change <money_name>
 * /change <money_name> <amount>
 *
 * Importante:
 * - En esta API, DefaultArg NO funciona como argumento posicional.
 * - Para soportar "amount" como segundo token posicional, hay que usar addUsageVariant()
 *   con un comando variante creado usando el constructor "solo descripción" (sin nombre),
 *   o el servidor lanza: "Cannot add a variant command with a name".
 */
public final class ChangeMoneyCommand extends AbstractPlayerCommand {

    private final LanguageManager lang;
    private final CoinManager coins;
    private final TheEconomyService economy;

    // Variante 1 (tu modo actual): /change <money_name>  (amount = 1)
    private final RequiredArg<String> moneyNameArg;

    // (Puedes mantener este DefaultArg si lo usas en help/auto-doc, pero NO será posicional)
    @SuppressWarnings("unused")
    private final DefaultArg<Integer> amountDefaultArg;

    public ChangeMoneyCommand(LanguageManager lang, CoinManager coins, TheEconomyService economy) {
        super("change", "Convierte dinero virtual (TheEconomy) en monedas físicas (EcoCoins).");

        this.lang = lang;
        this.coins = coins;
        this.economy = economy;

        this.moneyNameArg = withRequiredArg("money_name", "Nombre de moneda (primary o alias).", ArgTypes.STRING);

        // Esto NO hace que /change gold 3 funcione, pero ayuda en documentación.
        this.amountDefaultArg = withDefaultArg("amount", "Cantidad a comprar.", ArgTypes.INTEGER, 1, "1");

        // Variante 2 real: /change <money_name> <amount>  (posicional)
        addUsageVariant(new ChangeMoneyAmountVariant());
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {
        String moneyName = ctx.get(moneyNameArg);
        executePurchase(ctx, store, playerEntityRef, playerRef, moneyName, 1);
    }

    private void executePurchase(CommandContext ctx,
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

        // Resolver idioma UNA sola vez y usarlo en todo el flujo
        String pLang = lang.resolveLang(playerRef.getLanguage());

        // ✅ FIX: si amount <= 0, NO hacer nada y avisar (multilenguaje)
        if (amount <= 0) {
            ctx.sendMessage(lang.trMsg(pLang, "command.change.invalid_amount", Map.of(
                    "amount", amount
            )));
            return;
        }

        Optional<CoinDefinition> coinOpt = coins.findByMoneyName(moneyName);

        if (coinOpt.isEmpty()) {
            ctx.sendMessage(lang.trMsg(pLang, "command.change.not_found", Map.of("moneyName", moneyName)));
            return;
        }

        CoinDefinition coin = coinOpt.get();
        if (coin.name_item == null || coin.name_item.isBlank()) {
            ctx.sendMessage(Message.raw("[EcoCoins] Coin inválida: falta name_item en JSON."));
            return;
        }

        double cost = coin.pay * (double) amount;

        if (!economy.isAvailable()) {
            ctx.sendMessage(Message.raw("[EcoCoins] TheEconomy no está disponible; no puedo retirar balance."));
            return;
        }

        if (cost <= 0.0) {
            ctx.sendMessage(Message.raw("[EcoCoins] Coin inválida: pay debe ser > 0 en el JSON."));
            return;
        }

        UUID uuid = playerRef.getUuid();

        // 1) Verificar inventario antes de cobrar
        if (!InventoryUtil.canAddItemId(player.getInventory(), coin.name_item, amount)) {
            ctx.sendMessage(lang.trMsg(pLang, "command.change.inventory_full", Map.of()));
            return;
        }

        // 2) Verificar balance
        if (!economy.has(uuid, cost)) {
            ctx.sendMessage(lang.trMsg(pLang, "command.change.not_enough_money", Map.of(
                    "amount", amount,
                    "cost", cost
            )));
            return;
        }

        // 3) Retirar
        boolean withdrawn = economy.remove(uuid, cost);
        if (!withdrawn) {
            ctx.sendMessage(Message.raw("[EcoCoins] No pude retirar dinero de TheEconomy."));
            return;
        }

        // 4) Dar monedas físicas
        boolean added = InventoryUtil.addItemId(player.getInventory(), coin.name_item, amount);
        if (!added) {
            // rollback best-effort
            economy.add(uuid, cost);
            ctx.sendMessage(lang.trMsg(pLang, "command.change.inventory_full", Map.of()));
            return;
        }

        ctx.sendMessage(lang.trMsg(pLang, "command.change.success", Map.of(
                "amount", amount,
                "moneyName", coin.money_name != null ? coin.money_name.primary : moneyName,
                "cost", cost
        )));
    }

    /**
     * Variante posicional: /change <money_name> <amount>
     *
     * IMPORTANTE: Para addUsageVariant, la variante debe crearse con el constructor
     * AbstractPlayerCommand(String description) (sin nombre), o el server se cae.
     */
    private final class ChangeMoneyAmountVariant extends AbstractPlayerCommand {

        private final RequiredArg<String> moneyNameArg2;
        private final RequiredArg<Integer> amountArg2;

        private ChangeMoneyAmountVariant() {
            super("Compra una cantidad específica: /change <money_name> <amount>");

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

            ChangeMoneyCommand.this.executePurchase(ctx, store, playerEntityRef, playerRef, moneyName, amount);
        }
    }
}