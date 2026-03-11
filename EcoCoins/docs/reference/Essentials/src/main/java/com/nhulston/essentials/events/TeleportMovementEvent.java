package com.nhulston.essentials.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.TeleportManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Checks player movement each tick and cancels pending teleports if they move too far.
 */
public class TeleportMovementEvent {
    private final TeleportManager teleportManager;

    public TeleportMovementEvent(@Nonnull TeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new TeleportMovementCheckSystem(teleportManager));
    }

    /**
     * System that checks player movement for pending teleports.
     */
    private static class TeleportMovementCheckSystem extends EntityTickingSystem<EntityStore> {
        private final TeleportManager teleportManager;

        TeleportMovementCheckSystem(@Nonnull TeleportManager teleportManager) {
            this.teleportManager = teleportManager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> chunk,
                         @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            // Only check if player has a pending teleport
            if (!teleportManager.hasPendingTeleport(playerRef.getUuid())) {
                return;
            }

            // Get the current entity ref from this tick (not the stored one from command time)
            Ref<EntityStore> currentRef = chunk.getReferenceTo(index);
            
            Vector3d currentPosition = playerRef.getTransform().getPosition();
            teleportManager.tick(playerRef.getUuid(), currentRef, currentPosition, deltaTime, buffer);
        }
    }
}
