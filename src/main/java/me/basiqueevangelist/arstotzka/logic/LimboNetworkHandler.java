package me.basiqueevangelist.arstotzka.logic;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import me.basiqueevangelist.arstotzka.Arstotzka;
import me.basiqueevangelist.arstotzka.config.ArstotzkaConfigModel;
import me.basiqueevangelist.arstotzka.util.Base62;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.TimeUnit;

// Mostly copied from https://github.com/Patbox/polymer/blob/dev/1.20/polymer-autohost/src/main/java/eu/pb4/polymer/autohost/impl/ResourcePackNetworkHandler.java#L24
// ;)
public class LimboNetworkHandler extends EarlyPlayNetworkHandler {
    private static final ArmorStandEntity FAKE_ENTITY = new ArmorStandEntity(EntityType.ARMOR_STAND, PolymerCommonUtils.getFakeWorld());
    private final String connectionId = Base62.random(8);
    private final long connectedAtNanos;

    public LimboNetworkHandler(Context context) {
        super(Arstotzka.id("limbo"), context);
        this.connectedAtNanos = System.nanoTime();

        var player = getPlayer();

        if (context.server().isHost(player.getGameProfile())) {
            // The host is always let in.
            continueJoining();
            return;
        }

        if (Arstotzka.CONFIG.mode() != ArstotzkaConfigModel.Mode.HOLD) {
            // Bans and whitelists will be checked later.
            continueJoining();
            return;
        }
        
        LimboLogic.join(this);

        sendInitialGameJoin();
        sendPacket(FAKE_ENTITY.createSpawnPacket());
        sendPacket(new EntityTrackerUpdateS2CPacket(FAKE_ENTITY.getId(), FAKE_ENTITY.getDataTracker().getChangedEntries()));
        sendPacket(new SetCameraEntityS2CPacket(FAKE_ENTITY));

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("arstotzka:limbo");
            sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, buf));
        }

        sendPacket(new WorldTimeUpdateS2CPacket(0, 0, false));
        sendPacket(new CloseScreenS2CPacket(0));

        sendPacket(new GameMessageS2CPacket(Text.translatable(
            "message.arstotzka.welcome_to_limbo.1",
            Text.literal(player.getGameProfile().getName())
                .formatted(Formatting.AQUA),
            Text.literal(connectionId)
                .styled(x -> x
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.copy.click")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, connectionId)))
                .formatted(Formatting.YELLOW)
        ), false));
        sendPacket(new GameMessageS2CPacket(Text.translatable("message.arstotzka.welcome_to_limbo.2"), false));
    }

    @Override
    protected void onTick() {
        long now = System.nanoTime();

        if ((now - connectedAtNanos) >= TimeUnit.SECONDS.toNanos(30)) {
            disconnect(Text.translatable("message.arstotzka.join_timeout"));
            LimboLogic.leave(this);
        }
    }

    public void approve() {
        sendPacket(new GameMessageS2CPacket(Text.translatable("message.arstotzka.approved.1"), false));
        sendPacket(new GameMessageS2CPacket(Text.translatable("message.arstotzka.approved.2"), false));

        continueJoining();

        LimboLogic.leave(this);
    }

    public void reject() {
        disconnect(Text.translatable("message.arstotzka.rejected"));

        LimboLogic.leave(this);
    }

    @Override
    public void handleDisconnect(Text reason) {
        LimboLogic.leave(this);
    }

    public String connectionId() {
        return connectionId;
    }
}
