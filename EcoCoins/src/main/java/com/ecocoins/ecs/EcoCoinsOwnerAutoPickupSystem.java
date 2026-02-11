package com.ecocoins.ecs;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Mantiene la lógica:
 * - Durante 3s: SOLO el dueño recoge automáticamente (si está en rango).
 * - Después de 3s: el item pasa a pickup interactivo (click), nunca auto.
 */
public final class EcoCoinsOwnerAutoPickupSystem extends EntityTickingSystem<EntityStore> {

    private final com.hypixel.hytale.component.ComponentType<EntityStore, EcoCoinsOwnerPickupLock> lockType;

    public EcoCoinsOwnerAutoPickupSystem(com.hypixel.hytale.component.ComponentType<EntityStore, EcoCoinsOwnerPickupLock> lockType) {
        super();
        this.lockType = lockType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                ItemComponent.getComponentType(),
                TransformComponent.getComponentType(),
                lockType
        );
    }

    @Override
    public boolean isParallel(int start, int end) {
        return false; // toca inventario / entidades
    }

    @Override
    public void tick(float dt,
                     int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> cmd) {

        Ref<EntityStore> itemRef = chunk.getReferenceTo(index);

        ItemComponent itemComp = (ItemComponent) chunk.getComponent(index, ItemComponent.getComponentType());
        TransformComponent itemTransform = (TransformComponent) chunk.getComponent(index, TransformComponent.getComponentType());
        EcoCoinsOwnerPickupLock lock = (EcoCoinsOwnerPickupLock) chunk.getComponent(index, lockType);

        if (itemComp == null || itemTransform == null || lock == null) return;

        lock.tickDown(dt);

        // Mientras dure el lock, intentamos auto-pickup SOLO del dueño
        if (lock.getSecondsRemaining() > 0.0f) {
            Ref<EntityStore> ownerRef = lock.getOwner();
            if (ownerRef == null || !ownerRef.isValid()) return;

            TransformComponent ownerTransform = store.getComponent(ownerRef, TransformComponent.getComponentType());
            if (ownerTransform == null) return;

            Vector3d itemPos = itemTransform.getPosition();
            Vector3d ownerPos = ownerTransform.getPosition();

            float radius = itemComp.getPickupRadius(cmd);
            double dx = itemPos.x - ownerPos.x;
            double dy = itemPos.y - ownerPos.y;
            double dz = itemPos.z - ownerPos.z;
            double dist2 = dx*dx + dy*dy + dz*dz;

            if (dist2 <= (double)(radius * radius)) {
                // Recoge como si fuese una interacción (usa la lógica vanilla de pickup + animación)
                ItemUtils.interactivelyPickupItem(ownerRef, itemComp.getItemStack(), itemPos, cmd);
                cmd.removeEntity(itemRef, RemoveReason.REMOVE);
            }
            return;
        }

        // Expiró: quitar lock + permitir pickup interactivo para cualquiera.
        // IMPORTANT: nunca auto pickup de nuevo.
        cmd.tryRemoveComponent(itemRef, lockType);
        cmd.tryRemoveComponent(itemRef, PreventPickup.getComponentType());
        cmd.addComponent(itemRef, Interactable.getComponentType(), Interactable.INSTANCE);
    }
}