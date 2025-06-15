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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupHandler {

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
            }
        });
        disableAutoSave.get();

        CompletableFuture<Void> saveFuture = new CompletableFuture<>();
        server.execute(() -> {
            try {
                server.getCommandManager().executeWithPrefix(silentSource, "save-all");
            } finally {
                saveFuture.complete(null);

                broadcast(server, Text.literal("Backup started").formatted(Formatting.GREEN));
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

        int totalFiles;
        try (var stream = Files.walk(worldPath)) {
            totalFiles = (int) stream.filter(Files::isRegularFile).count();
        }
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger lastPercentShown = new AtomicInteger(0);

        sendProgress(server, 0);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            try (var paths = Files.walk(worldPath)) {
                paths.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        if (!file.getFileName().toString().endsWith(".lock")) {
                            ZipEntry entry = new ZipEntry(worldPath.relativize(file).toString());
                            zos.putNextEntry(entry);
                            Files.copy(file, zos);
                            zos.closeEntry();
                        }

                        int current = processed.incrementAndGet();
                        int percent = (int) ((current / (double) totalFiles) * 100);

                        if (percent % 20 == 0 && percent != lastPercentShown.get()) {
                            lastPercentShown.set(percent);
                            sendProgress(server, percent);
                        }

                    } catch (IOException e) {
                        System.err.println("Error copying " + file + ": " + e.getMessage());
                    }
                });
            }

        }

        server.execute(() -> {
            server.getCommandManager().executeWithPrefix(silentSource, "save-on");
            broadcast(server, Text.literal("Backup created successfully!").formatted(Formatting.GREEN));
        });
    }

    public static void listBackups(ServerCommandSource source) {
        Path backupDir = Paths.get("backups");
        try {
            if (Files.exists(backupDir)) {
                List<Path> backups;
                try (var stream = Files.list(backupDir)) {
                    backups = stream
                            .filter(path -> path.toString().endsWith(".zip"))
                            .sorted()
                            .toList();
                }


                source.sendMessage(Text.literal(backups.size() + " backup(s) found:"));

                for (Path path : backups) {
                    String filename = path.getFileName().toString();
                    String trimmed = filename.replaceFirst("[.][^.]+$", "").replaceFirst("backup_", "");

                    source.sendMessage(Text.literal("- " + trimmed));
                }
            } else {
                source.sendMessage(Text.literal("No backups found").formatted(Formatting.RED));
            }
        } catch (IOException e) {
            source.sendMessage(Text.literal("Error reading backup directory: " + e.getMessage())
                    .styled(style -> style.withColor(Formatting.RED)));
        }
    }


    public static void deleteOldestBackups(ServerCommandSource source, Integer amountToKeep) {
        Path backupDir = Paths.get("backups");

        try {
            if (!Files.exists(backupDir)) {
                source.sendMessage(Text.literal("Backup folder does not exist")
                        .styled(style -> style.withColor(Formatting.RED)));
                return;
            }

            List<Path> backups;
            try (var stream = Files.list(backupDir)) {
                backups = stream
                        .filter(path -> path.toString().endsWith(".zip"))
                        .sorted((a, b) -> {
                            try {
                                return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .toList();
            }

            amountToKeep = (amountToKeep == null) ? 5 : amountToKeep;


            if (amountToKeep < 5) {
                source.sendMessage(Text.literal("At least 5 backups must remain")
                        .styled(style -> style.withColor(Formatting.RED)));
                return;
            }

            int toDelete = backups.size() - amountToKeep;

            for (int i = 0; i < toDelete; i++) {
                Files.deleteIfExists(backups.get(i));
            }

            MinecraftServer server = source.getServer();
            server.execute(() -> broadcast(server, Text.literal("Deleted " + (Math.max(toDelete, 0)) + " oldest backup(s)")));


        } catch (IOException e) {
            source.sendMessage(Text.literal("Error deleting backups: " + e.getMessage())
                    .styled(style -> style.withColor(Formatting.RED)));
        }
    }

    private static void broadcast(MinecraftServer server, MutableText text) {
        server.getPlayerManager().broadcast(text, false);
    }

    private static void sendProgress(MinecraftServer server, int percent) {
        broadcast(server, Text.literal("Backup in progress... " + percent + "%"));
    }
}
