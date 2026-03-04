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
            setText("BalanceAmount", value);
            initialized = pushUpdates();
            return;
        }

        UICommandBuilder builder = createBuilder();
        builder.set("#BalanceAmount.Text", value);

        boolean updated = sendUpdate(builder);
        if (!updated) {
            // Si el delta falla, forzar rebuild completo en siguiente update.
            initialized = false;
        }
    }

    private static String uiPathFor(Position position) {
        return position == Position.BOTTOM_LEFT
                ? "Pages/EcoCoins_BalanceHud_Left.ui"
                : "Pages/EcoCoins_BalanceHud_Right.ui";
    }
}
