package org.valesz.ups.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * This pane will be displayed after the end of game
 *
 * @author Zdenek Vales
 */
public class EndGameAlert extends Alert{

    private String winnerNick;

    public static final ButtonType EXIT_BUTTON = new ButtonType("Odejít");
    public static final ButtonType NEW_GAME_BUTTON = new ButtonType("Nová hra");
    public static final ButtonType ANOTHER_SERVER_BUTTON = new ButtonType("Jiný server");

    public EndGameAlert(String winnerNick) {
        super(AlertType.CONFIRMATION);
        this.winnerNick = winnerNick;
        this.setTitle("Konec hry!");
        this.setHeaderText("Hráč "+winnerNick+" vyhrál!");
        this.setContentText("Co budeme dělat dál?");
        this.getButtonTypes().setAll(EXIT_BUTTON, NEW_GAME_BUTTON, ANOTHER_SERVER_BUTTON);
    }
}
