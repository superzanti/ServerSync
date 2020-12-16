package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.ETheme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// GUI of options
public class PaneOptions extends GridPane {

    private final TextField address = getAddressField();
    private final TextField port = getPortField();
    private final CheckBox refuseClientMods = getRefuseClientModsCheckbox();
    private final ComboBox<String> themeComboBox = getThemeComboBox();
    private ComboBox<String> comboBoxlanguage;

    public PaneOptions() {
        this.setAlignment(Pos.CENTER);
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

        ComboBox<String> comboBox = getThemeComboBox();
        comboBox.getSelectionModel().select(SyncConfig.getConfig().THEME.ordinal());
        comboBox.valueProperty().addListener((obs, oldItem, newItem) -> {
            ETheme newTheme = ETheme.valueOf(newItem);
            Gui_JavaFX.getStackMainPane().setStyle(newTheme.toString());
            SyncConfig.getConfig().THEME = newTheme;
            saveConfig();
        });
        setRowIndex(comboBox, 1);
        setColumnIndex(comboBox, 1);

        this.getChildren().addAll(labelTheme, comboBox);

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

        TextField fieldAddress = getAddressField();
        fieldAddress.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue) {
                SyncConfig.getConfig().SERVER_IP = fieldAddress.getText();
                saveConfig();
            }
        });
        setRowIndex(fieldAddress, 3);
        setColumnIndex(fieldAddress, 1);

        /* CLIENT -> PORT*/
        Label labelPort = I18N.labelForValue(() -> I18N.get("ui/server_port"));
        setRowIndex(labelPort, 4);
        setColumnIndex(labelPort, 0);

        TextField fieldPort = getPortField();
        fieldPort.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue) {
                SyncConfig.getConfig().SERVER_PORT = Integer.parseInt(fieldPort.getText(), 10);
                saveConfig();
            }
        });
        setRowIndex(fieldPort, 4);
        setColumnIndex(fieldPort, 1);

        /* CLIENT -> Refuse client mods*/
        Label labelRefuse = I18N.labelForValue(() -> I18N.get("ui/refuse_client_mods"));
        setRowIndex(labelRefuse, 5);
        setColumnIndex(labelRefuse, 0);

        CheckBox cbxRefuse = getRefuseClientModsCheckbox();
        cbxRefuse.setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
        cbxRefuse.selectedProperty().addListener((observable, oldValue, newValue) -> {
            SyncConfig.getConfig().REFUSE_CLIENT_MODS = newValue;
            saveConfig();
        });
        setRowIndex(cbxRefuse, 5);
        setColumnIndex(cbxRefuse, 1);

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

        this.getChildren().addAll(labelClient, labelIp, labelPort, labelRefuse, cbxRefuse, fieldAddress, fieldPort);
//        this.getChildren().addAll(labelIgnore, ignoreList);
        refreshConfigValues();
    }

    public TextField getAddressField() {
        if (address == null) {
            return new TextField();
        }
        return address;
    }

    public TextField getPortField() {
        if (port == null) {
            return new TextField();
        }
        return port;
    }

    public CheckBox getRefuseClientModsCheckbox() {
        if (refuseClientMods == null) {
            return new CheckBox();
        }
        return refuseClientMods;
    }

    public ComboBox<String> getThemeComboBox() {
        if (themeComboBox == null) {
            ObservableList<String> themes =
                FXCollections.observableArrayList(
                    Stream.of(ETheme.values())
                          .map(Enum::name)
                          .collect(Collectors.toList())
                );
            return new ComboBox<>(themes);
        }
        return themeComboBox;
    }

    public ComboBox<String> getComboBoxLanguage() {
        if (comboBoxlanguage == null) {
            comboBoxlanguage = new ComboBox();

            comboBoxlanguage.getItems().addAll(
                    ServerSync.strings.getString("language/english"),
                    ServerSync.strings.getString("language/spanish"),
                    ServerSync.strings.getString("language/french"),
                    ServerSync.strings.getString("language/polish"),
                    ServerSync.strings.getString("language/russian"),
                    ServerSync.strings.getString("language/chinese"));
            setDefaultComboxBox();
            comboBoxlanguage.valueProperty().addListener((obs, oldItem, newItem) -> {
                changeLanguage(newItem);
                saveConfig();
            });
        }
        return comboBoxlanguage;
    }

    public void refreshConfigValues() {
        getAddressField().setText(SyncConfig.getConfig().SERVER_IP);
        getPortField().setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        getRefuseClientModsCheckbox().setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
        getThemeComboBox().getSelectionModel().select(SyncConfig.getConfig().THEME.name());
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
        comboBoxlanguage.getItems().clear();
        comboBoxlanguage.getItems().addAll(
                ServerSync.strings.getString("language/english"),
                ServerSync.strings.getString("language/spanish"),
                ServerSync.strings.getString("language/french"),
                ServerSync.strings.getString("language/polish"),
                ServerSync.strings.getString("language/russian"),
                ServerSync.strings.getString("language/chinese"));
        setDefaultComboxBox();
    }

    private void setDefaultComboxBox(){
        Locale locale = SyncConfig.getConfig().LOCALE;
        if (locale.equals(new Locale("en", "US"))) {
            comboBoxlanguage.getSelectionModel().select(ServerSync.strings.getString("language/english"));
        } else if (locale.equals(new Locale("es", "ES"))) {
            comboBoxlanguage.getSelectionModel().select(ServerSync.strings.getString("language/spanish"));
        } else if (locale.equals(new Locale("fr", "FR"))) {
            comboBoxlanguage.getSelectionModel().select(ServerSync.strings.getString("language/french"));
        } else if (locale.equals(new Locale("pl", "PL"))) {
            comboBoxlanguage.getSelectionModel().select(ServerSync.strings.getString("language/polish"));
        } else if (locale.equals(new Locale("ru", "RU"))) {
            comboBoxlanguage.getSelectionModel().select(ServerSync.strings.getString("language/russian"));
        } else if (locale.equals(new Locale("zh", "CN"))) {
            comboBoxlanguage.getSelectionModel().select(ServerSync.strings.getString("language/chinese"));
        }
    }
}
