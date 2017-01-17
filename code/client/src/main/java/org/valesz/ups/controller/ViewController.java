package org.valesz.ups.controller;

import org.valesz.ups.main.MainApp;
import org.valesz.ups.ui.MainPane;

/**
 * This controller will switch between views - main and login.
 *
 * @author Zdenek Vales
 */
public class ViewController {

    private MainPane mainView;

    /**
     * Displays login pane.
     */
    public void displayLoginPane() {
        MainApp.switchToLogin();
    }

    /**
     * Displays main pane.
     */
    public void displayMainPane() {
        MainApp.switchToMain();
    }

    /**
     * Displays main pane with port and nick info.
     */
    public void displayMainPane(int port, String nick) {

        MainApp.switchToMain();
        mainView.showPortAndNick(port, nick);
    }

    public void setMainView(MainPane mainView) {
        this.mainView = mainView;
    }
}
