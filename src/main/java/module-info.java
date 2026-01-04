module sut.project.oop.gitextfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires java.sql;
    requires javafx.graphics;
    requires io.github.javadiffutils;
    requires atlantafx.base;

    opens sut.project.oop.gitextfx to javafx.fxml;
    exports sut.project.oop.gitextfx;
    opens sut.project.oop.gitextfx.controllers;
    exports sut.project.oop.gitextfx.enums;
    opens sut.project.oop.gitextfx.enums to javafx.fxml;
}