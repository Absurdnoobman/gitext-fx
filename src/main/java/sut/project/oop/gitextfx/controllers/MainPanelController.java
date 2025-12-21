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
import sut.project.oop.gitextfx.clazz.CompressionUtil;
import sut.project.oop.gitextfx.clazz.ErrorDialog;
import sut.project.oop.gitextfx.clazz.Schema;
import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    public void onReady(Path filepath, long file_id, Stage stage){
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
        VersionsList.getChildren().clear();

        for (var v: tags) {
            var txt = new Text(v.tag());
            txt.setOnMouseClicked(_ -> {
                renderVersion(v.row_id());
            });
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

            String text = CompressionUtil.decompressed(this_version.getCompressed());

            if (!this_version.isDelta()) { // is non-delta version (major)
                unifiedDiffArea.setText(text);
            } else {
                var last_major_tag = tags.stream().filter(t -> !t.is_delta() ).toList().getLast();
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

                    String patch_decompressed = CompressionUtil.decompressed(compressed);

                    rs.close();

                    rs = db.table("Versions")
                            .select("compressed")
                            .where("file_id", "=", fileId)
                            .where("id", "=", last_major_tag.row_id())
                            .get();

                    compressed = rs.getBytes(1);

                    String major_content = CompressionUtil.decompressed(compressed);

                    Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(
                            Arrays.asList(patch_decompressed.split("\n", -1))
                    );

                    List<String> major_lines = major_content.lines().toList();

                    List<String> previous_content_lines = patch.applyTo(major_lines);

                    var this_version_content = CompressionUtil.decompressed(this_version.getCompressed());
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
            ErrorDialog.showException("Can not render diff version " + id);
        } catch (PatchFailedException e) {
            ErrorDialog.showException("Can not patch");
        }
    }


    @FXML
    private void onNewVersionButtonPressed() {
        FileChooser dialog = new FileChooser();
        dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        File result = dialog.showOpenDialog(null);

        if (result == null) return;

        List<String> content;

        try (FileReader reader = new FileReader(result)) {
            content = reader.readAllLines();
        } catch (FileNotFoundException e) {
            ErrorDialog.showException("File not found.");
            return;
        } catch (IOException e) {
            ErrorDialog.showException("File operation error.");
            return;
        }

        if (content == null) {
            return;
        }

        String new_tag;
        while (true) {
            TextInputDialog tag_name_dialog = new TextInputDialog();
            tag_name_dialog.initOwner(stage);
            tag_name_dialog.setTitle("New Version Tag");
            tag_name_dialog.setHeaderText(null);
            tag_name_dialog.setContentText("Enter a tag (max 24 characters):");

            Optional<String> input = tag_name_dialog.showAndWait();

            // User pressed Cancel or closed dialog
            if (input.isEmpty()) {
                new_tag = null;
                break;
            }

            if (input.get().isBlank()) {
                ErrorDialog.showException("Tag cannot be empty.","Invalid Tag");
                continue;
            }

            if (input.get().length() > 24) {
                ErrorDialog.showException("Tag must not exceed 24 characters.", "Invalid Tag");
                continue;
            }

            new_tag = input.get();
            break;
        }

        VersionTag last_major_tag = tags.stream().filter(tag -> !tag.is_delta() ).toList().getLast();

        long inserted_id;
        try(var db = new Schema()){
            ResultSet major = db.query()
                    .select("compressed")
                    .from("Versions")
                    .where("file_id", "=", fileId)
                    .where("id", "=", last_major_tag.row_id())
                    .get();

            byte[] compressed = major.getBytes("compressed");

            String major_content = CompressionUtil.decompressed(compressed);

            List<String> major_lines = major_content.lines().toList();

            Patch<String> patch = DiffUtils.diff(major_lines, content);

            List<String> unified_diff = UnifiedDiffUtils.generateUnifiedDiff(
                    "old",
                    "new",
                    major_lines,
                    patch,
                    3
            );

            String unified_diff_str = String.join("\n", unified_diff);
            byte[] diff_compressed = CompressionUtil.compress(unified_diff_str);

            var distance = tags.stream()
                    .filter(tag -> tag.row_id() > last_major_tag.row_id())
                    .count();

            var interval = db.table("Files")
                    .select("non_delta_interval")
                    .where("id", "=", fileId)
                    .get().getInt(1);

            boolean is_delta = distance <= interval - 1; // false if this new version is a Major version

            inserted_id = db.insertAndReturnID("""
                    INSERT INTO Versions (file_id, is_delta, compressed, parent_id, tag)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    fileId , is_delta, diff_compressed, last_major_tag.row_id(), new_tag
            );

            ResultSet inserted = db.table("Versions")
                    .select("tag", "is_delta")
                    .where("id", "=", inserted_id)
                    .where("file_id", "=", fileId)
                    .get();

            tags.add(new VersionTag((int) inserted_id, inserted.getString("tag"), inserted.getBoolean("is_delta")));

        } catch (SQLException e) {
            ErrorDialog.showDevException(e, "Database Operation Error.");
            return;
        } catch (IOException e) {
            ErrorDialog.showException("File error.");
            return;
        }

        if (inserted_id == -1) {
            return;
        }

        renderList();

        renderVersion(inserted_id);

    }

    @FXML
    private void onDeleteVersionButtonPressed() {
        /* TODO */
    }

    @FXML
    private void onUseThisVersionButtonPressed() {
        VersionTag last_major_tag = tags.stream().filter(tag -> !tag.is_delta() ).toList().getLast();

        Path old_path = null;
        boolean is_parent_exist = false;
        try (var db = new Schema()) {
            ResultSet rs = db.table("Files").select("file_path").where("id", "=", fileId).get();
            String path_str = rs.getString(1);
            old_path = Path.of(path_str);
            is_parent_exist = Files.exists(old_path.getParent());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Can not get the old file path for some reason.", "Warning", JOptionPane.WARNING_MESSAGE);
        }

        FileChooser dialog = new FileChooser();
        dialog.setTitle("Save a file");
        dialog.setInitialDirectory(old_path == null || !is_parent_exist ? AppPath.DOCUMENT_FILE : new File(old_path.getParent().toString()));
        dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter("text file", "*.txt"));
        dialog.setInitialFileName(old_path == null || !is_parent_exist ? "%s.txt".formatted(versionValue.getText()) : old_path.getFileName().toString());

        var file  = dialog.showSaveDialog(null);

        if (file == null) {
            return;
        }

        String result_content;
        try (var db = new Schema()){
            ResultSet rs = db.table("Versions")
                    .select("compressed")
                    .where("file_id", "=", fileId)
                    .where("id", "=", last_major_tag.row_id())
                    .get();

            var compressed = rs.getBytes(1);
            String last_major_content = CompressionUtil.decompressed(compressed);

            rs = db.table("Versions")
                    .select("compressed")
                    .where("file_id", "=", fileId)
                    .where("id", "=", SelectedVersion.getId())
                    .get();

            compressed = rs.getBytes(1);
            String selected_version = CompressionUtil.decompressed(compressed);

            if (SelectedVersion.isDelta()) {
                Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(
                        Arrays.asList(selected_version.split("\n", -1))
                );

                List<String> result = patch.applyTo(last_major_content.lines().toList());

                result_content = String.join("\n", result);
            } else {
                result_content = selected_version;
            }

        } catch (SQLException e) {
            ErrorDialog.showException("Database operation error.");
            return;
        } catch (IOException e) {
            ErrorDialog.showException("Decompression error.");
            return;
        } catch (PatchFailedException e) {
            ErrorDialog.showException("Can not apply patch");
            return;
        }

        try {
            final boolean newFile = file.createNewFile(); // newFile is not used.
        } catch (IOException e) {
            ErrorDialog.showDevException(e, "File system error.");
            return;
        }

        try (var writer = new FileWriter(file)) {
            writer.write(result_content);
        } catch (IOException e) {
            ErrorDialog.showDevException(e, "Fail to write a content to the new file.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Success!");
        alert.initOwner(stage);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
