package com.ayoub.netsight.utils;

import com.ayoub.netsight.NetSightApp;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.Objects;

public class JavaFxUtils {

    public static void showInfo(String title, String content ){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(Objects.requireNonNull(NetSightApp.class.getResourceAsStream("/icons/eye-scan.png"))));

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setContentText(content);

        alert.showAndWait();
    }

}
