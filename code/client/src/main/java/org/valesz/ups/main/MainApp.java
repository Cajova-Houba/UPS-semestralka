package org.valesz.ups.main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.network.ThreadListener;

/**
 * Main javafx application.
 *
 * @author Zdenek Vales
 */
public class MainApp extends Application implements ThreadListener {

    public static final int DEF_WIDTH = 640;
    public static final int DEF_HEIGHT = 480;
    public static final int DEF_LOGIN_WIDTH = 640;
    public static final int DEF_LOGIN_HEIGHT = 240;
    public static final String DEF_TITLE = "Senet - klient";
    public static final String GAME_TITLE = "Senet";
    public static final Font DEF_FONT = Font.font("Tahoma", FontWeight.NORMAL, 20);
    public static final Font DEF_SMALL_FONT = Font.font("Tahoma", FontWeight.NORMAL, 12);


    public static String VERSION = "verze";
    private static Stage stage;
    private static Scene mainScene, loginScene;

    public static void main(String[] args) {
        VERSION = MainApp.class.getPackage().getImplementationVersion();
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        primaryStage.setTitle(DEF_TITLE);

        switchToLogin();

        primaryStage.show();
    }

    /**
     * Initializes main scene and returns it.
     */
    private static Scene initMainScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setPadding(new Insets(0,0,0,0));

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
        endBtn.setOnAction(event -> {
            switchToLogin();
        });
        sidePane.getChildren().add(endBtn);

        grid.add(sidePane,0,1,1,3);

        mainScene = new Scene(grid, DEF_WIDTH, DEF_HEIGHT);

        return mainScene;
    }


    /**
     * Initializes login scene and returns it.
     * @return
     */
    private static Scene initLoginScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        Text sceneTitle = new Text("Connect to server:");
        sceneTitle.setFont(DEF_FONT);
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label userName = new Label("Nickname:");
        grid.add(userName, 0, 1);
        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        Label address = new Label("Address:");
        grid.add(address, 0, 2);
        TextField addressTextField = new TextField();
        grid.add(addressTextField, 1, 2);

        Label port = new Label("Port:");
        grid.add(port, 0, 3);
        TextField portTextField = new TextField();
        grid.add(portTextField, 1, 3);

        Button btn = new Button("Connect");
        btn.setOnAction(event -> {switchToMain();});
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 5);

        loginScene = new Scene(grid, DEF_LOGIN_WIDTH, DEF_LOGIN_HEIGHT);

        return loginScene;
    }

    /**
     * Sets the loginScene as a scene.
     */
    private static void switchToLogin() {
        if(loginScene == null) {
            loginScene = initLoginScene();
        }

        stage.setScene(loginScene);
    }

    /**
     * Sets the mainScene as a scene.
     */
    private static void switchToMain() {
        if(mainScene == null) {
            mainScene = initMainScene();
        }

        stage.setScene(mainScene);
    }

    @Override
    public void notifyOnError(Thread thread, Error error) {

    }

    @Override
    public void notifyOnOperationOk(Thread thread) {

    }
}
