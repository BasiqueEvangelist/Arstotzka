package me.basiqueevangelist.arstotzka.logic;

import com.google.common.collect.MapMaker;
import io.wispforest.owo.Owo;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

public final class LimboLogic {
    private static final Map<String, LimboNetworkHandler> ON_HOLD = new MapMaker()
        .concurrencyLevel(1)
        .weakValues()
        .makeMap();

    private LimboLogic() {

    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ON_HOLD.clear();
        });
    }

    public static Map<String, LimboNetworkHandler> onHold() {
        return ON_HOLD;
    }

    public static void sendJoinNotification(ServerPlayerEntity player, LimboNetworkHandler handler) {
        player.sendMessage(Text.translatable(
            "message.arstotzka.new_connection",
            Text.literal(handler.getPlayer().getGameProfile().getName())
                .formatted(Formatting.AQUA),
            Text.literal(handler.connectionId())
                .formatted(Formatting.YELLOW),
            Text.empty()
                .append(Text.literal("✔")
                    .formatted(Formatting.GREEN)
                    .styled(x -> x
                        .withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(Text.translatable("message.arstotzka.new_connection.approve")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arstotzka approve " + handler.connectionId()))))
                .append(Text.literal(" | ")
                    .formatted(Formatting.DARK_GRAY))
                .append(Text.literal("✘")
                    .formatted(Formatting.DARK_RED)
                    .styled(x -> x
                        .withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(Text.translatable("message.arstotzka.new_connection.reject")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arstotzka reject " + handler.connectionId()))))
        ));
    }

    static void join(LimboNetworkHandler handler) {
        if (ON_HOLD.put(handler.connectionId(), handler) != null) {
            throw new IllegalStateException("ID collision!");
        }

        var server = Owo.currentServer();

        for (var target : server.getPlayerManager().getPlayerList()) {
            if (server.isHost(target.getGameProfile()) || Permissions.check(target, "arstotzka.notification", 3)) {
                sendJoinNotification(target, handler);
            }
        }
    }

    static void leave(LimboNetworkHandler handler) {
        ON_HOLD.remove(handler.connectionId());
    }
}
