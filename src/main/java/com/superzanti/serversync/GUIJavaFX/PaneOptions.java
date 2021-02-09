package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.Zipper;
import com.superzanti.serversync.util.enums.ETheme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// GUI of options
public class PaneOptions extends GridPane {

    private TextField fieldAddress;
    private TextField fieldPort;
    private CheckBox cbxRefuse ;
    private ComboBox<String> comboBoxTheme;
    private ComboBox<String> comboBoxLanguage;

    public PaneOptions() {
        this.setAlignment(Pos.CENTER);
        this.setHgap(10);
        this.setVgap(10);
        this.setPadding(new Insets(10, 10, 10, 10));

        /* CLIENT */
        /* CLIENT -> Themes*/
        Label labelClient = I18N.labelForValue(() -> I18N.get("ui/client_configuration"));
        labelClient.setFont(new Font("Arial", 16));
        setRowIndex(labelClient, 0);
        setColumnIndex(labelClient, 0);

        Label labelTheme = I18N.labelForValue(() -> I18N.get("ui/theme"));
        setRowIndex(labelTheme, 1);
        setColumnIndex(labelTheme, 0);

        setRowIndex(getComboBoxTheme(), 1);
        setColumnIndex(getComboBoxTheme(), 1);

        this.getChildren().addAll(labelTheme, getComboBoxTheme());

        Label labelLanguage = I18N.labelForValue(() -> I18N.get("ui/language"));
        setRowIndex(labelLanguage, 2);
        setColumnIndex(labelLanguage, 0);

        setRowIndex(getComboBoxLanguage(), 2);
        setColumnIndex(getComboBoxLanguage(), 1);
        this.getChildren().addAll(labelLanguage, getComboBoxLanguage());

        /* CLIENT -> IP*/
        Label labelIp = I18N.labelForValue(() -> I18N.get("ui/server_address"));
        setRowIndex(labelIp, 3);
        setColumnIndex(labelIp, 0);

        setRowIndex(getAddressField(), 3);
        setColumnIndex(getAddressField(), 1);

        /* CLIENT -> PORT*/
        Label labelPort = I18N.labelForValue(() -> I18N.get("ui/server_port"));
        setRowIndex(labelPort, 4);
        setColumnIndex(labelPort, 0);

        setRowIndex(getPortField(), 4);
        setColumnIndex(getPortField(), 1);

        /* CLIENT -> Refuse client mods*/
        Label labelRefuse = I18N.labelForValue(() -> I18N.get("ui/refuse_client_mods"));
        setRowIndex(labelRefuse, 5);
        setColumnIndex(labelRefuse, 0);

        setRowIndex(getRefuseClientModsCheckbox(), 5);
        setColumnIndex(getRefuseClientModsCheckbox(), 1);

        /*Backup button */
        Label labelBackup = I18N.labelForValue(() -> I18N.get("ui/backup"));
        Button btnBackup = new Button("Backup");
        btnBackup.getStyleClass().add("btn");
        btnBackup.setOnAction(e -> {
            Thread thread = new Thread(){
                public void run(){
                    try {
                        Date date = new Date();
                        Zipper.zipDirectory("mods", date);
                        Zipper.zipDirectory("config", date);

                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            };

            thread.start();
        });
        this.add(labelBackup,0,6);
        this.add(btnBackup,1,6);
        /* CLIENT -> Ignore list*/
        // TODO implement this ignore list
//        Label labelIgnore = new Label("Ignore list: ");
//        setRowIndex(labelIgnore, 5);
//        setColumnIndex(labelIgnore, 0);
//
//        ListView<String> ignoreList = new ListView<String>();
//        ObservableList<String> items = FXCollections.observableArrayList(SyncConfig.getConfig().FILE_IGNORE_LIST);
//        ignoreList.setItems(items);
//        setRowIndex(ignoreList, 6);
//        setColumnIndex(ignoreList, 0);

        this.getChildren().addAll(labelClient, labelIp, labelPort, labelRefuse, getRefuseClientModsCheckbox(), getAddressField(), getPortField());
//        this.getChildren().addAll(labelIgnore, ignoreList);
        refreshConfigValues();
    }

    public TextField getAddressField() {
        if (fieldAddress == null) {
            fieldAddress = new TextField();
            fieldAddress.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue) {
                    SyncConfig.getConfig().SERVER_IP = fieldAddress.getText();
                    saveConfig();
                }
            });
        }
        return fieldAddress;
    }

    public TextField getPortField() {
        if (fieldPort == null) {
            fieldPort = new TextField();
            fieldPort.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue) {
                    SyncConfig.getConfig().SERVER_PORT = Integer.parseInt(fieldPort.getText(), 10);
                    saveConfig();
                }
            });
        }
        return fieldPort;
    }

    public CheckBox getRefuseClientModsCheckbox() {
        if (cbxRefuse == null) {
            cbxRefuse = new CheckBox();
            cbxRefuse.setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
            cbxRefuse.selectedProperty().addListener((observable, oldValue, newValue) -> {
                SyncConfig.getConfig().REFUSE_CLIENT_MODS = newValue;
                saveConfig();
            });
        }
        return cbxRefuse;
    }

    public ComboBox<String> getComboBoxTheme() {
        if (comboBoxTheme == null) {
            ObservableList<String> themes =
                FXCollections.observableArrayList(
                    Stream.of(ETheme.values())
                          .map(Enum::name)
                          .collect(Collectors.toList())
                );
            comboBoxTheme = new ComboBox<>(themes);
            comboBoxTheme.getSelectionModel().select(SyncConfig.getConfig().THEME.ordinal());
            comboBoxTheme.valueProperty().addListener((obs, oldItem, newItem) -> {
                ETheme newTheme = ETheme.valueOf((String) newItem);
                Gui_JavaFX.getStackMainPane().setStyle(newTheme.toString());
                SyncConfig.getConfig().THEME = newTheme;
                saveConfig();
            });
        }
        return comboBoxTheme;
    }

    public ComboBox<String> getComboBoxLanguage() {
        if (comboBoxLanguage == null) {
            comboBoxLanguage = new ComboBox();
            comboBoxLanguage.getItems().addAll(
                    ServerSync.strings.getString("language/english"),
                    ServerSync.strings.getString("language/spanish"),
                    ServerSync.strings.getString("language/french"),
                    ServerSync.strings.getString("language/polish"),
                    ServerSync.strings.getString("language/russian"),
                    ServerSync.strings.getString("language/chinese"));
            setDefaultComboxBox();
            comboBoxLanguage.valueProperty().addListener((obs, oldItem, newItem) -> {
                changeLanguage(newItem);
                saveConfig();
            });
        }
        return comboBoxLanguage;
    }

    public void refreshConfigValues() {
        getAddressField().setText(SyncConfig.getConfig().SERVER_IP);
        getPortField().setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        getRefuseClientModsCheckbox().setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
        getComboBoxTheme().getSelectionModel().select(SyncConfig.getConfig().THEME.name());
    }

    public void updateLogsArea(String text) {
        Gui_JavaFX.getStackMainPane().getPaneLogs().updateLogsArea(text);
    }

    private void saveConfig() {
        try {
            SyncConfig.getConfig().save();
            updateLogsArea("Options saved");
        } catch (IOException ex) {
            Logger.debug(ex);
            updateLogsArea(ex.toString());
        }
    }

    private void changeLanguage(String language) {
        if (language.equals(ServerSync.strings.getString("language/english"))) {
            I18N.setLocale(new Locale("en", "US"));
        } else if (language.equals(ServerSync.strings.getString("language/spanish"))) {
            I18N.setLocale(new Locale("es", "ES"));
        } else if (language.equals(ServerSync.strings.getString("language/french"))) {
            I18N.setLocale(new Locale("fr", "FR"));
        } else if (language.equals(ServerSync.strings.getString("language/polish"))) {
            I18N.setLocale(new Locale("pl", "PL"));
        } else if (language.equals(ServerSync.strings.getString("language/russian"))) {
            I18N.setLocale(new Locale("ru", "RU"));
        } else if (language.equals(ServerSync.strings.getString("language/chinese"))) {
            I18N.setLocale(new Locale("zh", "CN"));
        }
    }

    private void setDefaultComboxBox(){
        Locale locale = SyncConfig.getConfig().LOCALE;
        if (locale.equals(new Locale("en", "US"))) {
            comboBoxLanguage.getSelectionModel().select(ServerSync.strings.getString("language/english"));
        } else if (locale.equals(new Locale("es", "ES"))) {
            comboBoxLanguage.getSelectionModel().select(ServerSync.strings.getString("language/spanish"));
        } else if (locale.equals(new Locale("fr", "FR"))) {
            comboBoxLanguage.getSelectionModel().select(ServerSync.strings.getString("language/french"));
        } else if (locale.equals(new Locale("pl", "PL"))) {
            comboBoxLanguage.getSelectionModel().select(ServerSync.strings.getString("language/polish"));
        } else if (locale.equals(new Locale("ru", "RU"))) {
            comboBoxLanguage.getSelectionModel().select(ServerSync.strings.getString("language/russian"));
        } else if (locale.equals(new Locale("zh", "CN"))) {
            comboBoxLanguage.getSelectionModel().select(ServerSync.strings.getString("language/chinese"));
        }
    }
}
