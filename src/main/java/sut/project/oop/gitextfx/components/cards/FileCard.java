package sut.project.oop.gitextfx.components.cards;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.AppDateFormat;
import sut.project.oop.gitextfx.GitextApp;
import sut.project.oop.gitextfx.controllers.MainPanelController;
import sut.project.oop.gitextfx.models.File;

import java.io.IOException;
import java.nio.file.Path;

public class FileCard extends HBox {
    public FileCard(int index, File file, Stage old_stage){
        this.setSpacing(2);
        this.setPadding(new Insets(4));
        this.setMaxWidth(Double.MAX_VALUE);

        // Create index label
        Text indexLabel = new Text("%d.".formatted(index));
        indexLabel.setTextAlignment(TextAlignment.CENTER);

        // Create index div
        HBox indexDiv = new HBox(indexLabel);
        indexDiv.setAlignment(Pos.CENTER);
        indexDiv.setPadding(new Insets(2));

        // Create file label and file box
        Text fileLabel = new Text(file.getFilePath());
        fileLabel.setTextAlignment(TextAlignment.CENTER);
        HBox fileBox = new HBox(fileLabel);
        fileBox.setAlignment(Pos.CENTER);

        // Create lasted edit date
        Text lastedEditTxt = new Text("(Lasted Edit %s)".formatted(file.getLastedEdit().format(AppDateFormat.DISPLAY)));
        lastedEditTxt.setTextAlignment(TextAlignment.CENTER);

        HBox dateBox = new HBox(lastedEditTxt);
        dateBox.setAlignment(Pos.CENTER);

        // Create spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create manage button
        Button manageButton = new Button("Manage");

        HBox.setHgrow(manageButton, Priority.NEVER);

        manageButton.setOnAction(_ -> {
            Stage new_stage = new Stage();
            FXMLLoader loader = new FXMLLoader(GitextApp.class.getResource("main-panel.fxml"));

            Scene scene = null;
            try {
                scene = new Scene(loader.load());
            } catch (IOException e) {
                return;
            }

            ( (MainPanelController) loader.getController()).onReady(Path.of(file.getFilePath()), file.getId(), new_stage);
            new_stage.setTitle("File: %s".formatted(Path.of(file.getFilePath()).getFileName()));
            new_stage.setScene(scene);
            new_stage.show();

            old_stage.close();
        });

        getChildren().addAll(indexDiv, fileBox, dateBox, spacer, manageButton);
    }
}
