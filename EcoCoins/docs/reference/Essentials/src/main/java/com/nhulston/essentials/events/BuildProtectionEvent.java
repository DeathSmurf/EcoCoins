package com.nhulston.essentials.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.util.ConfigManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.UUID;

public class BuildProtectionEvent {
    private static final String BYPASS_PERMISSION = "essentials.build.bypass";
    private static final String PROTECTED_MESSAGE = "Building is disabled.";
    private static final String PROTECTED_COLOR = "#FF5555";

    private final ConfigManager configManager;

    public BuildProtectionEvent(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new BreakBlockProtectionSystem(configManager));
        registry.registerSystem(new PlaceBlockProtectionSystem(configManager));
        registry.registerSystem(new DamageBlockProtectionSystem(configManager));
    }

    private static boolean canBypass(@Nonnull UUID playerUuid) {
        return PermissionsModule.get().hasPermission(playerUuid, BYPASS_PERMISSION);
    }

    private static void sendProtectedMessage(PlayerRef playerRef) {
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(PROTECTED_MESSAGE).color(PROTECTED_COLOR));
        }
    }

    /**
     * Prevents block breaking globally.
     */
    private static class BreakBlockProtectionSystem
            extends EntityEventSystem<EntityStore, BreakBlockEvent> {

        private final ConfigManager configManager;

        BreakBlockProtectionSystem(ConfigManager configManager) {
            super(BreakBlockEvent.class);
            this.configManager = configManager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull BreakBlockEvent event) {
            if (!configManager.isBuildingDisabled() || event.isCancelled()) {
                return;
            }

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null && canBypass(playerRef.getUuid())) {
                return;
            }

            event.setCancelled(true);
            sendProtectedMessage(playerRef);
        }
    }

    /**
     * Prevents block placing globally.
     */
    private static class PlaceBlockProtectionSystem
            extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

        private final ConfigManager configManager;

        PlaceBlockProtectionSystem(ConfigManager configManager) {
            super(PlaceBlockEvent.class);
            this.configManager = configManager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull PlaceBlockEvent event) {
            if (!configManager.isBuildingDisabled() || event.isCancelled()) {
                return;
            }

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null && canBypass(playerRef.getUuid())) {
                return;
            }

            event.setCancelled(true);
            sendProtectedMessage(playerRef);
        }
    }

    /**
     * Prevents block damage (mining progress) globally.
     */
    private static class DamageBlockProtectionSystem
            extends EntityEventSystem<EntityStore, DamageBlockEvent> {

        private final ConfigManager configManager;

        DamageBlockProtectionSystem(ConfigManager configManager) {
            super(DamageBlockEvent.class);
            this.configManager = configManager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull DamageBlockEvent event) {
            if (!configManager.isBuildingDisabled() || event.isCancelled()) {
                return;
            }

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null && canBypass(playerRef.getUuid())) {
                return;
            }

            event.setCancelled(true);
        }
    }
}
