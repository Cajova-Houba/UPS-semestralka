package org.valesz.ups.main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.controller.LoginController;
import org.valesz.ups.controller.ViewController;
import org.valesz.ups.ui.LoginPane;
import org.valesz.ups.ui.MainPane;
import org.valesz.ups.network.TcpClient;

/**
 * Main javafx application.
 *
 * @author Zdenek Vales
 */
public class MainApp extends Application{

    private static final Logger logger = LogManager.getLogger(MainApp.class);


    private static TcpClient tcpClient;
    public static final int DEF_WIDTH = 855;
    public static final int DEF_HEIGHT = 480;
    public static final int DEF_LOGIN_WIDTH = 640;
    public static final int DEF_LOGIN_HEIGHT = 240;
    public static final String DEF_TITLE = "Senet - klient";


    /**
     * Scenes.
     */
    private static Scene mainScene, loginScene;
    private static Stage stage;

    public static void main(String[] args) {
        logger.info("Initializing main app.");
        tcpClient = new TcpClient();

        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.debug("Staring JavaFX app.");
        stage = primaryStage;
        stage.setTitle(DEF_TITLE);

        initLoginScene();
        initMainScene();

        stage.setScene(loginScene);
        stage.show();
    }

    private static void initLoginScene() {
        loginScene = new Scene(new LoginPane(tcpClient), DEF_LOGIN_WIDTH, DEF_LOGIN_HEIGHT);
    }

    private static void initMainScene() {
        mainScene = new Scene(new MainPane(tcpClient), DEF_WIDTH, DEF_HEIGHT);
    }

    /**
     * Sets the loginScene as a scene.
     */
    public static void switchToLogin() {
        if(loginScene == null) {
            initLoginScene();
        }

        stage.setScene(loginScene);
    }

    /**
     * Sets the mainScene as a scene.
     */
    public static void switchToMain() {
        if(mainScene == null) {
            initMainScene();
        }

        stage.setScene(mainScene);
        ((MainPane)mainScene.getRoot()).waitForStartGame();
    }


}
