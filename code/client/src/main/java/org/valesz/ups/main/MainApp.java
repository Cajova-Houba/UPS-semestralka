package org.valesz.ups.main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.controller.GameController;
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

    /**
     * Controllers used in app.
     */
    private static ViewController viewController;
    private static LoginController loginController;
    private static GameController gameController;

    public static void main(String[] args) {
        logger.info("Initializing main app.");
        tcpClient = new TcpClient();
        initControllers();

        Application.launch(args);
    }

    private static void initControllers() {
        viewController = new ViewController();
        loginController = new LoginController(tcpClient);
        gameController = new GameController(tcpClient);
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
        loginController = new LoginController(tcpClient);
        LoginPane loginPane = new LoginPane(tcpClient, loginController);
        loginScene = new Scene(loginPane, DEF_LOGIN_WIDTH, DEF_LOGIN_HEIGHT);
    }

    private static void initMainScene() {
        mainScene = new Scene(new MainPane(tcpClient, gameController, viewController, loginController), DEF_WIDTH, DEF_HEIGHT);
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
        } else {
            ((MainPane)mainScene.getRoot()).initialize();
        }
        stage.setScene(mainScene);
        ((MainPane)mainScene.getRoot()).waitForStartGame();
    }


}
