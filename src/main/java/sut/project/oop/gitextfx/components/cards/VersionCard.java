package sut.project.oop.gitextfx.components.cards;

import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import sut.project.oop.gitextfx.models.VersionTag;

public class VersionCard extends HBox {
    public VersionCard(VersionTag tag) {
        setPadding(new Insets(2));

        Text txt = new Text(tag.tag());

        getChildren().addAll(txt);
    }
}
