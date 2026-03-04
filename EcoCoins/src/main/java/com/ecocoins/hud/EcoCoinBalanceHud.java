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

    public EcoCoinBalanceHud(PlayerRef playerRef) {
        super(playerRef, "Pages/EcoCoins_BalanceHud.ui");
    }

    public void update(double balance, Position position) {
        String value = String.format("%.2f", balance);

        setText("RightBalanceAmount", value);
        setText("LeftBalanceAmount", value);

        boolean right = position == Position.BOTTOM_RIGHT;
        setVisible("RightBalancePanel", right);
        setVisible("LeftBalancePanel", !right);
        pushUpdates();
    }
}
