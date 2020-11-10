package com.superzanti.serversync.util.enums;

public enum EThemes {
    /*
        -fx-primary     -> Color background
        -fx-secondary   -> Color button/border/progressbar
        -fx-sidebar-primary -> Color background for the sidebar
    */
    DARK_CYAN   ("-fx-primary : #2A2E37; -fx-secondary : #4DB3B3; -fx-sidebar-primary: #23232e; -fx-primarytext : #B2B2B2; -fx-secondarytext: #b6b6b6; -fx-background-color: -fx-primary;"),
    DARK_GREEN  ("-fx-primary : #262626; -fx-secondary : #86d291; -fx-sidebar-primary: #1c1c1c; -fx-primarytext : #B2B2B2; -fx-secondarytext: #b6b6b6; -fx-background-color: -fx-primary;"),
    BLUE_YELLOW ("-fx-primary : #232834; -fx-secondary : #edbe60; -fx-sidebar-primary: #1f2430; -fx-primarytext : #B2B2B2; -fx-secondarytext: #b6b6b6; -fx-background-color: -fx-primary;"),
    GREY_BLUE    ("-fx-primary : #2e3440; -fx-secondary : #95c5c3; -fx-sidebar-primary: #3b4252; -fx-primarytext : #B2B2B2; -fx-secondarytext: #b6b6b6; -fx-background-color: -fx-primary;");

    private final String theme;

    EThemes(String theme) {
        this.theme = theme;
    }

    public String toString() {
        return this.theme;
    }
}
