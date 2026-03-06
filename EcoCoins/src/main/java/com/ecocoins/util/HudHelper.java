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

    private static boolean multipleHudAvailable = false;
    private static Object multipleHudInstance = null;
    private static Method setCustomHudMethod = null;
    private static Method hideCustomHudMethod = null;

    private HudHelper() {
    }

    public static void init() {
        try {
            Class<?> multipleHudClass = Class.forName(MULTIPLEHUD_CLASS);
            Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
            multipleHudInstance = getInstanceMethod.invoke(null);

            if (multipleHudInstance != null) {
                setCustomHudMethod = multipleHudClass.getMethod(
                        "setCustomHud",
                        Player.class,
                        PlayerRef.class,
                        String.class,
                        CustomUIHud.class
                );

                hideCustomHudMethod = multipleHudClass.getMethod(
                        "hideCustomHud",
                        Player.class,
                        PlayerRef.class,
                        String.class
                );

                multipleHudAvailable = true;
                EcoCoins.getInstance().getLogger().at(Level.INFO).log(
                        "[EcoCoins] MultipleHUD detectado, modo compatible activo (HUD_ID=" + HUD_ID + ")."
                );
            }
        } catch (ClassNotFoundException e) {
            EcoCoins.getInstance().getLogger().at(Level.INFO).log(
                    "[EcoCoins] MultipleHUD no detectado, usando HUD vanilla."
            );
        } catch (Exception e) {
            EcoCoins.getInstance().getLogger().at(Level.WARNING).log(
                    "[EcoCoins] Fallo inicializando compatibilidad MultipleHUD: " + e.getMessage()
            );
        }
    }

    public static boolean isMultipleHudAvailable() {
        return multipleHudAvailable;
    }

    public static void setCustomHud(Player player, PlayerRef playerRef, CustomUIHud hud) {
        if (multipleHudAvailable && multipleHudInstance != null && setCustomHudMethod != null) {
            try {
                setCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_ID, hud);
                return;
            } catch (Exception e) {
                EcoCoins.getInstance().getLogger().at(Level.WARNING).log(
                        "[EcoCoins] MultipleHUD setCustomHud falló, usando HUD vanilla: " + e.getMessage()
                );
            }
        }

        player.getHudManager().setCustomHud(playerRef, hud);
    }

    public static void hideCustomHud(Player player, PlayerRef playerRef) {
        if (multipleHudAvailable && multipleHudInstance != null && hideCustomHudMethod != null) {
            try {
                hideCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_ID);
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
        }
    }
}
