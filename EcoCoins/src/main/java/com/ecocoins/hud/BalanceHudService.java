package com.ecocoins.hud;

import com.ecocoins.core.TheEconomyService;
import com.ecocoins.util.HudHelper;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BalanceHudService {

    private final TheEconomyService economyService;
    private final ConcurrentHashMap<UUID, EcoCoinBalanceHud> huds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> enabled = new ConcurrentHashMap<>();

    public BalanceHudService(TheEconomyService economyService) {
        this.economyService = economyService;
    }

    public void showOnJoin(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) {
            return;
        }

        boolean hudEnabled = enabled.computeIfAbsent(playerRef.getUuid(), ignored -> true);
        if (!hudEnabled) {
            return;
        }

        EcoCoinBalanceHud hud = huds.computeIfAbsent(playerRef.getUuid(), ignored -> new EcoCoinBalanceHud(playerRef));
        hud.applyBalanceState(economyService.getBalance(playerRef.getUuid()));

        // Con MultipleHUD, setCustomHud ya fuerza el render dentro del contenedor compuesto.
        // Llamar hud.render() después puede causar builds extra no prefijados.
        HudHelper.setCustomHud(player, playerRef, hud);
        if (!HudHelper.isMultipleHudAvailable()) {
            hud.render();
        }
    }

    public void hide(Player player, PlayerRef playerRef) {
        enabled.put(playerRef.getUuid(), false);
        HudHelper.hideCustomHud(player, playerRef);
    }

    public void show(Player player, PlayerRef playerRef) {
        enabled.put(playerRef.getUuid(), true);
        showOnJoin(player, playerRef);
    }

    public EcoCoinBalanceHud.Position togglePosition(Player player, PlayerRef playerRef) {
        player.sendMessage(Message.raw("[EcoCoins] /changeposition está desactivado por estabilidad. HUD fijo en esquina inferior derecha (estilo Ecotale)."));
        return EcoCoinBalanceHud.Position.BOTTOM_RIGHT;
    }

    public void updateBalance(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) {
            return;
        }

        boolean hudEnabled = enabled.computeIfAbsent(playerRef.getUuid(), ignored -> true);
        if (!hudEnabled) {
            return;
        }

        EcoCoinBalanceHud hud = huds.computeIfAbsent(playerRef.getUuid(), ignored -> new EcoCoinBalanceHud(playerRef));
        boolean changed = hud.applyBalanceState(economyService.getBalance(playerRef.getUuid()));
        if (changed) {
            if (HudHelper.isMultipleHudAvailable()) {
                HudHelper.setCustomHud(player, playerRef, hud);
            } else {
                hud.render();
            }
        }
    }

    public void onDisconnect(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        huds.remove(playerRef.getUuid());
        enabled.remove(playerRef.getUuid());
    }

    public void sendPositionMessage(Player player, EcoCoinBalanceHud.Position position) {
        player.sendMessage(Message.raw("[EcoCoins] HUD fijo en esquina inferior derecha (estructura Ecotale estable)."));
    }
}
