package com.ecocoins.hud;

import com.ecocoins.util.HudHelper;
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
    private boolean mounted = false;

    public EcoCoinBalanceHud(PlayerRef playerRef, Position position) {
        super(playerRef, uiPathFor(position));
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void update(double balance) {
        String value = String.format("%.2f", balance);

        // En MultipleHUD evitamos deltas incrementales, porque algunos wrappers
        // internos aplican selectores propios (ej. #MultipleHUD) y pueden entrar
        // en conflicto con update(false, ...).
        if (HudHelper.isMultipleHudAvailable()) {
            setText("BalanceAmount", value);
            mounted = pushUpdates();
            return;
        }

        // 1) Primer montaje: show() completo del documento.
        if (!mounted) {
            setText("BalanceAmount", value);
            mounted = pushUpdates();
            return;
        }

        // 2) En vanilla, usar delta incremental para reducir churn.
        UICommandBuilder builder = createBuilder();
        builder.set("#BalanceAmount.Text", value);

        boolean ok = sendUpdate(builder);
        if (!ok) {
            // Si el delta falla, reintentar con montaje completo en el siguiente update.
            mounted = false;
        }
    }

    private static String uiPathFor(Position position) {
        return position == Position.BOTTOM_LEFT
                ? "Pages/EcoCoins_BalanceHud_Left.ui"
                : "Pages/EcoCoins_BalanceHud_Right.ui";
    }
}
