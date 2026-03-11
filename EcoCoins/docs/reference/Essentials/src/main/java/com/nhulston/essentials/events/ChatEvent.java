package com.nhulston.essentials.events;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.nhulston.essentials.managers.ChatManager;

import javax.annotation.Nonnull;

public class ChatEvent {
    private final ChatManager chatManager;

    public ChatEvent(@Nonnull ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.<String, PlayerChatEvent>registerAsyncGlobal(PlayerChatEvent.class, future ->
                future.thenApply(event -> {
                    if (chatManager.isEnabled()) {
                        event.setFormatter(chatManager.createFormatter());
                    }
                    return event;
                })
        );
    }
}
