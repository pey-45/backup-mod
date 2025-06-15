package com.pey.backupmod.backupMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class BackupCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("backup")
                .executes(context -> {
                    CompletableFuture.runAsync(() -> {
                        try {
                            BackupHandler.createBackup(context.getSource());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    return 1;
                })
                .then(CommandManager.literal("list")
                        .executes(context -> {
                            BackupHandler.listBackups(context.getSource());
                            return 1;
                        })
                )
                .then(CommandManager.literal("clean")
                        .then(CommandManager.argument("backups to keep", IntegerArgumentType.integer(5))
                                .executes(context -> {
                                    CompletableFuture.runAsync(() -> {
                                        try {
                                            BackupHandler.createBackup(context.getSource());
                                            BackupHandler.deleteOldestBackups(context.getSource(), IntegerArgumentType.getInteger(context, "backups to keep"));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    BackupHandler.createBackup(context.getSource());
                                    BackupHandler.deleteOldestBackups(context.getSource(), null);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            return 1;
                        })
                )
                .then(CommandManager.literal("onlyclean")
                        .then(CommandManager.argument("backups to keep", IntegerArgumentType.integer(5))
                                .executes(context -> {
                                    CompletableFuture.runAsync(() -> {
                                        try {
                                            BackupHandler.deleteOldestBackups(context.getSource(), IntegerArgumentType.getInteger(context, "backups to keep"));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    BackupHandler.deleteOldestBackups(context.getSource(), null);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            return 1;
                        })
                )
        );
    }
}