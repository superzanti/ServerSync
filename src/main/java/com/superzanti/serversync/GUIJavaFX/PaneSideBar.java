package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

// GUI of the left panel to navigate in the application
public class PaneSideBar extends VBox {
    // SVG for each icons of the left panel
    final String svgPathSync = "M 1 0 h 22 v 23 H 1 M 1 23 L 23 23 L 23 0 L 1 0 z M19 8l-4 4h3c0 3.31-2.69 6-6 6-1.01 0-1.97-.25-2.8-.7l-1.46 1.46C8.97 19.54 10.43 20 12 20c4.42 0 8-3.58 8-8h3l-4-4zM6 12c0-3.31 2.69-6 6-6 1.01 0 1.97.25 2.8.7l1.46-1.46C15.03 4.46 13.57 4 12 4c-4.42 0-8 3.58-8 8H1l4 4 4-4H6z";
    final String svgPathLogs = "M 1 0 h 22 v 23 H 1 M 1 23 L 23 23 L 23 0 L 1 0 z M21 2H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h7v2H8v2h8v-2h-2v-2h7c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H3V4h18v12z";
    final String svgPathBuild = "M 1 0 h 22 v 23 H 1 M 1 23 L 23 23 L 23 0 L 1 0 z M 22.7 19 l -9.1 -9.1 c 0.9 -2.3 0.4 -5 -1.5 -6.9 c -2 -2 -5 -2.4 -7.4 -1.3 L 9 6 L 6 9 L 1.6 4.7 C 0.4 7.1 0.9 10.1 2.9 12.1 c 1.9 1.9 4.6 2.4 6.9 1.5 l 9.1 9.1 c 0.4 0.4 1 0.4 1.4 0 l 2.3 -2.3 c 0.5 -0.4 0.5 -1.1 0.1 -1.4 z";
    final String svgPathUpdate = "M 1 0 h 22 v 23 H 1 M 1 23 L 23 23 L 23 0 L 1 0 z M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z";
    final String svgPathNotUpdate = "M 1 0 h 22 v 23 H 1 M 1 23 L 23 23 L 23 0 L 1 0 z M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14zm-1-6h-3V8h-2v5H8l4 4 4-4z";
    final String svgPathOffline = "M 1 0 h 22 v 23 H 1 M 1 23 L 23 23 L 23 0 L 1 0 z M22,16v-2l-8.5-5V3.5C13.5,2.67,12.83,2,12,2s-1.5,0.67-1.5,1.5V9L2,14v2l8.5-2.5V19L8,20.5L8,22l4-1l4,1l0-1.5L13.5,19 v-5.5L22,16z";
    private Button btnUpdate;

    public PaneSideBar() {
        /* SYNC button */
        SVGPath svgSync = new SVGPath();
        svgSync.setContent(svgPathSync);
        svgSync.getStyleClass().add("sidebar-icon");
        svgSync.setScaleX(0.9);
        svgSync.setScaleY(0.9);

        Button btnSync = I18N.buttonForKey(("ui/sync"));
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

        Button btnLogs = I18N.buttonForKey(("ui/logs"));
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

        Button btnOptions = I18N.buttonForKey(("ui/options"));
        btnOptions.setPrefWidth(125);
        btnOptions.getStyleClass().add("sidebar-button");
        btnOptions.setGraphic(svgOptions);
        btnOptions.setOnAction(e -> {
            Gui_JavaFX.getStackMainPane().displayPanel(2);
            Gui_JavaFX.getStackMainPane().getPaneOptions().refreshConfigValues();
        });

        /* UPDATE button */
        SVGPath svgUpdate = new SVGPath();
        svgUpdate.setContent(svgPathUpdate);
        svgUpdate.getStyleClass().add("sidebar-icon");
        svgUpdate.setScaleX(0.9);
        svgUpdate.setScaleY(0.9);

        btnUpdate = I18N.buttonForKey(("ui/update"));
        btnUpdate.setPrefWidth(125);
        btnUpdate.getStyleClass().add("sidebar-button");
        btnUpdate.setGraphic(svgUpdate);
        btnUpdate.setOnAction(e -> {
            Gui_JavaFX.getStackMainPane().displayPanel(3);
            Gui_JavaFX.getStackMainPane().getPaneOptions().refreshConfigValues();
        });

        this.getStyleClass().add("sidebar-vbx");
        this.getChildren().addAll(btnSync, btnLogs, btnOptions, btnUpdate);
    }

    public void updateIconUpdate(String icon){
        SVGPath svgUpdate = new SVGPath();
        svgUpdate.getStyleClass().add("sidebar-icon");
        svgUpdate.setScaleX(0.9);
        svgUpdate.setScaleY(0.9);

        switch (icon){
            case "notUpdate":
                svgUpdate.setContent(svgPathNotUpdate);
                break;
            case "offline":
                svgUpdate.setContent(svgPathOffline);
                break;
            default: break;
        }


        btnUpdate.setGraphic(svgUpdate);
    }
}
