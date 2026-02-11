package com.ecocoins;

import com.ecocoins.commands.ChangeMoneyCommand;
import com.ecocoins.core.CoinManager;
import com.ecocoins.core.ConfigBootstrap;
import com.ecocoins.core.InventoryUtil;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.TheEconomyService;
import com.ecocoins.model.CoinDefinition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class EcoCoins extends JavaPlugin {

    private ConfigBootstrap bootstrap;
    private LanguageManager languageManager;
    private CoinManager coinManager;
    private TheEconomyService economy;

    public EcoCoins(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[EcoCoins] setup()...");

        try {
            Path dataDir = getDataDirectory(); // .../plugins/EcoCoins/
            this.bootstrap = new ConfigBootstrap(getLogger(), dataDir);

            // Copia defaults del JAR a plugins/... (editable)
            this.bootstrap.ensureEditableExternalFolder();

            Path external = bootstrap.getExternalFolder();
            this.languageManager = new LanguageManager(getLogger(), external.resolve("Languages"));
            this.coinManager = new CoinManager(getLogger(), external.resolve("Coins"));

            // TheEconomy via reflection (no crashea si falta)
            this.economy = new TheEconomyService(getLogger());

            // ✅ Cargar aquí (no depender de start)
            languageManager.loadAll();
            coinManager.loadAll();

            // ✅ Registrar /change aquí (para que exista siempre)
            getCommandRegistry().registerCommand(
                    new ChangeMoneyCommand(languageManager, coinManager, economy)
            );

            getLogger().at(Level.INFO).log(
                    "[EcoCoins] setup OK. coins=" + coinManager.countCoins()
                            + " langs=" + languageManager.countLanguages()
                            + " theEconomy=" + economy.isAvailable()
                            + " (/change registrado)"
            );

        } catch (Throwable t) {
            getLogger().at(Level.SEVERE).log("[EcoCoins] CRASH en setup(): " + t);
            for (StackTraceElement e : t.getStackTrace()) {
                getLogger().at(Level.SEVERE).log("  at " + e);
            }
            throw t;
        }
    }

    @Override
    protected void start() {
        // Si el server no llama start() por alguna razón, /change igual existe (lo registramos en setup()).
        getLogger().at(Level.INFO).log("[EcoCoins] start()...");

        try {
            // =========================
            // LISTENER CLICK DERECHO
            // =========================
            getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                InteractionType t = event.getActionType();
                if (t != InteractionType.Secondary && t != InteractionType.Use) return;

                ItemStack hand = event.getItemInHand();
                if (hand == null || hand.isEmpty()) return;

                String itemId = hand.getItemId();
                Optional<CoinDefinition> coinOpt = coinManager.findByItemId(itemId);
                if (coinOpt.isEmpty()) return; // no es una moneda EcoCoins

                event.setCancelled(true);

                var player = event.getPlayer();

                if (!economy.isAvailable()) {
                    player.sendMessage(Message.raw("[EcoCoins] TheEconomy no está disponible."));
                    return;
                }

                CoinDefinition coin = coinOpt.get();
                if (coin.pay <= 0) return;

                UUID uuid = player.getPlayerRef().getUuid();

                // consumir 1 moneda física
                boolean removed = InventoryUtil.removeItemId(player.getInventory(), itemId, 1);
                if (!removed) {
                    player.sendMessage(Message.raw("[EcoCoins] No tienes suficientes monedas."));
                    return;
                }

                // depositar dinero virtual
                boolean deposited = economy.add(uuid, coin.pay);
                if (!deposited) {
                    // rollback best-effort
                    InventoryUtil.addItemId(player.getInventory(), itemId, 1);
                    player.sendMessage(Message.raw("[EcoCoins] No pude depositar dinero en TheEconomy."));
                    return;
                }

                player.sendMessage(Message.raw("[EcoCoins] +" + coin.pay));
            });

            getLogger().at(Level.INFO).log("[EcoCoins] start() OK.");

        } catch (Throwable t) {
            getLogger().at(Level.SEVERE).log("[EcoCoins] CRASH en start(): " + t);
            for (StackTraceElement e : t.getStackTrace()) {
                getLogger().at(Level.SEVERE).log("  at " + e);
            }
            throw t;
        }
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[EcoCoins] shutdown()");
    }
}
