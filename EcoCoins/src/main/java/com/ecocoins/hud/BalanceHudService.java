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
    private final ConcurrentHashMap<UUID, HudSettings> settings = new ConcurrentHashMap<>();

    public BalanceHudService(TheEconomyService economyService) {
        this.economyService = economyService;
    }

    public void showOnJoin(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) {
            return;
        }

        HudSettings cfg = settings.computeIfAbsent(playerRef.getUuid(), ignored -> HudSettings.defaults());
        if (!cfg.enabled()) {
            return;
        }

        EcoCoinBalanceHud hud = huds.computeIfAbsent(playerRef.getUuid(), ignored -> new EcoCoinBalanceHud(playerRef));
        HudHelper.setCustomHud(player, playerRef, hud);
        hud.update(economyService.getBalance(playerRef.getUuid()), cfg.position());
    }

    public void hide(Player player, PlayerRef playerRef) {
        HudSettings cfg = settings.computeIfAbsent(playerRef.getUuid(), ignored -> HudSettings.defaults());
        settings.put(playerRef.getUuid(), cfg.withEnabled(false));
        HudHelper.hideCustomHud(player, playerRef);
    }

    public void show(Player player, PlayerRef playerRef) {
        HudSettings cfg = settings.computeIfAbsent(playerRef.getUuid(), ignored -> HudSettings.defaults());
        settings.put(playerRef.getUuid(), cfg.withEnabled(true));
        showOnJoin(player, playerRef);
    }

    public EcoCoinBalanceHud.Position togglePosition(Player player, PlayerRef playerRef) {
        HudSettings cfg = settings.computeIfAbsent(playerRef.getUuid(), ignored -> HudSettings.defaults());
        EcoCoinBalanceHud.Position newPosition = cfg.position().toggle();
        settings.put(playerRef.getUuid(), cfg.withPosition(newPosition));

        if (cfg.enabled()) {
            showOnJoin(player, playerRef);
        }

        return newPosition;
    }

    public void updateBalance(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) {
            return;
        }

        HudSettings cfg = settings.computeIfAbsent(playerRef.getUuid(), ignored -> HudSettings.defaults());
        if (!cfg.enabled()) {
            return;
        }

        EcoCoinBalanceHud hud = huds.computeIfAbsent(playerRef.getUuid(), ignored -> new EcoCoinBalanceHud(playerRef));
        hud.update(economyService.getBalance(playerRef.getUuid()), cfg.position());
        HudHelper.setCustomHud(player, playerRef, hud);
    }

    public void onDisconnect(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        huds.remove(playerRef.getUuid());
        settings.remove(playerRef.getUuid());
    }

    public void sendPositionMessage(Player player, EcoCoinBalanceHud.Position position) {
        if (position == EcoCoinBalanceHud.Position.BOTTOM_LEFT) {
            player.sendMessage(Message.raw("[EcoCoins] HUD movido a esquina inferior izquierda."));
        } else {
            player.sendMessage(Message.raw("[EcoCoins] HUD movido a esquina inferior derecha."));
        }
    }

    private record HudSettings(boolean enabled, EcoCoinBalanceHud.Position position) {
        static HudSettings defaults() {
            return new HudSettings(true, EcoCoinBalanceHud.Position.BOTTOM_RIGHT);
        }

        HudSettings withEnabled(boolean value) {
            return new HudSettings(value, this.position);
        }

        HudSettings withPosition(EcoCoinBalanceHud.Position value) {
            return new HudSettings(this.enabled, value);
        }
    }
}
