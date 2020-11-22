package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

// GUI of the left panel to navigate in the application
public class PaneSideBar extends VBox {
    // SVG for each icons of the left panel
    String svgPathSync = "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z";
    String svgPathLogs = "M21 2H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h7v2H8v2h8v-2h-2v-2h7c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H3V4h18v12z";
    String svgPathBuild = "M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z";

    public PaneSideBar() {
        /* SYNC button */
        SVGPath svgSync = new SVGPath();
        svgSync.setContent(svgPathSync);
        svgSync.getStyleClass().add("sidebar-icon");
        svgSync.setScaleX(1.25);
        svgSync.setScaleY(1.25);

        Button btnSync = new Button(" Sync");
        btnSync.setPrefWidth(125);
        btnSync.getStyleClass().add("sidebar-button");
        btnSync.setGraphic(svgSync);
        btnSync.setOnAction(e -> Gui_JavaFX.getStackMainPane().displayPanel(0));

        /* LOGS button */
        SVGPath svgLogs = new SVGPath();
        svgLogs.setContent(svgPathLogs);
        svgLogs.getStyleClass().add("sidebar-icon");
        svgLogs.setScaleX(0.9);
        svgLogs.setScaleY(0.9);

        Button btnLogs = new Button("Logs");
        btnLogs.setPrefWidth(125);
        btnLogs.getStyleClass().add("sidebar-button");
        btnLogs.setGraphic(svgLogs);
        btnLogs.setOnAction(e -> Gui_JavaFX.getStackMainPane().displayPanel(1));

        /* OPTIONS button */
        SVGPath svgOptions = new SVGPath();
        svgOptions.setContent(svgPathBuild);
        svgOptions.getStyleClass().add("sidebar-icon");
        svgOptions.setScaleX(0.9);
        svgOptions.setScaleY(0.9);

        Button btnOptions = new Button("Options");
        btnOptions.setPrefWidth(125);
        btnOptions.getStyleClass().add("sidebar-button");
        btnOptions.setGraphic(svgOptions);
        btnOptions.setOnAction(e -> {
            Gui_JavaFX.getStackMainPane().displayPanel(2);
            Gui_JavaFX.getStackMainPane().getPaneOptions().refreshConfigValues();
        });

        this.getStyleClass().add("sidebar-vbx");
        this.getChildren().addAll(btnSync, btnLogs, btnOptions);
    }
}
