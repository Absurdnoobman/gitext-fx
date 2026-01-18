module sut.project.oop.gitextfx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.desktop;
    requires java.sql;
    requires javafx.graphics;
    requires atlantafx.base;
    requires io.github.javadiffutils;

    opens sut.project.oop.gitextfx to javafx.fxml;
    exports sut.project.oop.gitextfx;
    opens sut.project.oop.gitextfx.controllers;
    exports sut.project.oop.gitextfx.enums;
    opens sut.project.oop.gitextfx.enums to javafx.fxml;
}