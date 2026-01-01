package sut.project.oop.gitextfx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.clazz.*;
import sut.project.oop.gitextfx.components.cards.FileCard;
import sut.project.oop.gitextfx.enums.UIeditSortOrder;
import sut.project.oop.gitextfx.models.FileRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class WelcomeController {
    public Stage stage;
    @FXML
    public ComboBox<UIeditSortOrder> sortComboBox;

    @FXML
    private VBox FileListVBox;

    private List<FileRecord> files = new ArrayList<>();

    public void onReady(List<FileRecord> file_result) {
        FileListVBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        FileListVBox.setFillWidth(true);

        sortComboBox.getItems().setAll(
                UIeditSortOrder.LAST_EDIT_NEWEST,
                UIeditSortOrder.LAST_EDIT_OLDEST
        );

        sortComboBox.setValue(UIeditSortOrder.LAST_EDIT_NEWEST);

        sortComboBox.valueProperty().addListener(
                (_, _, newValue) -> applySort(newValue)
        );

        files = file_result;

        refreshFileList();
    }

    private void applySort(UIeditSortOrder order) {
        switch (order) {
            case LAST_EDIT_NEWEST -> sortByLastEditNewest();
            case LAST_EDIT_OLDEST -> sortByLastEditOldest();
        }
        refreshFileList();
    }

    private void sortByLastEditNewest() {
        sort(files,
                (a, b) -> a.getLastedEdit().isBefore(b.getLastedEdit())
        );
    }

    private void sortByLastEditOldest() {
        sort(files,
                (a, b) -> a.getLastedEdit().isAfter(b.getLastedEdit())
        );
    }

    private void refreshFileList() {
        FileListVBox.getChildren().clear();

        for (int i = 0; i < files.size(); i++) {
            FileListVBox.getChildren().add(
                    new FileCard(i + 1, files.get(i), stage)
            );
        }
    }


    @FXML
    private void onAddFileButtonPressed(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        File result = fileChooser.showOpenDialog(null);

        if (result == null) return;

        String full_path = result.getPath();
        long id;
        try {
            var store = new SqliteStore();
            Optional<Integer> rs = SettingManager.getDeltaInterval();

            id = store.insertNewFileRecord(full_path, LocalDateTime.now(), rs.orElse(5));

            if (id == -1) {
                ErrorDialog.showException("Can not insert a file.");
                return;
            }

            FileReader reader = new FileReader(full_path);
            String raw = reader.readAllAsString();
            reader.close();

            byte[] compressed = CompressionUtil.compress(raw);

            store.insertVersion((int) id, false, compressed, null, "First Version");

        } catch (SQLException | IOException e) {
            ErrorDialog.showDevException(e, "Error!!");
            return;
        }

        openMainPanel(full_path, (int) id);

    }

    private void openMainPanel(String full_path, int file_id) {
        Stage new_stage = new Stage();

        FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("main-panel.fxml"));

        Scene scene;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            ErrorDialog.showException("Can not open menu.");
            return;
        }

        if (file_id == -1) return;

        final Path path = Path.of(full_path);
        ((MainPanelController) loader.getController()).onReady(path, file_id, new_stage);
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

    private void sort(
            List<FileRecord> list,
            BiPredicate<FileRecord, FileRecord> shouldSwap
    ) {
        int n = list.size();
        boolean swapped;

        for (int i = 0; i < n - 1; i++) {
            swapped = false;

            for (int j = 0; j < n - i - 1; j++) {
                var a = list.get(j);
                var b = list.get(j + 1);

                if (shouldSwap.test(a, b)) {
                    list.set(j, b);
                    list.set(j + 1, a);
                    swapped = true;
                }
            }

            if (!swapped) break; // optimization
        }
    }
}
