package com.superzanti.serversync.files;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.PrettyCollection;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {
    public static final Path clientOnlyFilesDirectory = ServerSync.rootDir.resolve("clientmods");
    public static final Path logsDirectory = ServerSync.rootDir.resolve("logs");

    private FileManager() {
    }

    // New version of sync process

    public static Map<String, String> getDiffableFilesFromDirectory(String dir) throws IOException {
        return getDiffableFilesFromDirectories(Collections.singletonList(dir));
    }

    /**
     * Get all of the **files** present in the list of directories as a diffable map for file comparison.
     *
     * @param includedDirectories The list of directories to search
     * @return The files contained in the directories
     * @throws IOException when a configured directory is not a directory or does not exist.
     */
    public static Map<String, String> getDiffableFilesFromDirectories(List<String> includedDirectories) throws IOException {
        // Check for invalid directory configuration
        List<Path> dirs = new ArrayList<>();
        for (String includedDirectory : includedDirectories) {
            Path dir = ServerSync.rootDir.resolve(Paths.get(includedDirectory));
            if (!Files.exists(dir)) {
                Logger.debug(String.format("Configured directory: %s does not exist.", dir));
                throw new IOException("File does not exist");
            }

            if (!Files.isDirectory(dir)) {
                Logger.debug(String.format("Configured directory: %s is not a directory.", dir));
                throw new IOException("File is not a directory");
            }

            // Might as well build the list during this check
            dirs.add(dir);
        }

        List<Path> allFiles = dirs
            .parallelStream()
            .flatMap(dir -> {
                try {
                    return Files.walk(dir).filter(dirPath -> !Files.isDirectory(dirPath));
                } catch (IOException e) {
                    Logger.debug(String.format("Failed to access files in the directory: %s", dir));
                    Logger.debug(e);
                }
                return Stream.empty();
            })
            .map(ServerSync.rootDir::relativize)
            .collect(Collectors.toList());
        Logger.debug(String.format("All files: %s", PrettyCollection.get(allFiles)));

        List<Path> ignoredFiles = allFiles
            .parallelStream()
            .filter(IgnoredFilesMatcher::matches)
            .collect(Collectors.toList());
        Logger.debug(String.format("Ignored files: %s", PrettyCollection.get(ignoredFiles)));

        List<Path> filteredFiles = allFiles
            .parallelStream()
            .filter(f -> !IgnoredFilesMatcher.matches(f))
            .collect(Collectors.toList());
        Logger.debug(String.format("Filtered files: %s", PrettyCollection.get(filteredFiles)));

        return filteredFiles.stream()
                            .filter(path -> Files.exists(ServerSync.rootDir.resolve(path)))
                            .collect(
                                Collectors.toConcurrentMap(
                                    Path::toString,
                                    path -> FileHash.hashFile(ServerSync.rootDir.resolve(path))
                                ));
    }

    public static void removeEmptyDirectories(List<Path> directories, Consumer<Path> emptyDirectoryCallback) {
        directories.forEach(path -> {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc == null) {
                            try {
                                Files.delete(dir);

                                if (emptyDirectoryCallback != null) {
                                    emptyDirectoryCallback.accept(dir);
                                }
                            } catch (DirectoryNotEmptyException dne) {
                                // expected
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                Logger.debug(e);
            }
        });
    }
}
