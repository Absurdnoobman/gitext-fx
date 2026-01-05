package sut.project.oop.gitextfx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.clazz.CompressionUtil;
import sut.project.oop.gitextfx.clazz.ErrorDialog;
import sut.project.oop.gitextfx.clazz.SettingManager;
import sut.project.oop.gitextfx.clazz.SqliteStore;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

public class WelcomeController {
    private Stage stage;
    @FXML
    public ComboBox<UIeditSortOrder> sortComboBox;
    @FXML
    public TextField searchTextField;

    @FXML
    private VBox FileListVBox;

    private List<FileRecord> originalFiles = new ArrayList<>();
    private List<FileRecord> viewFiles = new ArrayList<>();

    public Stage getStage() {return stage;}

    public void onReady(List<FileRecord> file_result, Stage stage) {
        this.stage = stage;
        FileListVBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        FileListVBox.setFillWidth(true);

        sortComboBox.getItems().setAll(
                UIeditSortOrder.LAST_EDIT_NEWEST,
                UIeditSortOrder.LAST_EDIT_OLDEST
        );

        sortComboBox.setValue(UIeditSortOrder.LAST_EDIT_NEWEST);

        searchTextField.textProperty().addListener((_, _, _) -> updateView());

        sortComboBox.valueProperty().addListener((_, _, _) -> updateView());

        originalFiles = file_result;
        viewFiles.addAll(file_result);

        updateView();
    }

    public void queryOriginalFileRecords() {
        try {
            var store = new SqliteStore();
            var rs = store.getAllFileRecords();
            originalFiles = rs;
            viewFiles.clear();
            viewFiles.addAll(rs);
        } catch (SQLException e) {
            ErrorDialog.showDevException(e, "Can not get file records.");
            return;
        }

        updateView();
    }

    private void updateView() {
        viewFiles = search(searchTextField.getText());

        switch (sortComboBox.getValue()) {
            case LAST_EDIT_NEWEST -> sortByLastEditNewest();
            case LAST_EDIT_OLDEST -> sortByLastEditOldest();
        }

        refreshFileList(viewFiles);
    }

    private void sortByLastEditNewest() {
        sort(originalFiles,
                (a, b) -> a.getLastedEdit().isBefore(b.getLastedEdit())
        );
    }

    private void sortByLastEditOldest() {
        sort(originalFiles,
                (a, b) -> a.getLastedEdit().isAfter(b.getLastedEdit())
        );
    }

    private List<FileRecord> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(originalFiles);
        }

        List<FileRecord> result = new ArrayList<>();

        for (var file : originalFiles) {
            if (file.getFilePath()
                    .toLowerCase()
                    .contains(keyword.toLowerCase())) {
                result.add(file);
            }
        }

        return result;
    }

    private void refreshFileList(List<FileRecord> ls) {
        FileListVBox.getChildren().clear();

        for (int i = 0; i < ls.size(); i++) {
            FileListVBox.getChildren().add(
                    new FileCard(i + 1, ls.get(i), this)
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

        scene.getStylesheets().add(
                Objects.requireNonNull(
                                GitextApp.class.getResource("version-card.css"))
                        .toExternalForm()
        );

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

    private void sort(List<FileRecord> list, BiPredicate<FileRecord, FileRecord> shouldSwap) {
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

            if (!swapped) break;
        }
    }
}
