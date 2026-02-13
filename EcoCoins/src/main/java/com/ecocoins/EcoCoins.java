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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class EcoCoins extends JavaPlugin {

    private static final InteractionType COIN_INTERACTION_TRIGGER = InteractionType.Secondary;
    private static final String COIN_CUSTOM_INTERACTION_ID = "EcoCoins_CoinRedeem";

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
                InteractionType t = event.getActionType();
                if (!isCoinRedeemInteraction(t)) return;

                ItemStack hand = event.getItemInHand();
                if (hand == null || hand.isEmpty()) return;

                String itemId = resolveItemId(hand);
                if (itemId == null || itemId.isBlank()) return;
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

    private static boolean isCoinRedeemInteraction(InteractionType type) {
        // Este servidor procesa monedas físicas únicamente mediante Secondary,
        // porque los items registrados en el mod.zip usan ese trigger.
        // Cualquier otro tipo (Use, Primary, Ability, etc.) se ignora.
        return type == COIN_INTERACTION_TRIGGER;
    }

    private void registerCoinInteractionType() {
        try {
            Class<?> interactionClass = Class.forName("com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction");
            Field interactionCodecField = interactionClass.getField("CODEC");
            Object interactionCodec = interactionCodecField.get(null);

            Method getCodecRegistryMethod = null;
            for (Method m : this.getClass().getMethods()) {
                if (m.getName().equals("getCodecRegistry") && m.getParameterCount() == 1) {
                    getCodecRegistryMethod = m;
                    break;
                }
            }

            if (getCodecRegistryMethod == null) {
                getLogger().at(Level.WARNING).log("[EcoCoins] No pude ubicar getCodecRegistry para registrar interaction type.");
                return;
            }

            Object codecRegistry = getCodecRegistryMethod.invoke(this, interactionCodec);

            Class<?> simpleInteractionClass = Class.forName("com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.SimpleInteraction");
            Field simpleCodecField = simpleInteractionClass.getField("CODEC");
            Object simpleCodec = simpleCodecField.get(null);

            Method registerMethod = null;
            for (Method m : codecRegistry.getClass().getMethods()) {
                if (m.getName().equals("register") && m.getParameterCount() == 3) {
                    registerMethod = m;
                    break;
                }
            }

            if (registerMethod == null) {
                getLogger().at(Level.WARNING).log("[EcoCoins] No pude ubicar register(name,class,codec) para interaction type.");
                return;
            }

            registerMethod.invoke(codecRegistry, COIN_CUSTOM_INTERACTION_ID, simpleInteractionClass, simpleCodec);
            getLogger().at(Level.INFO).log("[EcoCoins] interaction type registrado: " + COIN_CUSTOM_INTERACTION_ID + " -> SimpleInteraction");
        } catch (Throwable t) {
            getLogger().at(Level.WARNING).log("[EcoCoins] No se pudo registrar interaction type custom. Continuo con trigger="
                    + COIN_INTERACTION_TRIGGER + ". Detalle: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
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
