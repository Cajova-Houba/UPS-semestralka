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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Main javafx application.
 *
 * @author Zdenek Vales
 */
public class MainApp extends Application{

    public static final int DEF_WIDTH = 640;
    public static final int DEF_HEIGHT = 480;
    public static final String DEF_TITLE = "Senet - klient";
    public static String VERSION = "verze";

    public static void main(String[] args) {
        VERSION = MainApp.class.getPackage().getImplementationVersion();
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle(DEF_TITLE);

        Scene scene = initScene();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Scene initScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        Text sceneTitle = new Text("Connect to server:");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
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
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 5);

        Scene scene = new Scene(grid, DEF_WIDTH, DEF_HEIGHT);

        return scene;
    }
}
