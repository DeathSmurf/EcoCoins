package com.ecocoins.events;

import com.ecocoins.core.CommandTimeoutService;
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

import javax.annotation.Nonnull;

/**
 * Sistema de contador de timeout tipo Essentials.
 * Tiquea solo jugadores y notifica al servicio para cancelar/ejecutar comandos diferidos.
 */
public final class CommandTimeoutMovementEvent {

    private final CommandTimeoutService timeoutService;

    public CommandTimeoutMovementEvent(@Nonnull CommandTimeoutService timeoutService) {
        this.timeoutService = timeoutService;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new CommandTimeoutMovementSystem(timeoutService));
    }

    private static final class CommandTimeoutMovementSystem extends EntityTickingSystem<EntityStore> {

        private final CommandTimeoutService timeoutService;

        private CommandTimeoutMovementSystem(@Nonnull CommandTimeoutService timeoutService) {
            this.timeoutService = timeoutService;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void tick(float deltaTime,
                         int index,
                         ArchetypeChunk<EntityStore> chunk,
                         Store<EntityStore> store,
                         CommandBuffer<EntityStore> buffer) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            if (!timeoutService.hasPending(playerRef.getUuid())) {
                return;
            }

            Ref<EntityStore> currentRef = chunk.getReferenceTo(index);
            Vector3d currentPosition = playerRef.getTransform().getPosition();
            timeoutService.tick(playerRef.getUuid(), store, currentRef, currentPosition, deltaTime);
        }
    }
}
