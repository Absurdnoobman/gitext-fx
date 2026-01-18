package sut.project.oop.gitextfx.controllers;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.AppDateFormat;
import sut.project.oop.gitextfx.AppPath;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.clazz.*;
import sut.project.oop.gitextfx.components.cards.VersionCard;
import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MainPanelController {
    @FXML
    public VBox diffContainer;
    @FXML
    public ScrollPane diffScroll;
    private Stage stage;
    @FXML
    public Text filenameLabel;
    @FXML
    public Text fullpathLabel;
    @FXML
    public VBox VersionsList;
    @FXML
    public Label versionValue;
    @FXML
    public Label createdAtValue;
    @FXML
    public Label typeValue;
    @FXML
    public Button deleteVersionBtn;
    @FXML
    public TextField searchVersionField;

    private final List<VersionTag> tags = new ArrayList<>();
    private List<VersionTag> viewTags = new ArrayList<>();

    private Version SelectedVersion = null;

    private long fileId;

    private VersionService versionService;

    private final BooleanProperty isEdited = new SimpleBooleanProperty(false);

    public void onReady(Path filepath, long file_id, Stage stage){
        this.versionService = new VersionService(new SqliteStore());
        this.stage = stage;
        this.fileId = file_id;
        fullpathLabel.setText(filepath.toString());
        filenameLabel.setText(filepath.getFileName().toString());

        tags.clear();

        try {
            var ts = versionService.loadTags((int) fileId);
            tags.addAll(ts);
            viewTags.addAll(ts);
        } catch (Exception e) {
            ErrorDialog.showDevException(e, "Fatal error: fail to load all tags");
        }

        if (tags.isEmpty()) return;

        renderList();

        try {
            renderVersion(tags.getLast().row_id());
        } catch (Exception e) {
            ErrorDialog.showDevException(e, "Can not render version %s".formatted(tags.getLast().tag()));
        }

        searchVersionField.textProperty().addListener((_, _,_) -> onSearchTextFieldTextChanged());

        isEdited.addListener(_ -> updateLastedEdit());

    }

    private void updateLastedEdit() {
        var store = new SqliteStore();
        try {
            store.updateLastedEdit((int) fileId, LocalDateTime.now());
        } catch (SQLException e) {
            ErrorDialog.showException("Can not update lasted edit time.");
        }
    }

    private void onSearchTextFieldTextChanged() {
         viewTags = search(searchVersionField.getText());

         renderList();
    }

    private List<VersionTag> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(tags);
        }

        List<VersionTag> result = new ArrayList<>();

        for (var t : tags) {
            if (t.tag()
                    .toLowerCase()
                    .contains(keyword.toLowerCase())) {
                result.add(t);
            }
        }

        return result;
    }

    private void renderList() {
        if (tags.size() == 1) {
            deleteVersionBtn.setDisable(true);
        } else if (tags.isEmpty()) {
            deleteVersionBtn.setDisable(true);
            return;
        } else {
            deleteVersionBtn.setDisable(false);
        }

        VersionsList.getChildren().clear();

        for (var v: viewTags) {
            var card = new VersionCard(v);

            card.setOnMouseClicked(_ -> {
                try {
                    renderVersion(v.row_id());
                } catch (Exception e) {
                    ErrorDialog.showDevException(e, "Can not render version %s".formatted(v.tag()));
                }
            });

            VersionsList.getChildren().add(card);
        }
    }

    private void renderVersion(int id) throws Exception {
        try {
            var store = new SqliteStore();
            var patch_service = new PatchService();
            var resolver = new VersionResolver(store, patch_service);

            var this_version = store.getVersion(id);

            var history = new VersionHistory(tags);

            var text = CompressionUtil.decompress(this_version.getCompressed());

            var previous = history.findPreviousOf(id);

            if (history.versionCount() <= 1 || previous.isEmpty()) {
                renderDiff(text.lines().toList());

                versionValue.setText(this_version.getTag());
                createdAtValue.setText(this_version.getCreatedAt().format(AppDateFormat.DISPLAY));
                typeValue.setText(this_version.isDelta() ? "Delta" : "Real");
                typeValue.setTooltip(this_version.isDelta()
                        ? new Tooltip("Stores only the changes since the last real version.")
                        : new Tooltip("Stores the entire file as a full version snapshot.")
                );

                SelectedVersion = this_version;

                return;
            }

            String old_text = resolver.resolve((int) fileId, previous.get().row_id());

            Patch<String> patch;
            if (this_version.isDelta()) {
                patch = patch_service.parseFromStr(text);
            } else {
                patch = DiffUtils.diff(old_text.lines().toList(), text.lines().toList());
            }

            var unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                    previous.get().tag(),
                    this_version.getTag(),
                    old_text.lines().toList(),
                    patch,
                    3
            );

            renderDiff(unifiedDiff);

            versionValue.setText(this_version.getTag());
            createdAtValue.setText(this_version.getCreatedAt().format(AppDateFormat.DISPLAY));
            typeValue.setText(this_version.isDelta() ? "Changes" : "Full");

            SelectedVersion = this_version;

        } catch (SQLException | IOException e) {
            ErrorDialog.showDevException(e, "Can not render diff version " + id);
        } catch (PatchFailedException e) {
            ErrorDialog.showDevException(e, "Can not patch");
        }
    }

    private void renderDiff(List<String> lines) {
        diffContainer.getChildren().clear();

        int oldLine = 1;
        int newLine = 1;

        for (String line : lines) {
            Label lineNumber = new Label();
            lineNumber.setMinWidth(45);
            lineNumber.setAlignment(Pos.TOP_RIGHT);
            lineNumber.setStyle("-fx-text-fill: #6a737d; -fx-font-family: 'Consolas';");

            // line number
            if (line.startsWith("+") && !line.startsWith("+++")) {
                lineNumber.setText(String.valueOf(newLine++));
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                lineNumber.setText(String.valueOf(oldLine++));
            } else if (line.startsWith("@@") || line.startsWith("+++") || line.startsWith("---")) {
                lineNumber.setText("");
                oldLine = newLine = 1; // placeholder reset
            } else {
                lineNumber.setText(oldLine + " | " + newLine);
                oldLine++;
                newLine++;
            }

            Text t = new Text(line);
            t.setStyle(get_diff_style(line));

            HBox row = new HBox(8, lineNumber, t);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(0, 4, 0, 4));

            diffContainer.getChildren().add(row);
        }
    }

    private String get_diff_style(String line) {
        if (line.isEmpty()) return "-fx-fill: #24292e; -fx-font-family: 'Consolas';";

        return switch (line.charAt(0)) {
            case '+'-> "-fx-fill: #22863a; -fx-font-family: 'Consolas';";
            case '-' -> "-fx-fill: #b31d28; -fx-font-family: 'Consolas';";
            case '@' -> "-fx-fill: #6f42c1; -fx-font-family: 'Consolas';";
            default -> "-fx-fill: #24292e; -fx-font-family: 'Consolas';";
        };
    }


    @FXML
    private void onNewVersionButtonPressed() {
        var file = chooseSourceFile();
        if (file == null) return;

        var content = readFileContent(file);
        if (content == null) return;

        var new_tag = openTagDialog();
        if (new_tag == null) return;

        try {
            var version_id = versionService.createNewVersion(
                    (int) fileId,
                    tags,
                    content,
                    new_tag
            );
            tags.clear();
            tags.addAll(new SqliteStore().getVersionTagsOf((int) fileId));

            viewTags.clear();
            viewTags.addAll(new SqliteStore().getVersionTagsOf((int) fileId));

            renderList();
            renderVersion(version_id);

            isEdited.set(true);

            var delete_file = SettingManager.getDeleteNewVersionFile().orElse(false);
            if (delete_file) {
                Files.deleteIfExists(file.toPath());
            }

        } catch (Exception e) {
            ErrorDialog.showDevException(e, "Failed to create version.");
        }
    }

    private File chooseSourceFile() {
        var dialog = new FileChooser();
        dialog.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text file", "*.txt")
        );

        try {
            var path = Path.of(fullpathLabel.getText());
            var parent = path.getParent();

            dialog.setInitialDirectory(
                    parent != null && Files.isDirectory(parent)
                            ? parent.toFile()
                            : AppPath.DOCUMENT_FILE
            );
        } catch (InvalidPathException _) { dialog.setInitialDirectory(AppPath.DOCUMENT_FILE); }

        return dialog.showOpenDialog(null);
    }

    private List<String> readFileContent(File file) {
        try (var reader = new FileReader(file)) {
            return reader.readAllLines();
        } catch (IOException e) {
            ErrorDialog.showException("File operation error.");
            return null;
        }
    }

    private String openTagDialog() {
        while (true) {
            var dialog = new TextInputDialog();
            dialog.initOwner(stage);
            dialog.setTitle("New Version Tag");
            dialog.setHeaderText(null);
            dialog.setContentText("Enter a tag (max 24 characters):");

            var input = dialog.showAndWait();
            if (input.isEmpty()) return null;

            var tag = input.get();

            if (tag.isBlank()) {
                ErrorDialog.showException("Tag cannot be empty.", "Invalid Tag");
                continue;
            }

            if (tag.length() > 24) {
                ErrorDialog.showException("Tag must not exceed 24 characters.", "Invalid Tag");
                continue;
            }

            return tag;
        }
    }


    @FXML
    private void onDeleteVersionButtonPressed() {
        try {
            versionService.deleteVersion(SelectedVersion);

            var updated_tags = versionService.loadTags((int) fileId);

            tags.clear();

            tags.addAll(updated_tags);

            tags.removeIf(tag -> tag.row_id() == SelectedVersion.getId());
            viewTags.removeIf(tag -> tag.row_id() == SelectedVersion.getId());

            renderList();
            renderVersion(tags.getLast().row_id());

            isEdited.set(true);

        } catch (Exception e) {
            ErrorDialog.showDevException(e, "failed.");
        }
    }

    @FXML
    private void onUseThisVersionButtonPressed() {
        try {
            Path path = chooseSavePath();
            if (path == null) return;

            VersionResolver resolver = new VersionResolver(new SqliteStore(), new PatchService());

            String content = resolver.resolve((int) fileId, SelectedVersion.getId());

            Files.writeString(path, content);
            showSuccess();

        } catch (Exception e) {
            ErrorDialog.showDevException(e, "Failed to load version.");
        }
    }

    private Path chooseSavePath() {
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Save a file");

        var old_path = Path.of(fullpathLabel.getText());
        var is_parent_exist = Files.exists(old_path);

        dialog.setInitialDirectory(is_parent_exist ? old_path.getParent().toFile() : AppPath.DOCUMENT_FILE);
        dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        dialog.setInitialFileName(is_parent_exist ? old_path.getFileName().toString() : "%s.txt".formatted(versionValue.getText()));

        File file = dialog.showSaveDialog(stage);
        return file == null ? null : file.toPath();
    }

    private void showSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Success!");
        alert.initOwner(stage);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onAddFilePressed(){
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

        Stage new_stage = new Stage();
        FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("main-panel.fxml"));

        Scene scene;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            ErrorDialog.showException("Can not open menu.");
            return;
        }

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

    @FXML
    private void onOpenFileListMenuPressed() {
        try {
            var store = new SqliteStore();
            var files = store.getAllFileRecords();

            Stage new_stage = new Stage();
            FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("welcome-view.fxml"));
            var scene = new Scene(loader.load(), 800, 600);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                                    GitextApp.class.getResource("file-card.css"))
                            .toExternalForm()
            );

            ((WelcomeController) loader.getController()).onReady(files, new_stage);

            new_stage.setTitle("Welcome");
            new_stage.setScene(scene);
            new_stage.show();

            stage.close();

        } catch (SQLException | IOException e) {
            ErrorDialog.showDevException(e, "Can not open a list.");
        }
    }

    @FXML
    private void onDeleteFileButtonPressed() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to delete all version history of this file.");
        var result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        if (Objects.requireNonNull(result.get().getButtonData()) == ButtonBar.ButtonData.OK_DONE) {
            delete_all_record();
            onOpenFileListMenuPressed();
        }
    }

    private void delete_all_record() {
        try {
            var store = new SqliteStore();

            for (var tag: tags) {
                store.deleteFileRecord(tag.row_id());
            }

            store.deleteFileRecord((int) fileId);

        } catch (SQLException e) {
            ErrorDialog.showDevException(e, "Can not delete a record.");
        }

    }
}
