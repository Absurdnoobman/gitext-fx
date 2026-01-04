package sut.project.oop.gitextfx.controllers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import sut.project.oop.gitextfx.clazz.ErrorDialog;
import sut.project.oop.gitextfx.clazz.SettingManager;

public class SettingController {
    @FXML
    public CheckBox deleteFileCheckBox;
    @FXML
    public Spinner<Integer> fullVersionIntervalSpinner;

    @FXML
    private void initialize() {
        deleteFileCheckBox.setSelected(SettingManager.getDeleteNewVersionFile().orElse(false));

        fullVersionIntervalSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, SettingManager.getDeltaInterval().orElse(5))
        );

        fullVersionIntervalSpinner.valueProperty()
                .addListener((_, _, newVal) -> {
                    if (newVal != null) {
                        var is_success = SettingManager.setDeltaInterval(newVal);
                        if (!is_success) ErrorDialog.showException("Can not save setting.");
                    }
                });

        deleteFileCheckBox.selectedProperty()
                .addListener((_, _, newVal) -> {
                    if (newVal != null) {
                        var is_success = SettingManager.setDeleteNewVersionFile(newVal);
                        if (!is_success) ErrorDialog.showException("Can not save setting.");
                    }
                });
    }
}
