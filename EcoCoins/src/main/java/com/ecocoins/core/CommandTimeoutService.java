package com.ecocoins.core;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Timeout tipo Essentials para comandos: espera por tiempo + cancelación al salir del bloque origen.
 *
 * Flujo equivalente a TeleportManager de Essentials:
 * 1) queueCommand(...) registra pending con putIfAbsent
 * 2) sistema de movimiento llama tick(...) cada frame de jugador
 * 3) tick cancela por movimiento de bloque o ejecuta al completar contador
 */
public final class CommandTimeoutService {

    public static final String PERM_VIP = "ecocoins.vip";
    public static final String PERM_TIMEPASS = "ecocoins.timepass";

    public enum TimeoutProfile {
        CHANGE("/change", 5, 3),
        CHANGE_ALL("/changeall", 15, 7);

        private final String commandLabel;
        private final int defaultSeconds;
        private final int vipSeconds;

        TimeoutProfile(String commandLabel, int defaultSeconds, int vipSeconds) {
            this.commandLabel = commandLabel;
            this.defaultSeconds = defaultSeconds;
            this.vipSeconds = vipSeconds;
        }

        public String commandLabel() {
            return commandLabel;
        }

        public int defaultSeconds() {
            return defaultSeconds;
        }

        public int vipSeconds() {
            return vipSeconds;
        }
    }

    private enum TimeoutTier { DEFAULT, VIP, TIMEPASS }

    public enum TimeoutProfile {
        CHANGE("/change", 5, 3),
        CHANGE_ALL("/changeall", 15, 7);

        private final String commandLabel;
        private final int defaultSeconds;
        private final int vipSeconds;

        TimeoutProfile(String commandLabel, int defaultSeconds, int vipSeconds) {
            this.commandLabel = commandLabel;
            this.defaultSeconds = defaultSeconds;
            this.vipSeconds = vipSeconds;
        }

        public String commandLabel() {
            return commandLabel;
        }

        public int defaultSeconds() {
            return defaultSeconds;
        }

        public int vipSeconds() {
            return vipSeconds;
        }
    }

    private final HytaleLogger logger;
    private final Map<UUID, PendingCommand> pendingByPlayer = new ConcurrentHashMap<>();

    public CommandTimeoutService(HytaleLogger logger) {
        this.logger = logger;
    }

    public void executeWithProfile(Player player,
                                   Store<EntityStore> store,
                                   Ref<EntityStore> playerEntityRef,
                                   PlayerRef playerRef,
                                   TimeoutProfile profile,
                                   Runnable action) {
        if (profile == null) {
            action.run();
            return;
        }

        executeWithTimeout(
                player,
                store,
                playerEntityRef,
                playerRef,
                profile.commandLabel(),
                profile.defaultSeconds(),
                profile.vipSeconds(),
                action
        );
    }

    public void executeWithTimeout(Player player,
                                   Store<EntityStore> store,
                                   Ref<EntityStore> playerEntityRef,
                                   PlayerRef playerRef,
                                   String commandLabel,
                                   int defaultSeconds,
                                   int vipSeconds,
                                   Runnable action) {
        if (player == null || store == null || playerEntityRef == null || !playerEntityRef.isValid() || playerRef == null) {
            return;
        }

        TimeoutTier tier = resolveTimeoutTier(playerRef);
        if (tier == TimeoutTier.TIMEPASS) {
            action.run();
            return;
        }

        int waitSeconds = (tier == TimeoutTier.VIP) ? vipSeconds : defaultSeconds;
        if (waitSeconds <= 0) {
            action.run();
            return;
        }

        BlockPos origin = resolveBlockPos(store, playerEntityRef);
        if (origin == null) {
            player.sendMessage(Message.raw("[EcoCoins] No pude resolver tu bloque actual para iniciar espera."));
            return;
        }

        PendingCommand pending = new PendingCommand(origin, action, waitSeconds);
        PendingCommand existing = pendingByPlayer.putIfAbsent(playerRef.getUuid(), pending);
        if (existing != null) {
            player.sendMessage(Message.raw("[EcoCoins] Ya tienes un comando en espera. Por favor espera a que termine."));
            return;
        }

        player.sendMessage(Message.raw("[EcoCoins] Espera " + waitSeconds + "s para ejecutar " + commandLabel + ". No salgas del bloque actual."));
    }

    public boolean hasPending(UUID playerUuid) {
        return playerUuid != null && pendingByPlayer.containsKey(playerUuid);
    }

    public void tick(UUID playerUuid,
                     Ref<EntityStore> currentRef,
                     Vector3d currentPosition,
                     float deltaTime,
                     CommandBuffer<EntityStore> buffer) {
        if (playerUuid == null || currentRef == null || !currentRef.isValid() || currentPosition == null || buffer == null) {
            return;
        }

        PendingCommand pending = pendingByPlayer.get(playerUuid);
        if (pending == null) {
            return;
        }

        BlockPos current = BlockPos.from(currentPosition);
        if (!pending.origin.equals(current)) {
            PendingCommand removed = pendingByPlayer.remove(playerUuid);
            if (removed != null) {
                buffer.run(store -> sendMessageIfOnline(store, currentRef,
                        Message.raw("[EcoCoins] Comando cancelado: saliste del bloque donde lo activaste.")));
            }
            return;
        }

        pending.elapsedSeconds += deltaTime;
        if (pending.elapsedSeconds < pending.waitSeconds) {
            return;
        }

        PendingCommand removed = pendingByPlayer.remove(playerUuid);
        if (removed == null) {
            return;
        }

        buffer.run(store -> {
            try {
                removed.action.run();
            } catch (Throwable t) {
                logger.at(Level.SEVERE).log("[EcoCoins] Error ejecutando comando diferido: "
                        + t.getClass().getName() + ": " + t.getMessage());
            }
        });
    }

    public void executeWithProfile(Player player,
                                   Store<EntityStore> store,
                                   Ref<EntityStore> playerEntityRef,
                                   PlayerRef playerRef,
                                   TimeoutProfile profile,
                                   Runnable action) {
        if (profile == null) {
            action.run();
            return;
        }

        executeWithTimeout(
                player,
                store,
                playerEntityRef,
                playerRef,
                profile.commandLabel(),
                profile.defaultSeconds(),
                profile.vipSeconds(),
                action
        );
    }

    public void cancelPending(PlayerRef playerRef) {
        if (playerRef == null) return;
        cancelPending(playerRef.getUuid());
    }

    public void cancelPending(UUID playerUuid) {
        if (playerUuid == null) return;
        pendingByPlayer.remove(playerUuid);
    }

    public void shutdown() {
        pendingByPlayer.clear();
    }

    private TimeoutTier resolveTimeoutTier(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        Set<String> groups = getGroupsSafe(uuid);
        boolean onlyDefaultGroup = groups.isEmpty() || (groups.size() == 1 && groups.contains("default"));

        if (groups.contains("ecocoins.timepass") || groups.contains("timepass")) {
            return TimeoutTier.TIMEPASS;
        }
        if (!onlyDefaultGroup && hasPermissionSafe(uuid, PERM_TIMEPASS)) {
            return TimeoutTier.TIMEPASS;
        }

        if (groups.contains("ecocoins.vip") || groups.contains("vip")) {
            return TimeoutTier.VIP;
        }
        if (!onlyDefaultGroup && hasPermissionSafe(uuid, PERM_VIP)) {
            return TimeoutTier.VIP;
        }

        return TimeoutTier.DEFAULT;
    }

    private Set<String> getGroupsSafe(UUID uuid) {
        try {
            Set<String> groups = PermissionsModule.get().getGroupsForUser(uuid);
            return groups != null ? groups : Set.of();
        } catch (Throwable t) {
            return Set.of();
        }
    }

    private boolean hasPermissionSafe(UUID uuid, String permissionNode) {
        try {
            return PermissionsModule.get().hasPermission(uuid, permissionNode);
        } catch (Throwable t) {
            logger.at(Level.WARNING).log("[EcoCoins] No pude consultar permisos para nodo '"
                    + permissionNode + "'. Uso valor por defecto. Detalle: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private void sendMessageIfOnline(Store<EntityStore> store,
                                     Ref<EntityStore> playerEntityRef,
                                     Message message) {
        if (store == null || playerEntityRef == null || !playerEntityRef.isValid()) return;
        Player online = store.getComponent(playerEntityRef, Player.getComponentType());
        if (online != null) {
            online.sendMessage(message);
        }
    }

    private BlockPos resolveBlockPos(Store<EntityStore> store,
                                     Ref<EntityStore> playerEntityRef) {
        if (store == null || playerEntityRef == null || !playerEntityRef.isValid()) return null;

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) return null;

        Vector3d pos = transform.getPosition();
        if (pos == null) return null;

        return BlockPos.from(pos);
    }

    private static final class PendingCommand {
        private final BlockPos origin;
        private final Runnable action;
        private final int waitSeconds;
        private float elapsedSeconds;

        private PendingCommand(BlockPos origin,
                               Runnable action,
                               int waitSeconds) {
            this.origin = origin;
            this.action = action;
            this.waitSeconds = waitSeconds;
            this.elapsedSeconds = 0.0f;
        }
    }

    private record BlockPos(int x, int y, int z) {
        private static BlockPos from(Vector3d pos) {
            return new BlockPos(
                    (int) Math.floor(pos.x),
                    (int) Math.floor(pos.y),
                    (int) Math.floor(pos.z)
            );
        }
    }
}
