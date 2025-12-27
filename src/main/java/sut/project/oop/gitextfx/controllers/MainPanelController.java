package sut.project.oop.gitextfx.controllers;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.AppDateFormat;
import sut.project.oop.gitextfx.AppPath;
import sut.project.oop.gitextfx.clazz.*;
import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
        this.versionService = new VersionService(new SqliteVersionStore());
        this.stage = stage;
        this.fileId = file_id;
        fullpathLabel.setText(filepath.toString());
        filenameLabel.setText(filepath.getFileName().toString());

        try (var db = new Schema()) {
            ResultSet result = db.query()
                    .select("id", "tag", "is_delta")
                    .from("Versions")
                    .where("file_id", "=", fileId)
                    .get();

            while (result.next()) {
                tags.add(new VersionTag( result.getInt("id") , result.getString("tag"), result.getBoolean("is_delta") ));
            }
        } catch (SQLException e) {
            ErrorDialog.showDevException(e, "Error!!!");
        }

        renderList();

        renderVersion(tags.getLast().row_id());

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
            txt.setOnMouseClicked(_ -> renderVersion(v.row_id()));
            VersionsList.getChildren().add(txt);
        }
    }

    private void renderVersion(long id) {
        try (var db = new Schema()) {
            ResultSet result = db.query()
                    .from("Versions")
                    .where("id", "=", id)
                    .where("file_id", "=", fileId)
                    .get();

            Version this_version = Version.from(result, Version::new);

            String text = CompressionUtil.decompress(this_version.getCompressed());

            if (!this_version.isDelta()) { // is non-delta version (major)
                unifiedDiffArea.setText(text);
            } else {
                var last_major_tag = tags.stream().filter(t -> !t.is_delta() && t.row_id() < id ).toList().getLast();
                var interval = tags.stream().filter(t -> t.row_id() > last_major_tag.row_id() && t.row_id() < id).toList();

                if (interval.size() <= 1) { // next to non-delta version
                    unifiedDiffArea.setText(text);
                } else {
                    var previous_tag = interval.getLast();

                    ResultSet rs = db.table("Versions")
                            .select("compressed")
                            .where("file_id", "=", fileId)
                            .where("id", "=", previous_tag.row_id())
                            .get();

                    byte[] compressed = rs.getBytes(1);

                    String patch_decompressed = CompressionUtil.decompress(compressed);

                    rs.close();

                    rs = db.table("Versions")
                            .select("compressed")
                            .where("file_id", "=", fileId)
                            .where("id", "=", last_major_tag.row_id())
                            .get();

                    compressed = rs.getBytes(1);

                    String major_content = CompressionUtil.decompress(compressed);

                    Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(
                            Arrays.asList(patch_decompressed.split("\n", -1))
                    );

                    List<String> major_lines = major_content.lines().toList();

                    List<String> previous_content_lines = patch.applyTo(major_lines);

                    var this_version_content = CompressionUtil.decompress(this_version.getCompressed());
                    List<String> this_version_lines = this_version_content.lines().toList();

                    patch = DiffUtils.diff(previous_content_lines, this_version_lines);

                    List<String> unified_diff = UnifiedDiffUtils.generateUnifiedDiff(
                            "old",
                            "new",
                            previous_content_lines,
                            patch,
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


//    @FXML
//    private void onNewVersionButtonPressed() {
//        FileChooser dialog = new FileChooser();
//        dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
//
//        try {
//            Path path = Path.of(fullpathLabel.getText());
//            Path parent = path.getParent();
//
//            if (parent != null && Files.isDirectory(parent)) dialog.setInitialDirectory(parent.toFile());
//            else dialog.setInitialDirectory(AppPath.DOCUMENT_FILE);
//
//        } catch (InvalidPathException _) {
//            dialog.setInitialDirectory(AppPath.DOCUMENT_FILE);
//        }
//
//        File result = dialog.showOpenDialog(null);
//
//        if (result == null) return;
//
//        List<String> content;
//
//        try (FileReader reader = new FileReader(result)) {
//            content = reader.readAllLines();
//        } catch (FileNotFoundException e) {
//            ErrorDialog.showException("File not found.");
//            return;
//        } catch (IOException e) {
//            ErrorDialog.showException("File operation error.");
//            return;
//        }
//
//        if (content == null) {
//            return;
//        }
//
//        String new_tag;
//        while (true) {
//            TextInputDialog tag_name_dialog = new TextInputDialog();
//            tag_name_dialog.initOwner(stage);
//            tag_name_dialog.setTitle("New Version Tag");
//            tag_name_dialog.setHeaderText(null);
//            tag_name_dialog.setContentText("Enter a tag (max 24 characters):");
//
//            Optional<String> input = tag_name_dialog.showAndWait();
//
//            // User pressed Cancel or closed dialog
//            if (input.isEmpty()) {
//                new_tag = null;
//                break;
//            }
//
//            if (input.get().isBlank()) {
//                ErrorDialog.showException("Tag cannot be empty.","Invalid Tag");
//                continue;
//            }
//
//            if (input.get().length() > 24) {
//                ErrorDialog.showException("Tag must not exceed 24 characters.", "Invalid Tag");
//                continue;
//            }
//
//            new_tag = input.get();
//            break;
//        }
//
//        if (new_tag == null) return;
//
//        VersionTag last_major_tag = tags.stream().filter(tag -> !tag.is_delta() ).toList().getLast();
//
//        long inserted_id;
//        byte[] compressed;
//
//        boolean is_delta;
//        try(var db = new Schema()) {
//            var distance = tags.stream()
//                    .filter(tag -> tag.row_id() > last_major_tag.row_id())
//                    .count();
//
//            var interval = db.table("Files")
//                    .select("non_delta_interval")
//                    .where("id", "=", fileId)
//                    .get().getInt(1);
//            is_delta = distance < interval - 1; // false if this new version is a Major version
//        } catch (SQLException e) {
//            ErrorDialog.showDevException(e, "Can not query.");
//            return;
//        }
//
//        try(var db = new Schema()){
//            if (is_delta) {
//                ResultSet major = db.query()
//                        .select("compressed")
//                        .from("Versions")
//                        .where("id", "=", last_major_tag.row_id())
//                        .get();
//
//                compressed = major.getBytes("compressed");
//
//                String major_content = CompressionUtil.decompress(compressed);
//
//                List<String> major_lines = major_content.lines().toList();
//
//                Patch<String> patch = DiffUtils.diff(major_lines, content);
//
//                List<String> unified_diff = UnifiedDiffUtils.generateUnifiedDiff(
//                        "old",
//                        "new",
//                        major_lines,
//                        patch,
//                        3
//                );
//
//                major.close();
//
//                String unified_diff_str = String.join("\n", unified_diff);
//                compressed = CompressionUtil.compress(unified_diff_str);
//            } else {
//                compressed = CompressionUtil.compress(String.join("\n", content));
//            }
//
//        } catch (SQLException e) {
//            ErrorDialog.showDevException(e, "Database Operation Error.");
//            return;
//        } catch (IOException e) {
//            ErrorDialog.showException("File error.");
//            return;
//        }
//
//        try (var db = new Schema()) {
//            inserted_id = db.insertAndReturnID("""
//                    INSERT INTO Versions (file_id, is_delta, compressed, parent_id, tag)
//                    VALUES (?, ?, ?, ?, ?)
//                    """,
//                    fileId , is_delta, compressed, is_delta ? last_major_tag.row_id() : null, new_tag
//            );
//        } catch (SQLException e) {
//            ErrorDialog.showDevException(e, "Can not execute database operations.");
//            return;
//        }
//
//        try (var db = new Schema()) {
//            ResultSet inserted = db.table("Versions")
//                    .select("tag", "is_delta")
//                    .where("id", "=", inserted_id)
//                    .where("file_id", "=", fileId)
//                    .get();
//
//            tags.add(new VersionTag(
//                    (int) inserted_id,
//                    inserted.getString("tag"),
//                    inserted.getBoolean("is_delta")
//            ));
//        } catch (SQLException e) {
//            ErrorDialog.showDevException(e, "Can not execute database operation.");
//        }
//
//
//        if (inserted_id == -1) {
//            return;
//        }
//
//        renderList();
//
//        renderVersion(inserted_id);
//
//    }

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
        if (SelectedVersion.getParentId() == null && !SelectedVersion.isDelta()) {
            List<Integer> dependent_ids = new Vector<>();
            try (var db = new Schema()) {
                ResultSet rs = db.query()
                        .select("id")
                        .from("Versions")
                        .where("file_id", "=", fileId)
                        .where("parent_id", "=", SelectedVersion.getId())
                        .get();

                while (rs.next()) {
                    dependent_ids.add(rs.getInt(1));
                }

                rs.close();
            } catch (SQLException e) {
                ErrorDialog.showDevException(e, "");
                return;
            }

            if (dependent_ids.isEmpty()) {

                try (var db = new Schema()) {
                    db.execute("DELETE FROM Versions WHERE id = ?", SelectedVersion.getId());
                } catch (SQLException e) {
                    ErrorDialog.showDevException(e, "Can not delete.");
                    return;
                }


                tags.removeIf(tag -> tag.row_id() == SelectedVersion.getId());

                renderList();

                renderVersion(tags.getLast().row_id());

                return;
            }

            // bubble Sort (Required By Rubric)
            for (int round = 1; round <= dependent_ids.size() - 1; round++) {
                for (int i = 0; i < dependent_ids.size() - round; i++) {
                    if (dependent_ids.get(i) > dependent_ids.get(i + 1)) {
                        var temp = dependent_ids.get(i);
                        dependent_ids.set(i, dependent_ids.get(i + 1));
                        dependent_ids.set(i + 1, temp);
                    }
                }
            }

                var major_elected_id = dependent_ids.getFirst(); // the first delta that depend (base) on this deleted major.

            byte[] compressed;
            try (var db = new Schema()) {
                compressed = db.table("Versions")
                        .select("compressed")
                        .where("id", "=", major_elected_id)
                        .get().getBytes(1);
            } catch (SQLException e) {
               ErrorDialog.showDevException(e, "");
               return;
            }

            if (compressed == null) {
                return;
            }

            List<String> new_content;
            try {
                var diff = CompressionUtil.decompress(compressed);
                Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(Arrays.asList(diff.split("\n", -1)));

                var content = CompressionUtil.decompress(SelectedVersion.getCompressed());
                new_content = DiffUtils.patch(content.lines().toList(), patch);
            } catch (PatchFailedException | IOException e) {
                ErrorDialog.showDevException(e, "Operational Error!");
                return;
            }

            try (var db = new Schema()) {
                db.execute(
                        "UPDATE Versions SET parent_id = null, compressed = ?, is_delta = ? WHERE id = ?",
                        CompressionUtil.compress(String.join("\n", new_content)),
                        false,
                        major_elected_id
                );

                dependent_ids.removeFirst();

                for (var id: dependent_ids) {
                    db.execute("UPDATE Versions SET parent_id = ? WHERE id = ?", major_elected_id, id);
                }
            } catch (SQLException e) {
                ErrorDialog.showDevException(e, "Can not update.");
                return;
            } catch (IOException e) {
                ErrorDialog.showDevException(e, "IO Error!");
            }

            try (var db = new Schema()) {
                db.execute("DELETE FROM Versions WHERE id = ?", SelectedVersion.getId());
            } catch (SQLException e) {
                ErrorDialog.showDevException(e, "Can not delete.");
                return;
            }


            try (var db = new Schema()) {
                var updated_tags = db.query()
                        .select("id", "tag", "is_delta")
                        .from("Versions")
                        .where("file_id", "=", fileId)
                        .get();

                tags.clear();

                while (updated_tags.next()) {
                    tags.add(new VersionTag( updated_tags.getInt("id") , updated_tags.getString("tag"), updated_tags.getBoolean("is_delta") ));
                }


            } catch (SQLException e) {
                ErrorDialog.showDevException(e, "Can not query");
                return;
            }

            renderList();
            renderVersion(major_elected_id);

        } else {
            try (var db = new Schema()) {
                db.execute("DELETE FROM Versions WHERE id = ?", SelectedVersion.getId());
            } catch (SQLException e) {
                ErrorDialog.showDevException(e, "Can not delete.");
                return;
            }

            tags.removeIf(tag -> tag.row_id() == SelectedVersion.getId());
            renderList();
            renderVersion(tags.getLast().row_id());
        }


    }

    @FXML
    private void onUseThisVersionButtonPressed() {
        try {
            Path path = chooseSavePath();
            if (path == null) return;

            VersionResolver resolver = new VersionResolver(new SqliteVersionStore(), new PatchApplier());

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

}
