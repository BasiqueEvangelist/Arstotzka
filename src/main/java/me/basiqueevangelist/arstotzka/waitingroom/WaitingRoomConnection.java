package me.basiqueevangelist.arstotzka.waitingroom;

import com.google.common.collect.MapMaker;
import io.wispforest.owo.Owo;
import me.basiqueevangelist.arstotzka.Arstotzka;
import me.basiqueevangelist.arstotzka.limbo.LimboEvent;
import me.basiqueevangelist.arstotzka.limbo.LimboNetworkHandler;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class WaitingRoomConnection {
    private static final Map<String, WaitingRoomConnection> ON_HOLD = new MapMaker()
        .concurrencyLevel(1)
        .weakValues()
        .makeMap();

    private final LimboNetworkHandler handler;
    private final CompletableFuture<Void> proceed;
    private final long connectedAtNanos;

    public WaitingRoomConnection(LimboNetworkHandler handler) {
        this.handler = handler;
        this.proceed = new CompletableFuture<>();
        this.connectedAtNanos = System.nanoTime();

        if (ON_HOLD.put(handler.connectionId(), this) != null) {
            throw new IllegalStateException("ID collision!");
        }

        handler.onTickEvent().register(() -> {
            long now = System.nanoTime();

            if ((now - connectedAtNanos) >= TimeUnit.SECONDS.toNanos(30)) {
                handler.disconnect(Text.translatable("message.arstotzka.join_timeout"));
                ON_HOLD.remove(handler.connectionId());
            }
        });

        handler.onDisconnect().register(reason -> {
            ON_HOLD.remove(handler.connectionId());
        });
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ON_HOLD.clear();
        });

        LimboEvent.JOINED.register(handler -> {
            if (handler.getServer().isHost(handler.getPlayer().getGameProfile())) {
                // The host is always let in.
                return CompletableFuture.completedFuture(null);
            }

            if (!Arstotzka.CONFIG.waitingRoom()) {
                // Bans and whitelists will be checked later.
                return CompletableFuture.completedFuture(null);
            }

            handler.sendPacket(new GameMessageS2CPacket(Text.translatable(
                "message.arstotzka.waiting_room.1",
                Text.literal(handler.getPlayer().getGameProfile().getName())
                    .formatted(Formatting.AQUA),
                Text.literal(handler.connectionId())
                    .styled(x -> x
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.copy.click")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, handler.connectionId())))
                    .formatted(Formatting.YELLOW)
            ), false));
            handler.sendPacket(new GameMessageS2CPacket(Text.translatable("message.arstotzka.waiting_room.2"), false));

            var pending = new WaitingRoomConnection(handler);

            pending.notifySubscribers();

            return pending.proceed;
        });
    }

    public static Map<String, WaitingRoomConnection> onHold() {
        return ON_HOLD;
    }

    public void approve() {
        handler.sendPacket(new GameMessageS2CPacket(Text.translatable("message.arstotzka.approved.1"), false));
        handler.sendPacket(new GameMessageS2CPacket(Text.translatable("message.arstotzka.approved.2"), false));

        proceed.complete(null);

        ON_HOLD.remove(handler.connectionId());
    }

    public void reject() {
        handler.disconnect(Text.translatable("message.arstotzka.rejected"));

        ON_HOLD.remove(handler.connectionId());
    }

    void notifySubscribers() {
        var server = Owo.currentServer();

        sendJoinNotification(server, this);

        for (var target : server.getPlayerManager().getPlayerList()) {
            if (server.isHost(target.getGameProfile()) || Permissions.check(target, "arstotzka.notification", 3)) {
                sendJoinNotification(target, this);
            }
        }
    }

    public static void sendJoinNotification(CommandOutput out, WaitingRoomConnection conn) {
        out.sendMessage(Text.translatable(
            "message.arstotzka.new_connection",
            Text.literal(conn.handler.getPlayer().getGameProfile().getName())
                .formatted(Formatting.AQUA),
            Text.literal(conn.handler.connectionId())
                .formatted(Formatting.YELLOW),
            Text.empty()
                .append(Text.literal("✔")
                    .formatted(Formatting.GREEN)
                    .styled(x -> x
                        .withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(Text.translatable("message.arstotzka.new_connection.approve")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arstotzka approve " + conn.handler.connectionId()))))
                .append(Text.literal(" | ")
                    .formatted(Formatting.DARK_GRAY))
                .append(Text.literal("✘")
                    .formatted(Formatting.DARK_RED)
                    .styled(x -> x
                        .withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(Text.translatable("message.arstotzka.new_connection.reject")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arstotzka reject " + conn.handler.connectionId()))))
        ));
    }

    public LimboNetworkHandler handler() {
        return handler;
    }
}
