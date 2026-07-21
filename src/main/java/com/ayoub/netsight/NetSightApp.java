package com.ayoub.netsight;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.Objects;

/**
 * Entry point of the NetSight application.
 * Bootstraps the JavaFX runtime, loads the main FXML layout,
 * and configures the primary stage with title, icon, and minimum dimensions.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class NetSightApp extends Application {

    /**
     * Initializes and displays the primary stage.
     *
     * @param stage the primary stage provided by the JavaFX runtime
     * @throws Exception if the FXML file fails to load
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Image icon = new Image(Objects.requireNonNull(NetSightApp.class.getResourceAsStream("/icons/eye-scan.png")));

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());

        stage.getIcons().add(icon);
        stage.setTitle("NetSight - LAN Network Scanner");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}