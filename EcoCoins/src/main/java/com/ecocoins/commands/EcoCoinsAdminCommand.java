package com.ecocoins.commands;

import com.ecocoins.core.CoinManager;
import com.ecocoins.core.LanguageManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * /ecocoins reload
 * Recarga monedas e idiomas desde disco sin reiniciar servidor.
 */
public final class EcoCoinsAdminCommand extends AbstractPlayerCommand {

    public static final String PERM_ECOCOINS_RELOAD_USE = "ecocoins.command.reload.use";

    private final LanguageManager lang;
    private final CoinManager coins;
    private final HytaleLogger logger;
    private final RequiredArg<String> actionArg;

    public EcoCoinsAdminCommand(LanguageManager lang, CoinManager coins, HytaleLogger logger) {
        super("ecocoins", "Comandos administrativos de EcoCoins. Usa: /ecocoins reload", false);
        this.lang = lang;
        this.coins = coins;
        this.logger = logger;
        this.actionArg = withRequiredArg("action", "Acción administrativa (reload).", ArgTypes.STRING);
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
        String action = ctx.get(actionArg);

        if (action == null || !"reload".equalsIgnoreCase(action.trim())) {
            player.sendMessage(lang.trMsg(pLang, "command.ecocoins.usage", Map.of()));
            return;
        }

        if (!hasReloadPermission(player, playerRef)) {
            player.sendMessage(lang.trMsg(pLang, "common.no_permission", Map.of()));
            return;
        }

        try {
            lang.loadAll();
            coins.loadAll();

            logger.at(Level.INFO).log("[EcoCoins] Reload ejecutado por " + playerRef.getUsername()
                    + " (" + playerRef.getUuid() + ")"
                    + " | coins=" + coins.countCoins()
                    + " langs=" + lang.countLanguages());

            String reloadedLang = lang.resolveLang(playerRef.getLanguage());
            player.sendMessage(lang.trMsg(reloadedLang, "command.ecocoins.reload.success", Map.of(
                    "coins", coins.countCoins(),
                    "langs", lang.countLanguages()
            )));
            player.sendMessage(lang.trMsg(reloadedLang, "command.ecocoins.reload.assets_restart_required", Map.of()));
        } catch (Throwable t) {
            logger.at(Level.SEVERE).log("[EcoCoins] Error en /ecocoins reload: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());

            String resolvedLang = lang.resolveLang(playerRef.getLanguage());
            player.sendMessage(lang.trMsg(resolvedLang, "command.ecocoins.reload.failed", Map.of()));
        }
    }

    private boolean hasReloadPermission(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) return false;
        if (player.hasPermission(PERM_ECOCOINS_RELOAD_USE)) return true;

        UUID uuid = playerRef.getUuid();
        try {
            Set<String> groups = PermissionsModule.get().getGroupsForUser(uuid);
            if (groups == null || groups.isEmpty()) return false;

            for (String group : groups) {
                if (group == null) continue;
                String g = group.toLowerCase(Locale.ROOT);
                if (g.equals("op") || g.equals("operator") || g.equals("owner") || g.equals("admin")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Fallback: si falla módulo de permisos, nos quedamos con hasPermission().
        }

        return false;
    }
}
