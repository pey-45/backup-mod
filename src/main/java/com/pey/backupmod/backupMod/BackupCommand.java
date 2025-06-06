package com.pey.backupmod.backupMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class BackupCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("backup")
                        .executes(context -> {
                            new Thread(() -> {
                                try {
                                    BackupHandler.createBackup(context.getSource());
                                } catch (IOException | ExecutionException | InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                            return 1;
                        })
                        .then(CommandManager.literal("count")
                                .executes(context -> BackupHandler.getBackupCount(context.getSource()))
                        )
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> BackupHandler.deleteOldestBackups(context.getSource(), IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
        );
    }
}