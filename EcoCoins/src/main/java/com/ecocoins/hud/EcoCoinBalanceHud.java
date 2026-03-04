package com.ecocoins.hud;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
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
    private boolean initialized = false;

    public EcoCoinBalanceHud(PlayerRef playerRef, Position position) {
        super(playerRef, uiPathFor(position));
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void update(double balance) {
        String value = String.format("%.2f", balance);

        if (!initialized) {
            // Primer render: append del documento + estado inicial
            setText("BalanceAmount", value);
            pushUpdates();
            initialized = true;
            return;
        }

        // Actualizaciones siguientes: enviar sólo delta de texto para reducir
        // conflictos de comandos HUD en runtime.
        UICommandBuilder builder = createBuilder();
        builder.set("#BalanceAmount.Text", value);
        sendUpdate(builder);
    }

    private static String uiPathFor(Position position) {
        return position == Position.BOTTOM_LEFT
                ? "Pages/EcoCoins_BalanceHud_Left.ui"
                : "Pages/EcoCoins_BalanceHud_Right.ui";
    }
}
