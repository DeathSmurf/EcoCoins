package com.ecocoins;

import com.ecocoins.commands.ChangeMoneyCommand;
import com.ecocoins.core.CoinManager;
import com.ecocoins.core.ConfigBootstrap;
import com.ecocoins.core.CoinRedeemService;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.TheEconomyService;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.logging.Level;

public final class EcoCoins extends JavaPlugin {

    private static final InteractionType COIN_INTERACTION_TRIGGER = InteractionType.Secondary;
    private static final String COIN_CUSTOM_INTERACTION_ID = "EcoCoins_CoinRedeem";
    private static final boolean REDEEM_DEBUG_LOGS = true;

    private ConfigBootstrap bootstrap;
    private LanguageManager languageManager;
    private CoinManager coinManager;
    private TheEconomyService economy;
    private CoinRedeemService coinRedeemService;

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
            this.coinRedeemService = new CoinRedeemService(getLogger(), coinManager, economy, REDEEM_DEBUG_LOGS);

            registerCoinInteractionType();

            // Carga tolerante: si hay JSON roto, no bloquear el registro de /change.
            try {
                languageManager.loadAll();
            } catch (Throwable t) {
                getLogger().at(Level.WARNING).log("[EcoCoins] Error cargando Languages. Continúo para no perder /change: " + t);
            }

            try {
                coinManager.loadAll();
            } catch (Throwable t) {
                getLogger().at(Level.WARNING).log("[EcoCoins] Error cargando Coins. Continúo para no perder /change: " + t);
            }

            // Registrar /change aunque falle la carga de configuración.
            getCommandRegistry().registerCommand(
                    new ChangeMoneyCommand(languageManager, coinManager, economy)
            );

            getLogger().at(Level.INFO).log(
                    "[EcoCoins] setup OK. coins=" + coinManager.countCoins()
                            + " langs=" + languageManager.countLanguages()
                            + " theEconomy=" + economy.isAvailable()
                            + " interactionTrigger=" + COIN_INTERACTION_TRIGGER
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
        getLogger().at(Level.INFO).log("[EcoCoins] start()... interactionTrigger=" + COIN_INTERACTION_TRIGGER);

        try {
            // =========================
            // LISTENER CLICK DERECHO
            // =========================
            getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                InteractionType action = event.getActionType();
                if (!isCoinRedeemInteraction(action)) return;

                ItemStack hand = event.getItemInHand();
                var player = event.getPlayer();
                boolean processed = coinRedeemService.redeemFromHandIfEcoCoin(player, action, hand);
                if (processed) {
                    event.setCancelled(true);
                }
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

    private static boolean isCoinRedeemInteraction(InteractionType type) {
        // Modo ideal y estricto: solo Secondary.
        return type == COIN_INTERACTION_TRIGGER;
    }
}
