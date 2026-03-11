package com.ecocoins.interactions;

import com.ecocoins.EcoCoins;
import com.ecocoins.core.CoinRedeemService;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Interaction ejecutada por el sistema de interacciones del item.
 * Esto evita depender de PlayerInteractEvent/PlayerMouseButtonEvent para canjear monedas.
 */
public class CoinRedeemInteraction extends SimpleInteraction {

    public static final BuilderCodec<CoinRedeemInteraction> CODEC = BuilderCodec.builder(
                    CoinRedeemInteraction.class,
                    CoinRedeemInteraction::new,
                    SimpleInteraction.CODEC
            )
            .documentation("Redeems EcoCoins item stack to digital balance.")
            .build();

    @Override
    protected void tick0(
            boolean local,
            float dt,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        // Mantener comportamiento base de SimpleInteraction (effects, next/failed chain, etc.)
        super.tick0(local, dt, type, context, cooldownHandler);

        if (type != InteractionType.Secondary) return;

        Ref<EntityStore> ref = context.getEntity();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        ItemStack itemInHand = context.getHeldItem();
        if (itemInHand == null || itemInHand.isEmpty()) return;

        EcoCoins plugin = EcoCoins.getInstance();
        if (plugin == null) return;

        CoinRedeemService redeemService = plugin.getCoinRedeemService();
        if (redeemService == null) return;

        redeemService.redeemFromHandIfEcoCoin(player, type, itemInHand);
    }
}
