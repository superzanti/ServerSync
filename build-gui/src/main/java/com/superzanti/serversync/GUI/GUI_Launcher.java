package com.superzanti.serversync.GUI;

import javafx.application.Application;

public class GUI_Launcher extends Thread {
    @Override
    public void run() {
        super.run();
        Application.launch(Gui_JavaFX.class);
    }
}
