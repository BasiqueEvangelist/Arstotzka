package me.basiqueevangelist.arstotzka.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.arstotzka.waitingroom.WaitingRoomConnection;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ApproveRejectCommand {
    private static final SimpleCommandExceptionType NO_SUCH_CONNECTION
        = new SimpleCommandExceptionType(Text.translatable("commands.arstotzka.no_such_connection"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("arstotzka")
                .then(literal("approve")
                    .requires(Permissions.require("arstotzka.approve", 3))
                    .then(argument("connection", StringArgumentType.word())
                        .executes(ApproveRejectCommand::approve)))
                .then(literal("reject")
                    .requires(Permissions.require("arstotzka.reject", 3))
                    .then(argument("connection", StringArgumentType.word())
                        .executes(ApproveRejectCommand::reject)))
        );
    }

    private static int approve(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String connectionId = StringArgumentType.getString(ctx, "connection");
        WaitingRoomConnection connection = WaitingRoomConnection.onHold().get(connectionId);

        if (connection == null)
            throw NO_SUCH_CONNECTION.create();

        connection.approve();

        src.sendFeedback(() -> Text.translatable(
            "commands.arstotzka.approve",
            Text.literal(connection.handler().getPlayer().getGameProfile().getName())
                .formatted(Formatting.AQUA),
            Text.literal(connection.handler().connectionId())
                .formatted(Formatting.YELLOW)
        ), true);

        return 1;
    }

    private static int reject(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String connectionId = StringArgumentType.getString(ctx, "connection");
        WaitingRoomConnection connection = WaitingRoomConnection.onHold().get(connectionId);

        if (connection == null)
            throw NO_SUCH_CONNECTION.create();

        connection.reject();

        src.sendFeedback(() -> Text.translatable(
            "commands.arstotzka.reject",
            Text.literal(connection.handler().getPlayer().getGameProfile().getName())
                .formatted(Formatting.AQUA),
            Text.literal(connection.handler().connectionId())
                .formatted(Formatting.YELLOW)
        ), true);

        return 1;
    }
}
