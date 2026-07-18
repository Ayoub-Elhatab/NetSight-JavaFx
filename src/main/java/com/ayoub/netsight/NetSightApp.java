package com.ayoub.netsight;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.Objects;

public class NetSightApp extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Image icon = new Image(Objects.requireNonNull(NetSightApp.class.getResourceAsStream("/icons/network-hub.png")));

        Scene scene = new Scene(loader.load());

        stage.getIcons().add(icon);
        stage.setTitle("LAN Network Scanner");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}