package com.ecocoins.core;

import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Agrupa mensajes de éxito por jugador/clave con ventana deslizante de 2 segundos.
 *
 * Comportamiento:
 * - Primer éxito de la ráfaga se envía inmediato.
 * - Éxitos adicionales dentro de 2s se acumulan y NO se imprimen al instante.
 * - Si pasan 2s sin nuevos éxitos, se envía una sola línea con lo acumulado.
 */
public final class SuccessMessageBurstService {

    private static final long BURST_WINDOW_MS = 2_000L;

    private final LanguageManager languageManager;
    private final ScheduledExecutorService scheduler;
    private final Map<BucketKey, BucketState> buckets = new ConcurrentHashMap<>();

    public SuccessMessageBurstService(LanguageManager languageManager) {
        this.languageManager = languageManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ecocoins-success-burst");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void sendSuccess(Player player, String langKey, int coins, double pay) {
        if (player == null) return;

        UUID uuid = player.getPlayerRef().getUuid();
        BucketKey key = new BucketKey(uuid, langKey);

        BucketState state;
        synchronized (buckets) {
            state = buckets.get(key);
            if (state == null) {
                sendTranslated(player, langKey, coins, pay);
                BucketState created = new BucketState(player);
                created.future = scheduleFlush(key, created);
                buckets.put(key, created);
                return;
            }

            state.player = player;
            state.pendingCoins += coins;
            state.pendingPay += pay;

            if (state.future != null) {
                state.future.cancel(false);
            }
            state.future = scheduleFlush(key, state);
        }
    }

    public void clearPlayer(UUID uuid) {
        if (uuid == null) return;

        synchronized (buckets) {
            buckets.entrySet().removeIf(entry -> {
                if (!entry.getKey().uuid.equals(uuid)) return false;
                BucketState state = entry.getValue();
                if (state.future != null) {
                    state.future.cancel(false);
                }
                return true;
            });
        }
    }

    public void shutdown() {
        synchronized (buckets) {
            for (BucketState state : buckets.values()) {
                if (state.future != null) {
                    state.future.cancel(false);
                }
            }
            buckets.clear();
        }
        scheduler.shutdownNow();
    }

    private ScheduledFuture<?> scheduleFlush(BucketKey key, BucketState state) {
        return scheduler.schedule(() -> flush(key, state), BURST_WINDOW_MS, TimeUnit.MILLISECONDS);
    }

    private void flush(BucketKey key, BucketState state) {
        Player playerToMessage;
        int coinsToSend;
        double payToSend;

        synchronized (buckets) {
            BucketState current = buckets.get(key);
            if (current != state) {
                return;
            }

            coinsToSend = current.pendingCoins;
            payToSend = current.pendingPay;
            playerToMessage = current.player;
            buckets.remove(key);
        }

        if (playerToMessage == null) return;
        if (coinsToSend <= 0 && payToSend <= 0.0) return;

        sendTranslated(playerToMessage, key.langKey, coinsToSend, payToSend);
    }

    private void sendTranslated(Player player, String langKey, int coins, double pay) {
        String resolvedLang = languageManager.resolveLang(player.getPlayerRef().getLanguage());
        Map<String, Object> vars = new HashMap<>();
        vars.put("pay", pay);
        vars.put("coins", coins);
        player.sendMessage(languageManager.trMsg(resolvedLang, langKey, vars));
    }

    private static final class BucketState {
        private Player player;
        private int pendingCoins;
        private double pendingPay;
        private ScheduledFuture<?> future;

        private BucketState(Player player) {
            this.player = player;
        }
    }

    private record BucketKey(UUID uuid, String langKey) {}
}
