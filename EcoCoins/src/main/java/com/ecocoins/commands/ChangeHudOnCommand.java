package com.ecocoins.commands;

import com.ecocoins.core.LanguageManager;
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

public final class ChangeHudOnCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGEON_USE = "ecocoins.command.changeon.use";

    private final LanguageManager lang;
    private final BalanceHudService hudService;

    public ChangeHudOnCommand(LanguageManager lang, BalanceHudService hudService) {
        super("changeon", "Muestra otra vez el HUD de balance de EcoCoins.", false);
        this.lang = lang;
        this.hudService = hudService;
        this.requirePermission(PERM_CHANGEON_USE);
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

        String pLang = lang.resolveLang(playerRef.getLanguage());

        hudService.show(player, playerRef);
        ctx.sendMessage(lang.trMsg(pLang, "command.changeon.success", java.util.Map.of()));
    }
}
