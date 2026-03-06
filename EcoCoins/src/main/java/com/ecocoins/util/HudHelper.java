package com.ecocoins.util;

import com.ecocoins.EcoCoins;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Helper para registrar HUD con compatibilidad opcional de MultipleHUD.
 * Implementación alineada al patrón funcional de referencia Ecotale.
 */
public final class HudHelper {

    private static final String HUD_ID = "ecocoins";
    private static final String MULTIPLEHUD_CLASS = "com.buuz135.mhud.MultipleHUD";

    private static volatile boolean multipleHudAvailable = false;
    private static volatile boolean initializationAttempted = false;
    private static volatile Object multipleHudInstance = null;
    private static volatile Method setCustomHudMethod = null;
    private static volatile Method hideCustomHudMethod = null;

    private HudHelper() {
    }

    public static void init() {
        tryEnableMultipleHud(false);
    }

    public static boolean isMultipleHudAvailable() {
        return tryEnableMultipleHud(true);
    }

    public static void setCustomHud(Player player, PlayerRef playerRef, CustomUIHud hud) {
        if (tryEnableMultipleHud(true) && multipleHudInstance != null && setCustomHudMethod != null) {
            try {
                setCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_ID, hud);
                return;
            } catch (Exception e) {
                disableMultipleHud("[EcoCoins] MultipleHUD setCustomHud falló, usando HUD vanilla: " + e.getMessage());
            }
        }

        player.getHudManager().setCustomHud(playerRef, hud);
    }

    public static void hideCustomHud(Player player, PlayerRef playerRef) {
        if (tryEnableMultipleHud(true) && multipleHudInstance != null && hideCustomHudMethod != null) {
            try {
                hideCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_ID);
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
        }
    }

    private static synchronized boolean tryEnableMultipleHud(boolean lazyRetry) {
        if (multipleHudAvailable && multipleHudInstance != null && setCustomHudMethod != null) {
            return true;
        }

        if (initializationAttempted && !lazyRetry) {
            return false;
        }

        initializationAttempted = true;

        try {
            Class<?> multipleHudClass = Class.forName(MULTIPLEHUD_CLASS);
            Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);

            if (instance == null) {
                multipleHudAvailable = false;
                if (lazyRetry) {
                    EcoCoins.getInstance().getLogger().at(Level.FINE).log(
                            "[EcoCoins] MultipleHUD presente, pero instancia aún no lista. Reintentando luego."
                    );
                } else {
                    EcoCoins.getInstance().getLogger().at(Level.INFO).log(
                            "[EcoCoins] MultipleHUD detectado pero getInstance()=null en setup. Se activará cuando esté listo."
                    );
                }
                return false;
            }

            Method setMethod = multipleHudClass.getMethod(
                    "setCustomHud",
                    Player.class,
                    PlayerRef.class,
                    String.class,
                    CustomUIHud.class
            );

            Method hideMethod = multipleHudClass.getMethod(
                    "hideCustomHud",
                    Player.class,
                    PlayerRef.class,
                    String.class
            );

            multipleHudInstance = instance;
            setCustomHudMethod = setMethod;
            hideCustomHudMethod = hideMethod;
            multipleHudAvailable = true;

            EcoCoins.getInstance().getLogger().at(Level.INFO).log(
                    "[EcoCoins] MultipleHUD detectado y activo (HUD_ID=" + HUD_ID + ")."
            );

            return true;

        } catch (ClassNotFoundException e) {
            if (!lazyRetry) {
                EcoCoins.getInstance().getLogger().at(Level.INFO).log(
                        "[EcoCoins] MultipleHUD no detectado, usando HUD vanilla."
                );
            }
            multipleHudAvailable = false;
            return false;
        } catch (Exception e) {
            disableMultipleHud("[EcoCoins] Fallo inicializando compatibilidad MultipleHUD: " + e.getMessage());
            return false;
        }
    }

    private static void disableMultipleHud(String message) {
        multipleHudAvailable = false;
        multipleHudInstance = null;
        setCustomHudMethod = null;
        hideCustomHudMethod = null;
        EcoCoins.getInstance().getLogger().at(Level.WARNING).log(message);
    }
}
