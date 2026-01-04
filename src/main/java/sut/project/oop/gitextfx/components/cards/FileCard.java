package sut.project.oop.gitextfx.components.cards;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.AppDateFormat;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.clazz.ErrorDialog;
import sut.project.oop.gitextfx.clazz.Schema;
import sut.project.oop.gitextfx.clazz.SqliteStore;
import sut.project.oop.gitextfx.controllers.MainPanelController;
import sut.project.oop.gitextfx.controllers.WelcomeController;
import sut.project.oop.gitextfx.models.FileRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

public class FileCard extends HBox {
    public FileCard(int index, FileRecord file, WelcomeController ctrl) {

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
        Button deleteButton = new Button("Delete");

        indexBox.getStyleClass().add("file-card-index-box");
        indexLabel.getStyleClass().add("file-card-index");
        fileName.getStyleClass().add("file-card-title");
        lastEdit.getStyleClass().add("file-card-subtitle");
        manageButton.getStyleClass().add("file-card-action");
        deleteButton.getStyleClass().add("file-card-action-destructive");

        manageButton.setOnAction(_ -> {
            try {
                Stage newStage = new Stage();
                FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("main-panel.fxml"));

                Scene scene = new Scene(loader.load());
                scene.getStylesheets().add(
                        Objects.requireNonNull(
                                        GitextApp.class.getResource("version-card.css"))
                                .toExternalForm()
                );
                ((MainPanelController) loader.getController()).onReady(Path.of(file.getFilePath()), file.getId(), newStage);

                newStage.setTitle("File: " + Path.of(file.getFilePath()).getFileName());
                newStage.setScene(scene);
                newStage.show();

                ctrl.stage.close();
            } catch (IOException _) {}
        });

        deleteButton.setOnAction(_ -> {
            try {
                var store = new SqliteStore();
                var version_tags = store.getVersionTagsOf((int) file.getId());

                for (var tag : version_tags) {
                    store.deleteVersion(tag.row_id());
                }

                store.deleteFileRecord((int) file.getId());

            } catch (SQLException e) {
                ErrorDialog.showDevException(e, "Can not delete a record.");
            }

            ctrl.queryOriginalFileRecords();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Success!");
            alert.initOwner(ctrl.stage);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.showAndWait();
        });

        getChildren().addAll(indexBox, infoBox, spacer, manageButton, deleteButton);
    }
}
