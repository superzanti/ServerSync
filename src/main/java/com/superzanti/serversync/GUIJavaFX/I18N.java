package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.client.ActionEntry;
import com.superzanti.serversync.client.EActionType;
import com.superzanti.serversync.config.SyncConfig;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;

import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

/**
 * I18N utility class..
 * source : https://outofmemory.programmingwith.com/javafx/5434/internationalization-in-javafx/23068/switching-language-dynamically-when-the-application-is-running
 */
public final class I18N {

    /** the current selected Locale. */
    private static final ObjectProperty<Locale> locale;

    static {
        locale = new SimpleObjectProperty<>(getDefaultLocale());
        locale.addListener((observable, oldValue, newValue) -> Locale.setDefault(newValue));
    }

    /**
     * get the supported Locales.
     *
     * @return List of Locale objects.
     */
    public static List<Locale> getSupportedLocales() {
        return new ArrayList<>(Arrays.asList(
                new Locale("en","US"),
                new Locale("es","ES"),
                new Locale("fr","FR"),
                new Locale("pl","PL"),
                new Locale("ru","RU"),
                new Locale("zh","CN")));
    }

    /**
     * get the default locale. This is the systems default if contained in the supported locales, english otherwise.
     *
     * @return
     */
    public static Locale getDefaultLocale() {
        Locale sysDefault = SyncConfig.getConfig().LOCALE;
        return getSupportedLocales().contains(sysDefault) ? sysDefault : Locale.ENGLISH;
    }

    public static Locale getLocale() {
        return locale.get();
    }

    public static void setLocale(Locale locale) {
        SyncConfig.getConfig().LOCALE = locale;
        ServerSync.strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", locale);
        localeProperty().set(locale);
        Locale.setDefault(locale);
    }

    public static ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    /**
     * gets the string with the given key from the resource bundle for the current locale and uses it as first argument
     * to MessageFormat.format, passing in the optional args and returning the result.
     *
     * @param key
     *         message key
     * @param args
     *         optional arguments for the message
     * @return localized formatted string
     */
    public static String get(final String key, final Object... args) throws UnsupportedEncodingException {
        ResourceBundle bundle = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", getLocale());
        return MessageFormat.format(bundle.getString(key), args);
    }

    /**
     * creates a String binding to a localized String for the given message bundle key
     *
     * @param key
     *         key
     * @return String binding
     */
    public static StringBinding createStringBinding(final String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), locale);
    }

    /**
     * creates a String Binding to a localized String that is computed by calling the given func
     *
     * @param func
     *         function called on every change
     * @return StringBinding
     */
    public static StringBinding createStringBinding(Callable<String> func) {
        return Bindings.createStringBinding(func, locale);
    }

    /**
     * creates a bound Label whose value is computed on language change.
     *
     * @param func
     *         the function to compute the value
     * @return Label
     */
    public static Label labelForValue(Callable<String> func) {
        Label label = new Label();
        label.textProperty().bind(createStringBinding(func));
        return label;
    }

    /**
     * creates a bound Button for the given resourcebundle key
     *
     * @param key
     *         ResourceBundle key
     * @param args
     *         optional arguments for the message
     * @return Button
     */
    public static Button buttonForKey(final String key, final Object... args) {
        Button button = new Button();
        button.textProperty().bind(createStringBinding(key, args));
        return button;
    }
    public static TableColumn<ActionEntry, String> tableColumnForKey(final String key, final Object... args) {
        TableColumn<ActionEntry, String> col = new TableColumn<>();
        col.textProperty().bind(createStringBinding(key, args));
        return col;
    }
    public static TableColumn<ActionEntry, EActionType> tableColumnEActionForKey(final String key, final Object... args) {
        TableColumn<ActionEntry, EActionType> col = new TableColumn<>();
        col.textProperty().bind(createStringBinding(key, args));
        return col;
    }
    public static Tooltip toolTipForKey(final String key, final Object... args) {
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(createStringBinding(key, args));
        return tooltip;
    }
}