package com.ecocoins.hud;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class EcoCoinBalanceHud extends SimpleHud {

    private static final String RIGHT_ANCHOR = "(Right: 0, Bottom: 152, Width: 190, Height: 28)";
    private static final String LEFT_ANCHOR = "(Left: 0, Bottom: 152, Width: 190, Height: 28)";

    public enum Position {
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public Position toggle() {
            return this == BOTTOM_RIGHT ? BOTTOM_LEFT : BOTTOM_RIGHT;
        }
    }

    public EcoCoinBalanceHud(PlayerRef playerRef) {
        super(playerRef, "Pages/EcoCoins_BalanceHud.ui");
    }

    public void update(double balance, Position position) {
        String value = String.format("%.2f", balance);
        setText("BalanceAmount", value);

        String anchor = (position == Position.BOTTOM_RIGHT) ? RIGHT_ANCHOR : LEFT_ANCHOR;
        setProperty("BalancePanel", "Anchor", anchor);
        pushUpdates();
    }
}
