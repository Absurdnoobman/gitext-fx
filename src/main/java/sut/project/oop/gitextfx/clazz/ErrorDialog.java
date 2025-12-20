package sut.project.oop.gitextfx.clazz;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDialog {
    private ErrorDialog() {} // utility class. can not instance

    public static void showException(String msg) {
        showException(msg, "Error", null);
    }

    public static void showException(String msg, String title) {
        showException(msg, title, null);
    }

    public static void showException(String msg, String title, Stage owner) {
        Runnable show = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        };

        // safe call from any thread
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    public static void showDevException(Throwable e, String msg) {
        showDevException(e, msg, null);
    }

    public static void showDevException(Throwable e, String msg, Stage owner) {

        Runnable show = () -> {
            // stack trace to string
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            Label message = new Label(msg);
            message.setWrapText(true);

            // Stack trace area
            TextArea stackTrace = new TextArea(sw.toString());
            stackTrace.setEditable(false);
            stackTrace.setWrapText(false);
            stackTrace.setPrefWidth(600);
            stackTrace.setPrefHeight(350);

            VBox content = new VBox(10, message, stackTrace);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.setTitle("DEBUG: Exception Details");
            alert.setHeaderText(null);
            alert.getDialogPane().setContent(content);
            alert.setResizable(true);

            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }
}
