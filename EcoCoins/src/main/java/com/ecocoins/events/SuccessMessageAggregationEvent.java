package com.ecocoins.events;

import com.ecocoins.core.SuccessMessageAggregationService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class SuccessMessageAggregationEvent {

    private final SuccessMessageAggregationService service;

    public SuccessMessageAggregationEvent(@Nonnull SuccessMessageAggregationService service) {
        this.service = service;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new SuccessMessageAggregationSystem(service));
    }

    private static final class SuccessMessageAggregationSystem extends EntityTickingSystem<EntityStore> {

        private final SuccessMessageAggregationService service;

        private SuccessMessageAggregationSystem(@Nonnull SuccessMessageAggregationService service) {
            this.service = service;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(
                    PlayerRef.getComponentType(),
                    Player.getComponentType()
            );
        }

        @Override
        public void tick(float deltaTime,
                         int index,
                         ArchetypeChunk<EntityStore> chunk,
                         Store<EntityStore> store,
                         CommandBuffer<EntityStore> buffer) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || !service.hasPending(playerRef.getUuid())) {
                return;
            }

            Ref<EntityStore> currentRef = chunk.getReferenceTo(index);
            buffer.run(queuedStore -> {
                Player player = queuedStore.getComponent(currentRef, Player.getComponentType());
                if (player == null) {
                    return;
                }
                service.flushDue(player, playerRef);
            });
        }
    }
}
