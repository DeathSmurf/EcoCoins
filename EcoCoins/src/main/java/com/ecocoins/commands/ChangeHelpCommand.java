package com.ecocoins.commands;

import com.ecocoins.core.LanguageManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;

public final class ChangeHelpCommand extends AbstractPlayerCommand {

    public static final String PERM_CHANGEHELP_USE = "ecocoins.command.changehelp.use";

    private final LanguageManager lang;

    public ChangeHelpCommand(LanguageManager lang) {
        super("changehelp", "Muestra ayuda de comandos de intercambio y HUD.", false);
        this.lang = lang;
        this.requirePermission(PERM_CHANGEHELP_USE);
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

        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.divline.first", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.header", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.section.exchange", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.line.change", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.line.changeall", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.line.changehand", Map.of()));
        ctx.sendMessage(Message.empty());
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.section.general", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.line.changeoff", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.line.changeon", Map.of()));
        ctx.sendMessage(lang.trMsg(pLang, "command.changehelp.divline.last", Map.of()));
    }
}
