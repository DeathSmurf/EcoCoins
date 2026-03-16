package com.ecocoins.core;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuccessMessageAggregationService {

    private static final long WINDOW_MS = 2_000L;

    private final LanguageManager lang;
    private final Map<UUID, Pending> pendingByPlayer = new ConcurrentHashMap<>();

    public SuccessMessageAggregationService(LanguageManager lang) {
        this.lang = lang;
    }

    public void onRedeemSuccess(Player player, PlayerRef playerRef, double pay) {
        enqueue(player, playerRef, Type.REDEEM, null, null, 0, pay, 0, 0.0);
    }

    public void onChangeHandSuccess(Player player, PlayerRef playerRef, int coins, double pay) {
        enqueue(player, playerRef, Type.CHANGE_HAND, null, null, coins, pay, 0, 0.0);
    }

    public void onChangeAllSuccess(Player player, PlayerRef playerRef, int coins, double pay) {
        enqueue(player, playerRef, Type.CHANGE_ALL, null, null, coins, pay, 0, 0.0);
    }

    public void onChangeSuccess(Player player,
                                PlayerRef playerRef,
                                String primaryMoneyName,
                                String itemId,
                                int amount,
                                double cost) {
        enqueue(player, playerRef, Type.CHANGE, primaryMoneyName, itemId, 0, 0.0, amount, cost);
    }

    public boolean hasPending(UUID playerUuid) {
        return playerUuid != null && pendingByPlayer.containsKey(playerUuid);
    }

    public void flushDue(Player player, PlayerRef playerRef) {
        if (player == null || playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        Pending pending = pendingByPlayer.get(uuid);
        if (pending == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if ((now - pending.lastActivityAtMs) <= WINDOW_MS) {
            return;
        }

        flushPending(player, pending);
        pendingByPlayer.remove(uuid);
    }

    public void clear(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        pendingByPlayer.remove(playerUuid);
    }

    public void shutdown() {
        pendingByPlayer.clear();
    }

    private void enqueue(Player player,
                         PlayerRef playerRef,
                         Type type,
                         String primaryMoneyName,
                         String itemId,
                         int coins,
                         double pay,
                         int amount,
                         double cost) {
        if (player == null || playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        long now = System.currentTimeMillis();
        Pending pending = pendingByPlayer.get(uuid);

        if (pending == null || pending.type != type || !pending.matches(primaryMoneyName, itemId)) {
            if (pending != null) {
                flushPending(player, pending);
            }

            sendNow(player, playerRef, type, primaryMoneyName, itemId, coins, pay, amount, cost);
            pendingByPlayer.put(uuid, new Pending(type, playerRef.getLanguage(), primaryMoneyName, itemId, now));
            return;
        }

        if ((now - pending.lastActivityAtMs) > WINDOW_MS) {
            flushPending(player, pending);
            sendNow(player, playerRef, type, primaryMoneyName, itemId, coins, pay, amount, cost);
            pendingByPlayer.put(uuid, new Pending(type, playerRef.getLanguage(), primaryMoneyName, itemId, now));
            return;
        }

        pending.lastActivityAtMs = now;
        pending.langCode = playerRef.getLanguage();
        pending.pendingCoins += coins;
        pending.pendingPay += pay;
        pending.pendingAmount += amount;
        pending.pendingCost += cost;
    }

    private void sendNow(Player player,
                         PlayerRef playerRef,
                         Type type,
                         String primaryMoneyName,
                         String itemId,
                         int coins,
                         double pay,
                         int amount,
                         double cost) {
        String pLang = lang.resolveLang(playerRef.getLanguage());
        player.sendMessage(buildMessage(type, pLang, primaryMoneyName, itemId, coins, pay, amount, cost));
    }

    private void flushPending(Player player, Pending pending) {
        if (!pending.hasPendingValues()) {
            return;
        }

        String pLang = lang.resolveLang(pending.langCode);
        player.sendMessage(buildMessage(
                pending.type,
                pLang,
                pending.primaryMoneyName,
                pending.itemId,
                pending.pendingCoins,
                pending.pendingPay,
                pending.pendingAmount,
                pending.pendingCost
        ));
    }

    private Message buildMessage(Type type,
                                 String pLang,
                                 String primaryMoneyName,
                                 String itemId,
                                 int coins,
                                 double pay,
                                 int amount,
                                 double cost) {
        return switch (type) {
            case REDEEM -> lang.trMsg(pLang, "redeem.success", Map.of("pay", pay));
            case CHANGE_HAND -> lang.trMsg(pLang, "command.changehand.success", Map.of("coins", coins, "pay", pay));
            case CHANGE_ALL -> lang.trMsg(pLang, "command.changeall.success", Map.of("coins", coins, "pay", pay));
            case CHANGE -> buildChangeSuccessMessage(pLang, primaryMoneyName, itemId, amount, cost);
        };
    }

    private Message buildChangeSuccessMessage(String pLang,
                                              String primaryMoneyName,
                                              String itemId,
                                              int amount,
                                              double cost) {
        String successTemplate = lang.tr(pLang, "command.change.success", Map.of(
                "amount", amount,
                "primary", primaryMoneyName,
                "cost", cost
        ));
        String moneyNamePlaceholder = "{lang_moneyName}";
        int moneyNameIndex = successTemplate.indexOf(moneyNamePlaceholder);

        Message itemName = Message.translation("server.items." + itemId + ".name");
        if (moneyNameIndex < 0) {
            return lang.rawToMsg(successTemplate);
        }

        String before = successTemplate.substring(0, moneyNameIndex);
        String after = successTemplate.substring(moneyNameIndex + moneyNamePlaceholder.length());
        applyLegacyStyleFromPrefix(itemName, before);
        return Message.join(lang.rawToMsg(before), itemName, lang.rawToMsg(after));
    }

    private static void applyLegacyStyleFromPrefix(Message target, String prefix) {
        LegacyStyle style = resolveLegacyStyle(prefix);
        if (style.colorHex != null) target.color(style.colorHex);
        if (style.bold) target.bold(true);
        if (style.italic) target.italic(true);
        if (style.monospace) target.monospace(true);
    }

    private static LegacyStyle resolveLegacyStyle(String input) {
        String colorHex = null;
        boolean bold = false;
        boolean italic = false;
        boolean monospace = false;

        if (input == null || input.isEmpty()) {
            return new LegacyStyle(colorHex, bold, italic, monospace);
        }

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch != '&' || i + 1 >= input.length()) continue;

            char code = Character.toLowerCase(input.charAt(i + 1));
            i++;

            String mapped = mapLegacyColorCodeToHex(code);
            if (mapped != null) {
                colorHex = mapped;
                continue;
            }

            switch (code) {
                case 'l' -> bold = true;
                case 'o' -> italic = true;
                case 'p' -> monospace = true;
                case 'r' -> {
                    colorHex = null;
                    bold = false;
                    italic = false;
                    monospace = false;
                }
                default -> { }
            }
        }

        return new LegacyStyle(colorHex, bold, italic, monospace);
    }

    private static String mapLegacyColorCodeToHex(char code) {
        return switch (code) {
            case '0' -> "#000000";
            case '1' -> "#0000AA";
            case '2' -> "#00AA00";
            case '3' -> "#00AAAA";
            case '4' -> "#AA0000";
            case '5' -> "#AA00AA";
            case '6' -> "#FFAA00";
            case '7' -> "#AAAAAA";
            case '8' -> "#555555";
            case '9' -> "#5555FF";
            case 'a' -> "#55FF55";
            case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555";
            case 'd' -> "#FF55FF";
            case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF";
            default -> null;
        };
    }

    private enum Type {
        REDEEM,
        CHANGE_HAND,
        CHANGE_ALL,
        CHANGE
    }

    private static final class Pending {
        private final Type type;
        private String langCode;
        private final String primaryMoneyName;
        private final String itemId;
        private long lastActivityAtMs;
        private int pendingCoins;
        private double pendingPay;
        private int pendingAmount;
        private double pendingCost;

        private Pending(Type type,
                        String langCode,
                        String primaryMoneyName,
                        String itemId,
                        long lastActivityAtMs) {
            this.type = type;
            this.langCode = langCode;
            this.primaryMoneyName = primaryMoneyName;
            this.itemId = itemId;
            this.lastActivityAtMs = lastActivityAtMs;
        }

        private boolean hasPendingValues() {
            return pendingCoins > 0 || pendingAmount > 0 || pendingPay > 0.0 || pendingCost > 0.0;
        }

        private boolean matches(String primaryMoneyName, String itemId) {
            return java.util.Objects.equals(this.primaryMoneyName, primaryMoneyName)
                    && java.util.Objects.equals(this.itemId, itemId);
        }
    }

    private static final class LegacyStyle {
        private final String colorHex;
        private final boolean bold;
        private final boolean italic;
        private final boolean monospace;

        private LegacyStyle(String colorHex, boolean bold, boolean italic, boolean monospace) {
            this.colorHex = colorHex;
            this.bold = bold;
            this.italic = italic;
            this.monospace = monospace;
        }
    }
}
