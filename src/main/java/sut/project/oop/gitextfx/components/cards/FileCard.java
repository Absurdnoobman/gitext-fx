package sut.project.oop.gitextfx.components.cards;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.AppDateFormat;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.controllers.MainPanelController;
import sut.project.oop.gitextfx.models.FileRecord;

import java.io.IOException;
import java.nio.file.Path;

public class FileCard extends HBox {
    public FileCard(int index, FileRecord file, Stage oldStage) {

        getStyleClass().add("file-card");

        // Card container
        setSpacing(12);
        setPadding(new Insets(12));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("file-card");
        setAlignment(Pos.CENTER_LEFT);

        /* ───────── Index ───────── */
        Label indexLabel = new Label(String.valueOf(index));
        indexLabel.getStyleClass().add("file-card-index");

        StackPane indexBox = new StackPane(indexLabel);
        indexBox.setMinSize(28, 28);
        indexBox.getStyleClass().add("file-card-index-box");

        /* ───────── File info ───────── */
        Label fileName = new Label(Path.of(file.getFilePath()).getFileName().toString());
        fileName.getStyleClass().add("file-card-title");

        Label lastEdit = new Label(
                "Last edited " + file.getLastedEdit().format(AppDateFormat.DISPLAY)
        );
        lastEdit.getStyleClass().add("file-card-subtitle");

        VBox infoBox = new VBox(4, fileName, lastEdit);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        /* ───────── Spacer ───────── */
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        /* ───────── Action ───────── */
        Button manageButton = new Button("Manage");
        manageButton.getStyleClass().add("file-card-action");

        indexBox.getStyleClass().add("file-card-index-box");
        indexLabel.getStyleClass().add("file-card-index");
        fileName.getStyleClass().add("file-card-title");
        lastEdit.getStyleClass().add("file-card-subtitle");
        manageButton.getStyleClass().add("file-card-action");

        manageButton.setOnAction(_ -> {
            try {
                Stage newStage = new Stage();
                FXMLLoader loader =
                        new FXMLLoader(GitextApp.class.getResource("main-panel.fxml"));

                Scene scene = new Scene(loader.load());
                ((MainPanelController) loader.getController()).onReady(Path.of(file.getFilePath()), file.getId(), newStage);

                newStage.setTitle("File: " + Path.of(file.getFilePath()).getFileName());
                newStage.setScene(scene);
                newStage.show();

                oldStage.close();
            } catch (IOException _) {}
        });

        getChildren().addAll(indexBox, infoBox, spacer, manageButton);
    }
}
