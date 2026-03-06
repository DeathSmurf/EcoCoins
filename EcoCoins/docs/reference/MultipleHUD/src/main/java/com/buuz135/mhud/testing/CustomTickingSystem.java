package com.buuz135.mhud.testing;


import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.awt.*;
import java.util.HashMap;

public class CustomTickingSystem extends EntityTickingSystem<EntityStore> {

    public CustomTickingSystem() {

    }

    @Override
    public void tick(float v, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        assert playerRef != null;
        assert player != null;
        MultipleHUD.getInstance().setCustomHud(player, playerRef, "Hud1", new TestUIHUD(playerRef, "Buuz135_MHUD_ChunkEntry.ui"));
        MultipleHUD.getInstance().setCustomHud(player, playerRef, "Hud2", new TestUIHUD(playerRef, "Buuz135_MHUD_PartyMemberListEntry.ui"));
        //MultipleHUD.getInstance().hideCustomHud(player, playerRef, "Hud2");
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
