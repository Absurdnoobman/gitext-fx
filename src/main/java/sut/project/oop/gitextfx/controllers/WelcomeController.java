package sut.project.oop.gitextfx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.clazz.CompressionUtil;
import sut.project.oop.gitextfx.clazz.ErrorDialog;
import sut.project.oop.gitextfx.clazz.Schema;
import sut.project.oop.gitextfx.components.cards.FileCard;
import sut.project.oop.gitextfx.models.FileRecord;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class WelcomeController {
    public Stage stage;

    @FXML
    private VBox FileListVBox;

    public void onReady(List<FileRecord> file_result) {
        FileListVBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        FileListVBox.setFillWidth(true);

        int i = 1;

        for (var f : file_result) {
            var card = new FileCard(i, f, stage);
            FileListVBox.getChildren().add(card);

            i++;
        }
    }


    @FXML
    private void onAddFileButtonPressed(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        File result = fileChooser.showOpenDialog(null);

        if (result == null) return;

        String full_path = result.getPath();
        long id = -1;
        try (var db = new Schema()) {
            id = db.insertAndReturnID("""
                    INSERT INTO Files (file_path, lasted_edit) VALUES (?, ?)
                    """, full_path, LocalDateTime.now());

            if (id == -1) {
                ErrorDialog.showException("Can not insert a file.");
                return;
            }

            FileReader reader = new FileReader(full_path);
            String raw = reader.readAllAsString();

            byte[] compressed = CompressionUtil.compress(raw);

            var is_success = db.execute("""
                    INSERT INTO Versions (file_id, parent_id, tag, is_delta, compressed, created_at)
                    VALUES ( ?, NULL, 'First version', FALSE, ?, ? )
                    """,
                    id, compressed, LocalDateTime.now()
            );

            if (!is_success) ErrorDialog.showException("Can not insert a version.");

        } catch (SQLException | IOException e) {
            var is_dev_mode = Boolean.parseBoolean(System.getProperty("dev"));

            if (is_dev_mode) {
                ErrorDialog.showDevException(e, "cannot execute database operations");
            } else {
                ErrorDialog.showException("cannot execute database operations");
            }
        }

        Stage new_stage = new Stage();

        FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("main-panel.fxml"));

        Scene scene = null;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            ErrorDialog.showException("Can not open menu.");
            return;
        }

        if (id == -1) return;

        final Path path = Path.of(full_path);
        ((MainPanelController) loader.getController()).onReady(path, id, new_stage);
        new_stage.setTitle("File: %s".formatted(path.getFileName()));
        new_stage.setScene(scene);
        new_stage.show();

        stage.close();

    }

    @FXML
    private void onSettingPressed() {
        FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("setting-view.fxml"));

        try {
            var scene = new Scene(loader.load());
            Stage new_stage = new Stage();
            new_stage.setTitle("Setting");
            new_stage.setScene(scene);
            new_stage.show();
        } catch (IOException e) {
            ErrorDialog.showException("Can not open menu.");
        }

    }
}
