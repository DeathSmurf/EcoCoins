package com.ecocoins.commands;

import com.ecocoins.hud.BalanceHudService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ChangeHudOffCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGEOFF_USE = "ecocoins.command.changeoff.use";

    private final BalanceHudService hudService;

    public ChangeHudOffCommand(BalanceHudService hudService) {
        super("changeoff", "Oculta el HUD de balance de EcoCoins.", false);
        this.hudService = hudService;
        this.requirePermission(PERM_CHANGEOFF_USE);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef,
                           World world) {
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("[EcoCoins] No pude resolver el Player entity."));
            return;
        }

        hudService.hide(player, playerRef);
        ctx.sendMessage(Message.raw("[EcoCoins] HUD de balance desactivado. Usa /changeon para volver a mostrarlo."));
    }
}
