package me.basiqueevangelist.arstotzka.limbo;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public final class LimboEvent {
    public static final Event<Joined> JOINED = EventFactory.createArrayBacked(Joined.class, handlers -> handler -> {
        CompletableFuture<Void> proceed = null;

        for (Joined joinHandler : handlers) {
            if (proceed == null)
                proceed = joinHandler.onJoined(handler);
            else
                proceed = proceed.thenCompose(unused -> joinHandler.onJoined(handler));
        }

        return proceed;
    });

    public interface Joined {
        CompletableFuture<Void> onJoined(LimboNetworkHandler handler);
    }

    public interface Tick {
        void onTick();
    }

    public interface Disconnect {
        void onDisconnect(Text reason);
    }
}
