package com.ecocoins.hud;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class EcoCoinBalanceHud extends SimpleHud {

    public enum Position {
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public Position toggle() {
            return this == BOTTOM_RIGHT ? BOTTOM_LEFT : BOTTOM_RIGHT;
        }
    }

    private final Position position;

    public EcoCoinBalanceHud(PlayerRef playerRef, Position position) {
        super(playerRef, uiPathFor(position));
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void update(double balance) {
        String value = String.format("%.2f", balance);
        setText("BalanceAmount", value);

        // Estrategia Ecotale-like: full show() en cada actualización de HUD
        // (sin update incremental), para evitar conflictos de aplicación de comandos.
        pushUpdates();
    }

    private static String uiPathFor(Position position) {
        return position == Position.BOTTOM_LEFT
                ? "Pages/EcoCoins_BalanceHud_Left.ui"
                : "Pages/EcoCoins_BalanceHud_Right.ui";
    }
}
