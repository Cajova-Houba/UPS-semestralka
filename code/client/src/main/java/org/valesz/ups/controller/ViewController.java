package org.valesz.ups.controller;

import org.valesz.ups.main.MainApp;

/**
 * This controller will switch between views - main and login.
 *
 * @author Zdenek Vales
 */
public class ViewController {

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
}
