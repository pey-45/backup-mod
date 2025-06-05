package com.pey.backupmod.backupMod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class BackupCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("backup")
                        // .requires(source -> source.hasPermissionLevel(4)) // solo operadores
                        .executes(context -> {
                            new Thread(() -> {
                                try {
                                    BackupHandler.createBackup(context.getSource());
                                } catch (Exception e) {
                                    context.getSource().sendMessage(Text.literal("Error al hacer backup: " + e.getMessage()).styled(style -> style
                                            .withColor(Formatting.RED)
                                    ));
                                    e.printStackTrace();
                                }
                            }).start();
                            return 1;
                        })
        );
    }
}