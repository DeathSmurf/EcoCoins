package com.ecocoins.commands;

import com.ecocoins.hud.BalanceHudService;
import com.ecocoins.hud.EcoCoinBalanceHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ChangePositionCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGEPOSITION_USE = "ecocoins.command.changeposition.use";

    private final BalanceHudService hudService;

    public ChangePositionCommand(BalanceHudService hudService) {
        super("changeposition", "Alterna la posición del HUD entre esquina inferior izquierda y derecha.", false);
        this.hudService = hudService;
        this.requirePermission(PERM_CHANGEPOSITION_USE);
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

        EcoCoinBalanceHud.Position position = hudService.togglePosition(player, playerRef);
        hudService.sendPositionMessage(player, position);
    }
}
