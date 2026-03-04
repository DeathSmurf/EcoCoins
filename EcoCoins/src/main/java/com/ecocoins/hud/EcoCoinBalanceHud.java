package com.ecocoins.hud;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class EcoCoinBalanceHud extends SimpleHud {

    public enum Position {
        BOTTOM_RIGHT;

        public Position toggle() {
            return BOTTOM_RIGHT;
        }
    }

    public EcoCoinBalanceHud(PlayerRef playerRef) {
        super(playerRef, "Pages/EcoCoins_BalanceHud.ui");
    }

    /**
     * Solo actualiza estado de texto; no empuja comandos al cliente.
     */
    public void applyBalanceState(double balance) {
        String value = String.format("%.2f", balance);
        setText("CurrencyName", "EcoCoin");
        setText("BalanceSymbol", "$");
        setText("BalanceAmount", value);
    }

    /**
     * Render completo del HUD (vanilla path).
     */
    public void render() {
        pushUpdates();
    }
}
