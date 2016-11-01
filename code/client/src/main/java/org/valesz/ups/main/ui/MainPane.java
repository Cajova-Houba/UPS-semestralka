package org.valesz.ups.main.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.network.TcpClient;

/**
 * Main window.
 *
 * @author Zdenek Vales
 */
public class MainPane extends GridPane {

    private static final Logger logger = LogManager.getLogger(MainPane.class);
    public static final Font DEF_FONT = Font.font("Tahoma", FontWeight.NORMAL, 20);
    public static final Font DEF_SMALL_FONT = Font.font("Tahoma", FontWeight.NORMAL, 12);
    public static final String GAME_TITLE = "Senet";

    private TcpClient tcpClient;

    public MainPane(TcpClient tcpClient) {
        super();
        this.tcpClient = tcpClient;
        initialize();
    }

    /**
     * Initializes the components in this pane.
     */
    private void initialize() {
        setAlignment(Pos.CENTER);
        setHgap(0);
        setVgap(0);
        setPadding(new Insets(0,0,0,0));

        /* side panel */
        VBox sidePane = new VBox();
        Text gameTitle = new Text(GAME_TITLE);
        gameTitle.setFont(DEF_FONT);
        sidePane.getChildren().add(gameTitle);

        Text p1 = new Text("Player 1");
        p1.setFont(DEF_SMALL_FONT);
        sidePane.getChildren().add(p1);
        Text p2 = new Text("Player 2");
        p2.setFont(DEF_SMALL_FONT);
        sidePane.getChildren().add(p2);

        Button endBtn = new Button("Exit");
        endBtn.setOnAction(event -> {onExitClick();});
        sidePane.getChildren().add(endBtn);

        add(sidePane,0,1,1,3);
    }

    /**
     * Exit button callback.
     */
    public void onExitClick() {
        MainApp.switchToMain();
    }
}
