package com.pey.backupmod.backupMod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupHandler {

    private static void broadcast(MinecraftServer server, MutableText text) {
        server.getPlayerManager().broadcast(text, false);
    }

    public static void createBackup(ServerCommandSource source) throws IOException, InterruptedException, ExecutionException {

        ServerCommandSource silentSource = source.withSilent();
        MinecraftServer server = silentSource.getServer();
        Path worldPath = Paths.get("world");

        CompletableFuture<Void> disableAutoSave = new CompletableFuture<>();
        server.execute(() -> {
            try {
                server.getCommandManager().executeWithPrefix(silentSource, "save-off");
            } finally {
                disableAutoSave.complete(null);
                broadcast(server, Text.literal("[Server: Automatic saving is now disabled]")
                        .styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
            }
        });
        disableAutoSave.get();

        CompletableFuture<Void> saveFuture = new CompletableFuture<>();
        server.execute(() -> {
            try {
                server.getCommandManager().executeWithPrefix(silentSource, "save-all");
            } finally {
                saveFuture.complete(null);
                broadcast(server, Text.literal("[Server: Saved the game]")
                        .styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
            }
        });
        saveFuture.get();

        // Crear carpeta de backups
        Path backupPath = Paths.get("backups");
        Files.createDirectories(backupPath);

        // Nombre del backup con fecha
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        String formattedDate = now.format(formatter);
        String backupName = "backup_" + formattedDate + ".zip";
        Path zipFile = backupPath.resolve(backupName);

        broadcast(server, Text.literal("Creating backup..."));

        // Comprimir carpeta del mundo
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(worldPath).filter(Files::isRegularFile).forEach(file -> {
                try {
                    if (!file.getFileName().toString().endsWith(".lock")) {
                        ZipEntry entry = new ZipEntry(worldPath.relativize(file).toString());
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    System.err.println("Error al copiar " + file + ": " + e.getMessage());
                }
            });
        }

        broadcast(server, Text.literal("Backup successfully created!").formatted(Formatting.GREEN));

        // Reactivar guardado automático
        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(silentSource, "save-on");
            broadcast(server, Text.literal("[Server: Automatic saving is now enabled]")
                    .styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
        });
    }
}
