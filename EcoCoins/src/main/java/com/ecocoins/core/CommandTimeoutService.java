package com.ecocoins.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Timeout tipo /tpa para comandos: espera por tiempo + cancelación si el jugador sale del mismo bloque.
 */
public final class CommandTimeoutService {

    public static final String PERM_VIP = "ecocoins.vip";
    public static final String PERM_TIMEPASS = "ecocoins.timepass";

    private static final long TICK_MS = 100L;

    private final HytaleLogger logger;
    private final ScheduledExecutorService scheduler;
    private final Map<UUID, PendingCommand> pendingByPlayer = new ConcurrentHashMap<>();

    public CommandTimeoutService(HytaleLogger logger) {
        this.logger = logger;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EcoCoins-CommandTimeoutService");
            t.setDaemon(true);
            return t;
        });
    }

    public void executeWithTimeout(Player player,
                                   Store<EntityStore> store,
                                   Ref<EntityStore> playerEntityRef,
                                   PlayerRef playerRef,
                                   String commandLabel,
                                   int defaultSeconds,
                                   int vipSeconds,
                                   Runnable action) {
        if (player == null || store == null || playerRef == null) {
            return;
        }

        if (player.hasPermission(PERM_TIMEPASS)) {
            action.run();
            return;
        }

        int waitSeconds = player.hasPermission(PERM_VIP) ? vipSeconds : defaultSeconds;
        if (waitSeconds <= 0) {
            action.run();
            return;
        }

        BlockPos origin = resolveBlockPos(store, playerEntityRef, playerRef);
        if (origin == null) {
            sendMessageIfOnline(store, playerEntityRef, playerRef,
                    Message.raw("[EcoCoins] No pude resolver tu bloque actual para iniciar espera."));
            return;
        }

        UUID uuid = playerRef.getUuid();
        PendingCommand replaced = pendingByPlayer.remove(uuid);
        if (replaced != null) {
            replaced.cancel();
            sendMessageIfOnline(store, playerEntityRef, playerRef,
                    Message.raw("[EcoCoins] Se reemplazó el comando en espera por uno nuevo."));
        }

        sendMessageIfOnline(store, playerEntityRef, playerRef,
                Message.raw("[EcoCoins] Espera " + waitSeconds + "s para ejecutar " + commandLabel
                        + ". No salgas del bloque actual."));

        PendingCommand pending = new PendingCommand(uuid, origin, action, System.currentTimeMillis(), waitSeconds);
        pendingByPlayer.put(uuid, pending);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> tickPending(pending, store, playerEntityRef, playerRef),
                TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
        pending.attachFuture(future);
    }

    public void cancelPending(PlayerRef playerRef) {
        if (playerRef == null) return;
        cancelPending(playerRef.getUuid());
    }

    public void cancelPending(UUID playerUuid) {
        if (playerUuid == null) return;
        PendingCommand pending = pendingByPlayer.remove(playerUuid);
        if (pending != null) {
            pending.cancel();
        }
    }

    public void shutdown() {
        for (PendingCommand pending : pendingByPlayer.values()) {
            pending.cancel();
        }
        pendingByPlayer.clear();
        scheduler.shutdownNow();
    }

    private void tickPending(PendingCommand pending,
                             Store<EntityStore> store,
                             Ref<EntityStore> playerEntityRef,
                             PlayerRef playerRef) {
        if (!pending.isActive()) {
            return;
        }

        BlockPos current = resolveBlockPos(store, playerEntityRef, playerRef);
        if (current == null) {
            pendingByPlayer.remove(pending.playerUuid, pending);
            pending.cancel();
            return;
        }

        if (!pending.origin.equals(current)) {
            if (pendingByPlayer.remove(pending.playerUuid, pending)) {
                pending.cancel();
                sendMessageIfOnline(store, playerEntityRef, playerRef,
                        Message.raw("[EcoCoins] Comando cancelado: saliste del bloque donde lo activaste."));
            }
            return;
        }

        long elapsedMs = System.currentTimeMillis() - pending.startedAtMs;
        if (elapsedMs < pending.waitSeconds * 1000L) {
            return;
        }

        if (pendingByPlayer.remove(pending.playerUuid, pending)) {
            pending.cancel();
            try {
                pending.action.run();
            } catch (Throwable t) {
                logger.at(Level.SEVERE).log("[EcoCoins] Error ejecutando comando diferido: "
                        + t.getClass().getName() + ": " + t.getMessage());
            }
        }
    }

    private void sendMessageIfOnline(Store<EntityStore> store,
                                     Ref<EntityStore> playerEntityRef,
                                     PlayerRef playerRef,
                                     Message message) {
        if (store == null) return;
        Player online = null;
        if (playerEntityRef != null && playerEntityRef.isValid()) {
            online = store.getComponent(playerEntityRef, Player.getComponentType());
        }
        if (online == null && playerRef != null) {
            online = store.getComponent(playerRef, Player.getComponentType());
        }
        if (online != null) {
            online.sendMessage(message);
        }
    }

    private BlockPos resolveBlockPos(Store<EntityStore> store,
                                     Ref<EntityStore> playerEntityRef,
                                     PlayerRef playerRef) {
        TransformComponent transform = null;
        if (playerEntityRef != null && playerEntityRef.isValid()) {
            transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        }
        if (transform == null && playerRef != null) {
            transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        }
        if (transform == null) return null;

        Vector3d pos = transform.getPosition();
        if (pos == null) return null;

        return new BlockPos(
                (int) Math.floor(pos.x),
                (int) Math.floor(pos.y),
                (int) Math.floor(pos.z)
        );
    }

    private static final class PendingCommand {
        private final UUID playerUuid;
        private final BlockPos origin;
        private final Runnable action;
        private final long startedAtMs;
        private final int waitSeconds;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private volatile ScheduledFuture<?> future;

        private PendingCommand(UUID playerUuid,
                               BlockPos origin,
                               Runnable action,
                               long startedAtMs,
                               int waitSeconds) {
            this.playerUuid = playerUuid;
            this.origin = origin;
            this.action = action;
            this.startedAtMs = startedAtMs;
            this.waitSeconds = waitSeconds;
        }

        private void attachFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        private boolean isActive() {
            return active.get();
        }

        private void cancel() {
            if (!active.compareAndSet(true, false)) return;
            ScheduledFuture<?> f = this.future;
            if (f != null) {
                f.cancel(false);
            }
        }
    }

    private record BlockPos(int x, int y, int z) {
    }
}
