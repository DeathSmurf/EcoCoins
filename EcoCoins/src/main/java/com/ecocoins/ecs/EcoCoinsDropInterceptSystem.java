package com.ecocoins.ecs;

import com.ecocoins.core.CoinManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Intercepta el drop de items del jugador. Si el item es una moneda EcoCoins,
 * cancela el drop normal y spawnea un item entity con:
 *  - PreventPickup (nadie auto-pickup por defecto)
 *  - EcoCoinsOwnerPickupLock (dueño + 3s)
 * Luego el sistema EcoCoinsOwnerAutoPickupSystem gestiona el auto pickup del dueño
 * por 3 segundos y después habilita pickup interactivo.
 */
public final class EcoCoinsDropInterceptSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {

    private static final float OWNER_AUTO_PICKUP_SECONDS = 3.0f;

    private final CoinManager coinManager;
    private final HytaleLogger logger;

    private final com.hypixel.hytale.component.ComponentType<EntityStore, EcoCoinsOwnerPickupLock> lockType;

    public EcoCoinsDropInterceptSystem(CoinManager coinManager,
                                      HytaleLogger logger,
                                      com.hypixel.hytale.component.ComponentType<EntityStore, EcoCoinsOwnerPickupLock> lockType) {
        super(DropItemEvent.Drop.class);
        this.coinManager = coinManager;
        this.logger = logger;
        this.lockType = lockType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Solo jugadores con los componentes necesarios para calcular dirección/posición
        return Query.and(
                com.hypixel.hytale.server.core.entity.entities.Player.getComponentType(),
                TransformComponent.getComponentType(),
                HeadRotation.getComponentType(),
                ModelComponent.getComponentType()
        );
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> cmd,
                       DropItemEvent.Drop event) {

        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty() || !stack.isValid()) return;

        String itemId = stack.getItem().getId();
        if (!coinManager.isEcoCoinItemId(itemId)) {
            return; // no es una moneda nuestra
        }

        // Cancelamos el drop normal: nosotros spawneamos nuestra entidad de item con lock
        event.setCancelled(true);

        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        HeadRotation headRot = store.getComponent(playerRef, HeadRotation.getComponentType());
        ModelComponent modelComp = store.getComponent(playerRef, ModelComponent.getComponentType());

        if (transform == null || headRot == null || modelComp == null) return;

        Vector3f rot = headRot.getRotation();
        Vector3d dir = Transform.getDirection(rot.getPitch(), rot.getYaw());

        Vector3d pos = transform.getPosition().clone();
        float eyeHeight = modelComp.getModel().getEyeHeight(playerRef, store);
        pos.add(0.0, (double) eyeHeight, 0.0);
        pos.add(dir);

        float throwSpeed = event.getThrowSpeed();
        float vx = (float) (dir.x * throwSpeed);
        float vy = (float) (dir.y * throwSpeed);
        float vz = (float) (dir.z * throwSpeed);

        // Generamos entidad de item como lo hace el server, pero la modificamos
        com.hypixel.hytale.component.Holder<EntityStore> holder =
                ItemComponent.generateItemDrop(cmd, stack, pos, Vector3f.ZERO, vx, vy, vz);

        if (holder == null) return;

        // Aseguramos que NO haya auto pickup por el sistema vanilla
        holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);

        // No la hacemos interactable al principio (solo auto del dueño durante 3s)
        // holder NO incluye Interactable.

        // Lock de dueño
        holder.addComponent(lockType, new EcoCoinsOwnerPickupLock(playerRef, OWNER_AUTO_PICKUP_SECONDS));

        // Si el ItemComponent existe, dejamos pickupDelay = 0 (no nos molesta, pero coherente)
        ItemComponent itemComp = holder.getComponent(ItemComponent.getComponentType());
        if (itemComp != null) itemComp.setPickupDelay(0.0f);

        cmd.addEntity(holder, AddReason.SPAWN);
    }
}