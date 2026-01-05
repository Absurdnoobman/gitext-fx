package sut.project.oop.gitextfx.components.cards;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import sut.project.oop.gitextfx.models.VersionTag;

public class VersionCard extends HBox {
    public VersionCard(VersionTag tag) {
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);

        getStyleClass().add("version-card");

        Label tagLabel = new Label(tag.tag());
        tagLabel.getStyleClass().add("version-card-label");

        getChildren().add(tagLabel);
    }
}
