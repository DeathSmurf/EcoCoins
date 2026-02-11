package com.ecocoins.ecs;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Marca una entidad de item dropeado como "moneda EcoCoins" con dueño temporal.
 * Durante secondsRemaining el dueño puede recoger automáticamente (solo él).
 * Luego se convierte a pickup interactivo (click) para cualquiera.
 */
public final class EcoCoinsOwnerPickupLock implements Component<EntityStore> {

    private Ref<EntityStore> owner;
    private float secondsRemaining;

    public EcoCoinsOwnerPickupLock() {
        // requerido por el registry
    }

    public EcoCoinsOwnerPickupLock(Ref<EntityStore> owner, float secondsRemaining) {
        this.owner = owner;
        this.secondsRemaining = secondsRemaining;
    }

    public Ref<EntityStore> getOwner() {
        return owner;
    }

    public void setOwner(Ref<EntityStore> owner) {
        this.owner = owner;
    }

    public float getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(float secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }

    public void tickDown(float dt) {
        this.secondsRemaining -= dt;
    }

    @Override
    public EcoCoinsOwnerPickupLock clone() {
        return new EcoCoinsOwnerPickupLock(owner, secondsRemaining);
    }

    public static ComponentType<EntityStore, EcoCoinsOwnerPickupLock> getComponentType(ComponentType<EntityStore, EcoCoinsOwnerPickupLock> type) {
        return type;
    }
}