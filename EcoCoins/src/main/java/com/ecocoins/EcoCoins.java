package com.ecocoins;

import com.ecocoins.commands.ChangeMoneyCommand;
import com.ecocoins.core.CoinManager;
import com.ecocoins.core.ConfigBootstrap;
import com.ecocoins.core.CoinRedeemService;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.TheEconomyService;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
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
    private static final boolean INPUT_DEBUG_LOGS = true;
    private static final boolean RAW_INPUT_DEBUG_LOGS = true;

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
            getLogger().at(Level.INFO).log("[EcoCoins] registrando listener PlayerInteractEvent");
            getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                InteractionType action = event.getActionType();
                String itemId = resolveItemId(event.getItemInHand());
                debugInput("PlayerInteractEvent action=" + action + " itemId=" + itemId);
                if (!isCoinRedeemInteraction(action)) return;

                ItemStack hand = event.getItemInHand();
                var player = event.getPlayer();
                boolean processed = coinRedeemService.redeemFromHandIfEcoCoin(player, action, hand);
                if (processed) {
                    event.setCancelled(true);
                } else {
                    debugInput("PlayerInteractEvent sin canje action=" + action + " itemId=" + itemId);
                }
            });

            // Fallback: algunos items custom no disparan PlayerInteractEvent al click derecho.
            // En ese caso usamos el evento de mouse para diagnosticar y canjear.
            getLogger().at(Level.INFO).log("[EcoCoins] registrando listener PlayerMouseButtonEvent");
            getEventRegistry().registerGlobal(PlayerMouseButtonEvent.class, event -> {
                var mouse = event.getMouseButton();
                var item = event.getItemInHand();
                String itemId = (item != null) ? item.getId() : null;
                debugRawInput("PlayerMouseButtonEvent button="
                        + (mouse != null ? mouse.mouseButtonType : null)
                        + " state="
                        + (mouse != null ? mouse.state : null)
                        + " clicks="
                        + (mouse != null ? mouse.clicks : null)
                        + " itemId=" + itemId);

                if (mouse == null) return;
                if (mouse.mouseButtonType != MouseButtonType.Right) return;
                if (mouse.state != MouseButtonState.Pressed) return;

                var player = event.getPlayer();
                debugInput("PlayerMouseButtonEvent right pressed itemId=" + itemId);
                boolean processed = coinRedeemService.redeemByMouseRightIfEcoCoin(player, itemId);
                if (processed) {
                    event.setCancelled(true);
                } else {
                    debugInput("PlayerMouseButtonEvent sin canje itemId=" + itemId);
                }
            });

            getLogger().at(Level.INFO).log("[EcoCoins] start() OK.");
            getLogger().at(Level.INFO).log("[EcoCoins] EcoCoin:cargado correctamente");

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


    private void registerCoinInteractionType() {
        try {
            getCodecRegistry(Interaction.CODEC).register(
                    COIN_CUSTOM_INTERACTION_ID,
                    SimpleInteraction.class,
                    SimpleInteraction.CODEC
            );
            getLogger().at(Level.INFO).log("[EcoCoins] interaction type registrado: " + COIN_CUSTOM_INTERACTION_ID
                    + " -> " + SimpleInteraction.class.getSimpleName());
        } catch (Throwable t) {
            getLogger().at(Level.WARNING).log("[EcoCoins] No se pudo registrar interaction type custom "
                    + COIN_CUSTOM_INTERACTION_ID + ". Continuo con trigger=" + COIN_INTERACTION_TRIGGER
                    + ". Detalle: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private void debugInput(String message) {
        if (!INPUT_DEBUG_LOGS) return;
        getLogger().at(Level.INFO).log("[EcoCoins][Input] " + message);
    }

    private void debugRawInput(String message) {
        if (!RAW_INPUT_DEBUG_LOGS) return;
        getLogger().at(Level.INFO).log("[EcoCoins][InputRaw] " + message);
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

    private static boolean isCoinRedeemInteraction(InteractionType type) {
        // Modo ideal y estricto: solo Secondary.
        return type == COIN_INTERACTION_TRIGGER || type == InteractionType.Use;
    }
}
