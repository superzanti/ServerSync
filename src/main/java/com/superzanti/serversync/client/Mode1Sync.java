package com.superzanti.serversync.client;

public class Mode1Sync implements Runnable {
    private final DialogServer server;

    private Mode1Sync(DialogServer server) {
        this.server = server;
    }

    public static Mode1Sync forServer(Server server) {
        return new Mode1Sync(new DialogServer(server));
    }

    @Override
    public void run() {
//        List<String> managedDirectories = getServerManagedDirectories();
//
//        Logger.log(String.format("Building file list for directories: %s", managedDirectories));
//        // Create dirs on the client that don't exist yet
//        managedDirectories.forEach(path -> {
//            try {
//                Files.createDirectories(Paths.get(path));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//
//        // Attempt to sync files with max number of retries, note that some files may fail here
//        // the user should be notified that they may have to manually deal with them
//        // TODO make retries user configurable?
//        int maxUpdateRetries = 2;
//        boolean updateSuccess = false;
//        Map<String, EFileProccessingStatus> updatedFiles = new HashMap<>();
//        for (int i = 0; i < maxUpdateRetries; i++) {
//            updatedFiles = updateFiles();
//
//            if (updatedFiles.containsValue(EFileProccessingStatus.FAILED)) {
//                Logger.log(String.format(
//                    "%s %s",
//                    RefStrings.ERROR_TOKEN,
//                    ServerSync.strings.getString("message_file_failed_to_sync")
//                ));
//
//                if (i < maxUpdateRetries - 1) {
//                    Logger.log(ServerSync.strings.getString("message_attempting_sync_retry"));
//                }
//                continue;
//            }
//
//            updateSuccess = true;
//            break;
//        }
//
//        // Move on to delete phase, note that some files may fail here
//        // the user should be notified that they may have to manually deal with them
//        // TODO make retries user configurable?
//        int maxDeleteRetries = 2;
//        boolean deleteSuccess = false;
//        Map<String, EFileProccessingStatus> deletedFiles = new HashMap<>();
//
//        for (int i = 0; i < maxDeleteRetries; i++) {
//            deletedFiles = deleteFiles(managedDirectories, updatedFiles);
//
//            if (deletedFiles.containsValue(EFileProccessingStatus.FAILED)) {
//                Logger.log(String.format(
//                    "%s %s",
//                    RefStrings.ERROR_TOKEN,
//                    ServerSync.strings.getString("message_file_failed_to_delete")
//                ));
//
//                if (i < maxDeleteRetries - 1) {
//                    Logger.log(ServerSync.strings.getString("message_attempting_delete_retry"));
//                }
//                continue;
//            }
//
//            deleteSuccess = true;
//            break;
//        }
//
//        // Cleanup phase, things like empty directories or duplicate files should be handled here.
//        FileManager.removeEmptyDirectories(
//            managedDirectories.stream().map(Paths::get).collect(Collectors.toList()),
//            (dir) -> Logger.log(String.format(
//                "%s Removed empty directory: %s",
//                RefStrings.CLEANUP_TOKEN,
//                dir.toString()
//            ))
//        );
//
//        // Catch update or delete errors and notify the user that they may have to manually intervene.
//        if (!updateSuccess) {
//            Logger.debug("Update failure, max retries exceeded");
//            List<String> fileNames = updatedFiles.entrySet()
//                                                 .parallelStream()
//                                                 .filter(e -> e.getValue().equals(EFileProccessingStatus.FAILED))
//                                                 .map(Map.Entry::getKey)
//                                                 .collect(Collectors.toList());
//            Logger.log(String.format(
//                "%s %s",
//                RefStrings.ERROR_TOKEN,
//                ServerSync.strings.getString("message_file_failed_to_sync")
//            ));
//            Logger.log(String.format(
//                "%s %s",
//                RefStrings.ERROR_TOKEN,
//                ServerSync.strings.getString("message_manual_action_required")
//            ));
//            Logger.log(fileNames.toString());
//        }
//
//        if (!deleteSuccess) {
//            Logger.debug("Delete failure, max retries exceeded");
//            List<String> fileNames = deletedFiles.entrySet()
//                                                 .parallelStream()
//                                                 .filter(e -> e.getValue().equals(EFileProccessingStatus.FAILED))
//                                                 .map(Map.Entry::getKey)
//                                                 .collect(Collectors.toList());
//            Logger.log(String.format(
//                "%s %s",
//                RefStrings.ERROR_TOKEN,
//                ServerSync.strings.getString("message_file_failed_to_delete")
//            ));
//            Logger.log(String.format(
//                "%s %s",
//                RefStrings.ERROR_TOKEN,
//                ServerSync.strings.getString("message_manual_action_required")
//            ));
//            Logger.log(fileNames.toString());
//        }
    }
}
