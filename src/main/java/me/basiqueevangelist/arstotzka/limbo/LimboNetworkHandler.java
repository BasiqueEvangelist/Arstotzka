package me.basiqueevangelist.arstotzka.limbo;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import me.basiqueevangelist.arstotzka.Arstotzka;
import me.basiqueevangelist.arstotzka.util.Base62;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Mostly copied from https://github.com/Patbox/polymer/blob/dev/1.20/polymer-autohost/src/main/java/eu/pb4/polymer/autohost/impl/ResourcePackNetworkHandler.java#L24
// ;)
public class LimboNetworkHandler extends EarlyPlayNetworkHandler {
    private static final ArmorStandEntity FAKE_ENTITY = new ArmorStandEntity(EntityType.ARMOR_STAND, PolymerCommonUtils.getFakeWorld());
    private final String connectionId = Base62.random(8);
    private final Event<LimboEvent.Disconnect> disconnectEvent = EventFactory.createArrayBacked(LimboEvent.Disconnect.class, handlers -> reason -> {
        for (var handler : handlers) {
            handler.onDisconnect(reason);
        }
    });
    private final Event<LimboEvent.Tick> tickEvent = EventFactory.createArrayBacked(LimboEvent.Tick.class, () -> {}, handlers -> () -> {
        for (var handler : handlers) {
            handler.onTick();
        }
    });
    private CompletableFuture<Void> proceed;

    public LimboNetworkHandler(Context context) {
        super(Arstotzka.id("limbo"), context);

        var proceed = LimboEvent.JOINED.invoker().onJoined(this);

        if (proceed.isDone()) {
            proceed.join();

            continueJoining();
            return;
        }

        this.proceed = proceed;

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
    }

    @Override
    protected void onTick() {
        if (proceed != null && proceed.isDone()) {
            proceed.join();
            proceed = null;
        }

        if (proceed == null) {
            continueJoining();
            return;
        }

        tickEvent.invoker().onTick();
    }

    @Override
    public void handleDisconnect(Text reason) {
        onDisconnect().invoker().onDisconnect(reason);
    }

    public Event<LimboEvent.Disconnect> onDisconnect() {
        return disconnectEvent;
    }

    public Event<LimboEvent.Tick> onTickEvent() {
        return tickEvent;
    }

    public String connectionId() {
        return connectionId;
    }

}
