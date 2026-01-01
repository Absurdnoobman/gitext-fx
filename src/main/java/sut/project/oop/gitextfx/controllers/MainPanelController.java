package sut.project.oop.gitextfx.controllers;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.PatchFailedException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.AppDateFormat;
import sut.project.oop.gitextfx.AppPath;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.clazz.*;
import sut.project.oop.gitextfx.models.FileRecord;
import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class MainPanelController {
    private Stage stage;
    @FXML
    public Text filenameLabel;
    @FXML
    public Text fullpathLabel;
    @FXML
    public VBox VersionsList;
    @FXML
    public TextArea unifiedDiffArea;
    @FXML
    public Label versionValue;
    @FXML
    public Label createdAtValue;
    @FXML
    public Label typeValue;
    @FXML
    public Button deleteVersionBtn;
    private final List<VersionTag> tags = new ArrayList<>();

    private Version SelectedVersion = null;

    private long fileId;

    private VersionService versionService;

    public void onReady(Path filepath, long file_id, Stage stage){
        this.versionService = new VersionService(new SqliteStore());
        this.stage = stage;
        this.fileId = file_id;
        fullpathLabel.setText(filepath.toString());
        filenameLabel.setText(filepath.getFileName().toString());

        tags.clear();
        try {
            tags.addAll(versionService.loadTags((int) fileId));
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

        for (var v: tags) {
            var txt = new Text(v.tag());
            txt.setOnMouseClicked(_ -> {
                try {
                    renderVersion(v.row_id());
                } catch (Exception e) {
                    ErrorDialog.showDevException(e, "Can not render version %s".formatted(v.tag()));
                }
            });
            VersionsList.getChildren().add(txt);
        }
    }

    private void renderVersion(int id) throws Exception {
        try {
            var this_version = new SqliteStore().getVersion(id);

            var history = new VersionHistory(tags);

            String text = CompressionUtil.decompress(this_version.getCompressed());

            if (!this_version.isDelta()) { // is non-delta version (major)
                unifiedDiffArea.setText(text);
            } else {
                var last_major_tag = history.findBaseOf(id);
                var interval = history.findDeltas(last_major_tag.row_id(), id);

                if (interval.size() <= 1) { // next to non-delta version
                    unifiedDiffArea.setText(text);
                } else {
                    var previous_version = interval.getLast();

                    VersionResolver resolver = new VersionResolver(new SqliteStore(), new PatchService());

                    var previous_version_content = resolver.resolve((int) fileId, previous_version.row_id());

                    var unified_diff = UnifiedDiffUtils.generateUnifiedDiff(
                            "old", "new",
                            previous_version_content.lines().toList(),
                            new PatchService().parseFromStr(new SqliteStore().load((int) fileId, id)),
                            3
                    );

                    unifiedDiffArea.setText(String.join("\n", unified_diff));
                }

            }
            versionValue.setText(this_version.getTag());
            createdAtValue.setText(this_version.getCreatedAt().format(AppDateFormat.DISPLAY));
            typeValue.setText(this_version.isDelta() ? "Delta" : "Real");

            SelectedVersion = this_version;

        } catch (SQLException | IOException e) {
            ErrorDialog.showDevException(e, "Can not render diff version " + id);
        } catch (PatchFailedException e) {
            ErrorDialog.showDevException(e, "Can not patch");
        }
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

            renderList();
            renderVersion(version_id);

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

            renderList();
            renderVersion(tags.getLast().row_id());

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
        long id = -1;
        try (var db = new Schema()) {
            id = db.insertAndReturnID("""
                    INSERT INTO Files (file_path) VALUES (?)
                    """, full_path);

            if (id == -1) {
                ErrorDialog.showException("Can not insert a file.");
                return;
            }

            var bos = new ByteArrayOutputStream();
            var gzip = new GZIPOutputStream(bos);

            FileReader reader = new FileReader(full_path);
            String raw = reader.readAllAsString();
            byte[] data = raw.getBytes(StandardCharsets.UTF_8);

            gzip.write(data);
            gzip.close();

            byte[] compressed = bos.toByteArray();

            var is_success = db.execute("""
                    INSERT INTO Versions (file_id, parent_id, tag, is_delta, compressed, created_at)
                    VALUES ( ?, NULL, 'First version', FALSE, ? )
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

    @FXML
    private void onOpenFileListMenuPressed() {
        try {
            var store = new SqliteStore();
            var files = store.getAllFileRecords();

            Stage new_stage = new Stage();
            FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("welcome-view.fxml"));
            var scene = new Scene(loader.load(), 600, 400);

            ((WelcomeController) loader.getController()).stage = new_stage;
            ((WelcomeController) loader.getController()).onReady(files);

            new_stage.setTitle("Welcome");
            new_stage.setScene(scene);
            new_stage.show();

            stage.close();

        } catch (SQLException | IOException e) {
            ErrorDialog.showDevException(e, "Can not open a list.");
        }
    }
}
