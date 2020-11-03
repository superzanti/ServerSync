package com.superzanti.serversync.client;

import com.superzanti.serversync.GUIJavaFX.Gui_JavaFX;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.communication.response.ServerInfo;
import com.superzanti.serversync.util.Logger;

/**
 * The sync process for clients.
 * - Get my state
 * - Stream server state and pop files from my state that are dealt with
 * - Delete files that are not present on the server (remaining)
 * <p>
 * Caveats:
 * - Client can configure to ignore files from deletion (e.g. Optifine, NEET and other such client side mods)
 * <p>
 *
 * @author Rheimus
 */
public class ClientWorker implements Runnable {

    private boolean errorInUpdates = false;
    private boolean updateHappened = false;

    private Server server;

    private final SyncConfig config = SyncConfig.getConfig();

    @Override
    public void run() {
        updateHappened = false;

        Logger.getLog().clearUserFacingLog();

        Client client = new Client(config.SERVER_IP, config.SERVER_PORT);
        server = Server.forClient(client);

        if (!server.connect()) {
            errorInUpdates = true;
            closeWorker();
            return;
        }

        ServerInfo serverInfo = server.info;

        switch (config.SYNC_MODE) {
            case 1:
                Mode1Sync.forServer(server).run();
                break;
            case 2:
                Mode2Sync.forServer(server).run();
                break;
            case 3:
                Mode3Sync.forServer(server).run();
                break;
            default:
                Logger.error(String.format("Unknown server mode: %d", serverInfo.syncMode));
                errorInUpdates = true;
                updateHappened = false;
                closeWorker();
                return;
        }


        updateHappened = true;
        closeWorker();

        // Update configured server to the latest used address
        // consideration to be had here for client silent sync mode
        //TODO fix or delete this
        if (ServerSync.clientGUI != null) {
            config.updateServerDetails(ServerSync.clientGUI.getIPAddress(), ServerSync.clientGUI.getPort());
        }

        Logger.log(ServerSync.strings.getString("update_complete"));
    }

    private void closeWorker() {
        Logger.debug("Closing client worker");
        if (server == null) {
            return;
        }
        server.close();

        if (!updateHappened && !errorInUpdates) {
            Logger.log(ServerSync.strings.getString("update_not_needed"));
        } else {
            Logger.debug(ServerSync.strings.getString("update_happened"));
        }

        if (errorInUpdates) {
            Logger.error(ServerSync.strings.getString("update_error"));
        }

        enableGuiButton();
    }

    private void enableGuiButton() {
        Gui_JavaFX.getStackMainPane().getPaneSync().getBtnSync().setDisable(false);
        Gui_JavaFX.getStackMainPane().getPaneSync().getBtnCheckUpdate().setDisable(false);
    }
}
