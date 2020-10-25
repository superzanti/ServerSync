package com.superzanti.serversync.GUIJavaFX;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class StackMainMenu extends StackPane{

    public StackMainMenu() {
        PaneMainMenu menu = new PaneMainMenu();
        PaneOptions options = new PaneOptions();
        this.getChildren().addAll(menu, options);
        changeTop();changeTop();
    }
    private void changeTop() {
        ObservableList<Node> childs = getChildren();

        if (childs.size() > 1) {
            //
            Node topNode = childs.get(childs.size()-1);

            // This node will be brought to the front
            Node newTopNode = childs.get(childs.size()-2);

            topNode.setVisible(false);
            topNode.toBack();

            newTopNode.setVisible(true);
        }
    }
}