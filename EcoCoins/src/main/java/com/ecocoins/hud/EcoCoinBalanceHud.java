package com.ecocoins.hud;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Locale;

public final class EcoCoinBalanceHud extends SimpleHud {

    private static final String CURRENCY_NAME = "EcoCoin";
    private static final String CURRENCY_SYMBOL = "$";

    private double displayedBalance = Double.NaN;

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
     * Actualiza el estado del balance con formato corto estilo Ecotale.
     * Retorna true cuando hay cambios visibles para evitar render redundante.
     */
    public boolean applyBalanceState(double balance) {
        if (!Double.isNaN(displayedBalance) && Math.abs(balance - displayedBalance) < 0.005d) {
            return false;
        }

        displayedBalance = balance;

        setText("CurrencyName", CURRENCY_NAME);
        setText("BalanceSymbol", CURRENCY_SYMBOL);
        setText("BalanceAmount", formatShort(balance));
        return true;
    }

    /**
     * Render completo del HUD.
     */
    public void render() {
        pushUpdates();
    }

    private String formatShort(double value) {
        double abs = Math.abs(value);

        if (abs >= 1_000_000_000d) {
            return String.format(Locale.US, "%.2fB", value / 1_000_000_000d);
        }
        if (abs >= 1_000_000d) {
            return String.format(Locale.US, "%.2fM", value / 1_000_000d);
        }
        if (abs >= 10_000d) {
            return String.format(Locale.US, "%.2fK", value / 1_000d);
        }

        return String.format(Locale.US, "%.2f", value);
    }
}
