package com.ecocoins.util;

import com.ecocoins.EcoCoins;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

public final class HudHelper {

    // Evitar caracteres raros por si la implementación de MultipleHUD valida IDs.
    private static final String HUD_ID = "ecocoins";
    private static final List<String> MULTIPLEHUD_CLASS_NAMES = List.of(
            "com.buuz135.mhud.MultipleHUD",
            "com.multiplehud.MultipleHUD",
            "dev.multiplehud.MultipleHUD",
            "MultipleHUD"
    );

    private static boolean multipleHudAvailable = false;
    private static Object multipleHudInstance = null;
    private static Method setCustomHudMethod = null;
    private static Method hideCustomHudMethod = null;

    private HudHelper() {
    }

    public static void init() {
        for (String className : MULTIPLEHUD_CLASS_NAMES) {
            try {
                Class<?> multipleHudClass = Class.forName(className);
                Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
                multipleHudInstance = getInstanceMethod.invoke(null);

                setCustomHudMethod = multipleHudClass.getMethod(
                        "setCustomHud",
                        Player.class,
                        PlayerRef.class,
                        String.class,
                        CustomUIHud.class
                );

                try {
                    hideCustomHudMethod = multipleHudClass.getMethod(
                            "hideCustomHud",
                            Player.class,
                            PlayerRef.class,
                            String.class
                    );
                } catch (NoSuchMethodException ignored) {
                    hideCustomHudMethod = null;
                }

                multipleHudAvailable = true;
                EcoCoins.getInstance().getLogger().at(Level.INFO).log(
                        "[EcoCoins] MultipleHUD detectado con clase " + className + ". HUD_ID=" + HUD_ID
                );
                return;
            } catch (ClassNotFoundException ignored) {
                // Intentamos siguiente nombre de clase.
            } catch (Throwable t) {
                EcoCoins.getInstance().getLogger().at(Level.WARNING).log(
                        "[EcoCoins] Error inicializando MultipleHUD: " + t.getClass().getSimpleName() + ": " + t.getMessage()
                );
                break;
            }
        }

        EcoCoins.getInstance().getLogger().at(Level.WARNING).log(
                "[EcoCoins] MultipleHUD no detectado: se usará HUD vanilla (1 HUD custom). " +
                        "Si otro mod ya usa CustomUI HUD, pueden aparecer conflictos de apply commands."
        );
    }

    public static boolean isMultipleHudAvailable() {
        return multipleHudAvailable;
    }

    public static void disableMultipleHudBridge(String reason) {
        if (!multipleHudAvailable) {
            return;
        }

        multipleHudAvailable = false;
        multipleHudInstance = null;
        setCustomHudMethod = null;
        hideCustomHudMethod = null;

        EcoCoins.getInstance().getLogger().at(Level.WARNING).log(
                "[EcoCoins] MultipleHUD bridge desactivado en runtime. Motivo: " + reason +
                        ". Se usará HUD vanilla para evitar bucle de errores CustomUI."
        );
    }

    public static void setCustomHud(Player player, PlayerRef playerRef, CustomUIHud hud) {
        if (multipleHudAvailable && multipleHudInstance != null && setCustomHudMethod != null) {
            try {
                setCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_ID, hud);
                return;
            } catch (Throwable t) {
                EcoCoins.getInstance().getLogger().at(Level.WARNING).log(
                        "[EcoCoins] Falló setCustomHud de MultipleHUD (" + t.getClass().getSimpleName() + "): " + t.getMessage()
                );
                // IMPORTANTE: no caer a vanilla si MultipleHUD está detectado pero falló,
                // porque puede causar exactamente el conflicto de apply commands
                // al competir con otros HUD custom ya envueltos por MultipleHUD.
                return;
            }
        }

        player.getHudManager().setCustomHud(playerRef, hud);
    }

    public static void hideCustomHud(Player player, PlayerRef playerRef) {
        if (multipleHudAvailable && multipleHudInstance != null && hideCustomHudMethod != null) {
            try {
                hideCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_ID);
            } catch (Throwable ignored) {
                // Best effort cleanup.
            }
        }
    }
}
