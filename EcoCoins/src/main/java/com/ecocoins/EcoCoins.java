package com.ecocoins;

import com.ecocoins.commands.ChangeAllMoneyCommand;
import com.ecocoins.commands.ChangeHudOffCommand;
import com.ecocoins.commands.ChangeHudOnCommand;
import com.ecocoins.commands.ChangeMoneyCommand;
import com.ecocoins.core.CoinManager;
import com.ecocoins.core.CoinPickupSoundService;
import com.ecocoins.core.CoinRedeemService;
import com.ecocoins.core.ConfigBootstrap;
import com.ecocoins.core.LanguageManager;
import com.ecocoins.core.TheEconomyService;
import com.ecocoins.hud.BalanceHudService;
import com.ecocoins.interactions.CoinRedeemInteraction;
import com.ecocoins.util.HudHelper;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.logging.Level;

public final class EcoCoins extends JavaPlugin {

    private static EcoCoins instance;
    private static final InteractionType COIN_INTERACTION_TRIGGER = InteractionType.Secondary;
    private static final String COIN_CUSTOM_INTERACTION_ID = "EcoCoins_CoinRedeem";
    private static final boolean REDEEM_DEBUG_LOGS = false;

    private ConfigBootstrap bootstrap;
    private LanguageManager languageManager;
    private CoinManager coinManager;
    private TheEconomyService economy;
    private BalanceHudService balanceHudService;
    private CoinRedeemService coinRedeemService;
    private CoinPickupSoundService coinPickupSoundService;

    public EcoCoins(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[EcoCoins] setup()...");

        try {
            Path dataDir = getDataDirectory();
            this.bootstrap = new ConfigBootstrap(getLogger(), dataDir);
            this.bootstrap.ensureEditableExternalFolder();

            Path external = bootstrap.getExternalFolder();
            this.languageManager = new LanguageManager(getLogger(), external.resolve("Languages"));
            this.coinManager = new CoinManager(getLogger(), external.resolve("Coins"));

            this.economy = new TheEconomyService(getLogger());
            this.balanceHudService = new BalanceHudService(economy);
            this.coinRedeemService = new CoinRedeemService(getLogger(), coinManager, economy, languageManager, balanceHudService, REDEEM_DEBUG_LOGS);
            this.coinPickupSoundService = new CoinPickupSoundService(getLogger(), coinManager);

            HudHelper.init();
            registerCoinInteractionType();
            validateHudResources();

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

            getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, this::onLivingEntityInventoryChange);
            getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onAddPlayerToWorld);
            getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

            getCommandRegistry().registerCommand(new ChangeMoneyCommand(languageManager, coinManager, economy, balanceHudService));
            getCommandRegistry().registerCommand(new ChangeAllMoneyCommand(languageManager, coinManager, economy, balanceHudService));
            getCommandRegistry().registerCommand(new ChangeHudOffCommand(balanceHudService));
            getCommandRegistry().registerCommand(new ChangeHudOnCommand(balanceHudService));

            getLogger().at(Level.INFO).log(
                    "[EcoCoins] setup OK. coins=" + coinManager.countCoins()
                            + " langs=" + languageManager.countLanguages()
                            + " theEconomy=" + economy.isAvailable()
                            + " interactionTrigger=" + COIN_INTERACTION_TRIGGER
                            + " (comandos: /change /changeall /changeoff /changeon)"
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
        getLogger().at(Level.INFO).log("[EcoCoins] start()... interactionTrigger=" + COIN_INTERACTION_TRIGGER);
        getLogger().at(Level.INFO).log("[EcoCoins] start() OK.");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[EcoCoins] shutdown()");
    }

    private void validateHudResources() {
        ClassLoader cl = EcoCoins.class.getClassLoader();
        boolean commonDefault = cl.getResource("Common/UI/Custom/Pages/EcoCoins_BalanceHud.ui") != null;
        boolean uiDefault = cl.getResource("UI/Custom/Pages/EcoCoins_BalanceHud.ui") != null;

        if (commonDefault || uiDefault) {
            getLogger().at(Level.INFO).log("[EcoCoins] HUD UI detectada en classpath. common(default)="
                    + commonDefault + " ui(default)=" + uiDefault);
        } else {
            getLogger().at(Level.WARNING).log("[EcoCoins] HUD UI no detectada en classpath. Revisa assets.json y empaquetado de resources.");
        }
    }

    private void registerCoinInteractionType() {
        try {
            getCodecRegistry(Interaction.CODEC).register(
                    COIN_CUSTOM_INTERACTION_ID,
                    CoinRedeemInteraction.class,
                    CoinRedeemInteraction.CODEC
            );
            getLogger().at(Level.INFO).log("[EcoCoins] interaction type registrado: " + COIN_CUSTOM_INTERACTION_ID
                    + " -> " + CoinRedeemInteraction.class.getSimpleName());
        } catch (Throwable t) {
            getLogger().at(Level.WARNING).log("[EcoCoins] No se pudo registrar interaction type custom "
                    + COIN_CUSTOM_INTERACTION_ID + ". Continuo con trigger=" + COIN_INTERACTION_TRIGGER
                    + ". Detalle: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
        Player player = event.getHolder().getComponent(Player.getComponentType());
        PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
        if (player != null && playerRef != null && balanceHudService != null) {
            balanceHudService.showOnJoin(player, playerRef);
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (balanceHudService != null) {
            balanceHudService.onDisconnect(event.getPlayerRef());
        }
    }

    public static EcoCoins getInstance() {
        return instance;
    }

    public CoinRedeemService getCoinRedeemService() {
        return coinRedeemService;
    }

    private void onLivingEntityInventoryChange(LivingEntityInventoryChangeEvent event) {
        if (coinPickupSoundService != null) {
            coinPickupSoundService.onInventoryChanged(event);
        }
    }
}
