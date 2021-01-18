package com.superzanti.serversync.GUIJavaFX;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.util.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;

public class PaneUpdate extends GridPane {
    private String curVersion = '"'+RefStrings.VERSION+'"';
    private Label labelCurVersion = I18N.labelForValue(() -> I18N.get("ui/current_version"));
    private Label labelCurVersion2 = new Label('"'+RefStrings.VERSION+'"');
    private Label labelVersion = new Label("Latest version: ");
    private Label labelVersion2 = new Label("?");
    private Label labelUrl = new Label("URL: ");
    private Hyperlink update_url = new Hyperlink("?");

    public PaneUpdate() {
        update_url.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                URI u = null;
                try {

                    u = new URI(update_url.getText());
                    Desktop.getDesktop().browse(u);
                    update_url.setVisited(false);
                } catch (URISyntaxException | IOException e) {
                    e.printStackTrace();
                }
            }

        });

        this.setAlignment(Pos.CENTER);
        this.setHgap(10);
        this.setVgap(10);
        this.setPadding(new Insets(10, 10, 10, 10));

        this.add(labelCurVersion,0,0);
        this.add(labelCurVersion2,1,0);
        this.add(labelVersion,0,1);
        this.add(labelVersion2,1,1);
        this.add(labelUrl,0,2);
        this.add(update_url,1,2);

        Platform.runLater(this::getLastReleases);
    }

    public void getLastReleases() {
        //String url = "https://github.com/superzanti/ServerSync/releases/latest";

        // Create a neat value object to hold the URL
        try {
            URL url = new URL("https://github.com/superzanti/ServerSync/releases/latest");

            // Open a connection(?) on the URL(??) and cast the response(???)
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Now it's "open", we can set the request method, headers etc.
            connection.setRequestProperty("accept", "application/json");

            // This line makes the request
            InputStream responseStream = connection.getInputStream();
            BufferedReader bR = new BufferedReader(  new InputStreamReader(responseStream));
            String line = "";

            StringBuilder responseStrBuilder = new StringBuilder();
            while((line =  bR.readLine()) != null){

                responseStrBuilder.append(line);
            }
            responseStream.close();


            JsonObject release = Json.parse(responseStrBuilder.toString()).asObject();
            this.labelVersion2.setText(release.get("tag_name").toString());
            String releaseVersion = release.get("update_url").toString();
            String urlRelease = "https://github.com" + releaseVersion.substring(1, releaseVersion.length() - 1);
            this.update_url.setText(urlRelease);
            System.out.println(responseStrBuilder.toString());
            if(!this.curVersion.equals(releaseVersion)){
                Gui_JavaFX.getStackMainPane().getPaneSideBar().updateBtnUpdate();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Can't check last version from Github");
            Logger.debug("Can't check last version from Github");
        }
    }
}
