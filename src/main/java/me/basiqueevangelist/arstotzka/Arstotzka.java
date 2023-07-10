package me.basiqueevangelist.arstotzka;

import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import me.basiqueevangelist.arstotzka.command.ApproveRejectCommand;
import me.basiqueevangelist.arstotzka.config.ArstotzkaConfig;
import me.basiqueevangelist.arstotzka.joinqueue.JoinQueueConnection;
import me.basiqueevangelist.arstotzka.limbo.LimboNetworkHandler;
import me.basiqueevangelist.arstotzka.waitingroom.WaitingRoomConnection;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Arstotzka implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Arstotzka");
	public static final ArstotzkaConfig CONFIG = ArstotzkaConfig.createAndLoad();

	public static Identifier id(String path) {
		return new Identifier("arstotzka", path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Glory to Arstotzka!");

		WaitingRoomConnection.init();
		JoinQueueConnection.init();
		EarlyPlayNetworkHandler.register(LimboNetworkHandler::new);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ApproveRejectCommand.register(dispatcher);
		});
	}
}