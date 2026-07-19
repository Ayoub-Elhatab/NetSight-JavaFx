package com.ayoub.netsight.utils;

import com.ayoub.netsight.NetSightApp;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.Objects;

/**
 * Utility class for common JavaFX UI operations.
 * Provides reusable methods for dialogs, alerts, and other UI helpers.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class JavaFxUtils {

    /**
     * Displays a styled information dialog with the NetSight icon.
     *
     * @param title   the dialog window title
     * @param content the message body to display
     */
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
