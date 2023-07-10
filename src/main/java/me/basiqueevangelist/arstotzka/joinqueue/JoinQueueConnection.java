package me.basiqueevangelist.arstotzka.joinqueue;

import me.basiqueevangelist.arstotzka.limbo.LimboEvent;
import me.basiqueevangelist.arstotzka.limbo.LimboNetworkHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class JoinQueueConnection {
    private static final List<JoinQueueConnection> ON_HOLD = new ArrayList<>();

    private final LimboNetworkHandler handler;
    private final CompletableFuture<Void> proceed;
    private final long connectedAtNanos;

    public JoinQueueConnection(LimboNetworkHandler handler) {
        this.handler = handler;
        this.proceed = new CompletableFuture<>();
        this.connectedAtNanos = System.nanoTime();

        ON_HOLD.add(this);

        for (JoinQueueConnection conn : ON_HOLD) {
            if (conn == this) continue;

            conn.sendQueueStatus();
        }

        handler.onDisconnect().register(reason -> {
            ON_HOLD.remove(this);
        });
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ON_HOLD.clear();
        });

        LimboEvent.JOINED.register(handler -> {
            if (handler.getServer().getCurrentPlayerCount() < handler.getServer().getMaxPlayerCount())
                return CompletableFuture.completedFuture(null);

            if (handler.getServer().getPlayerManager().canBypassPlayerLimit(handler.getPlayer().getGameProfile())) {
                return CompletableFuture.completedFuture(null);
            }

            var pending = new JoinQueueConnection(handler);

            pending.sendQueueStatus();

            return pending.proceed;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((disconnected, server) -> {
            boolean modified = false;

            while (server.getCurrentPlayerCount() - 1 < server.getMaxPlayerCount()
                && ON_HOLD.size() > 0) {
                var first = ON_HOLD.remove(0);

                first.proceed.complete(null);

                modified = true;
            }

            if (modified)
                ON_HOLD.forEach(JoinQueueConnection::sendQueueStatus);
        });
    }

    private void sendQueueStatus() {
        handler.sendPacket(new GameMessageS2CPacket(Text.translatable(
            "message.arstotzka.join_queue",
            Text.literal(String.valueOf(ON_HOLD.indexOf(this)))
                .formatted(Formatting.YELLOW),
            Text.literal(String.valueOf(ON_HOLD.size()))
                .formatted(Formatting.YELLOW)
        ), false));
    }
}
