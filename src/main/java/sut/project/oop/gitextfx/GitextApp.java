package sut.project.oop.gitextfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sut.project.oop.gitextfx.clazz.CompressionUtil;
import sut.project.oop.gitextfx.clazz.ErrorDialog;
import sut.project.oop.gitextfx.clazz.Schema;
import sut.project.oop.gitextfx.controllers.WelcomeController;
import sut.project.oop.gitextfx.models.FileRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

public class GitextApp extends Application {
    private void prepareDB(){
        try (var db = new Schema(AppPath.DB_PATH)) {
            db.execute("""
                    CREATE TABLE IF NOT EXISTS Files (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        file_path TEXT NOT NULL,
                        lasted_edit DATETIME,
                        non_delta_interval INTEGER DEFAULT 5
                    );
                    """);
            db.execute("""
                    CREATE TABLE IF NOT EXISTS Versions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        file_id INTEGER REFERENCES Files(id) ,
                        parent_id INTEGER,
                        tag VARCHAR(12) NOT NULL,
                        is_delta BOOLEAN NOT NULL,
                        compressed BLOB NOT NULL,
                        created_at DATETIME NOT NULL
                    );
                    """);

        } catch (SQLException e) {
            var is_dev_mode = Boolean.parseBoolean(System.getProperty("dev"));

            if (is_dev_mode) {
                ErrorDialog.showDevException(e, "cannot execute database operations");
            } else {
                ErrorDialog.showException("cannot execute database operations");
            }
        }
    }

    ///
    ///
    /// @return <code>true</code> if a new folder or a new setting file has been created or the folder already existed
    private boolean prepareFiles() throws IOException {
        var is_setting_exist = Files.exists(Path.of(AppPath.PROP_PATH));
        var is_db_exist = Files.exists(Path.of(AppPath.DB_PATH));

        if (is_setting_exist && is_db_exist) {
            return false;
        }

        if (!Files.exists(Path.of(AppPath.APP_PATH)) && !new File(AppPath.DB_PATH).mkdir()) {
            ErrorDialog.showException("Cannot Create a dir");
            return false;
        }

        var setting = new Properties();
        setting.setProperty("deleteNewVersionFile", "true");
        setting.setProperty("defaultNonDeltaInterval", "5");

        setting.storeToXML(new FileOutputStream(AppPath.PROP_PATH), "new setting file.");

        return true;
    }

    /// Seed Database
    private void seedDB() {
        try (var db = new Schema()){
            db.execute("""
                    DROP TABLE Versions;
                    """);
            db.execute("""
                    DROP TABLE Files;
                    """);

            db.execute("""
                    CREATE TABLE IF NOT EXISTS Files (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        file_path TEXT NOT NULL,
                        lasted_edit DATETIME,
                        non_delta_interval INTEGER DEFAULT 5
                    );
                    """);
            db.execute("""
                    CREATE TABLE IF NOT EXISTS Versions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        file_id INTEGER REFERENCES Files(id) ,
                        parent_id INTEGER,
                        tag VARCHAR(12) NOT NULL,
                        is_delta BOOLEAN NOT NULL,
                        compressed BLOB NOT NULL,
                        created_at DATETIME NOT NULL
                    );
                    """);

            db.execute("""
                    INSERT INTO Files (file_path, lasted_edit, non_delta_interval)
                    VALUES ('C:\\Users\\walter\\Documents\\game_parliament_session.txt', ?, 5);
                    """, LocalDateTime.now());

            FileReader reader = new FileReader("C:\\Users\\walter\\Documents\\game_parliament_session.txt");
            String raw = reader.readAllAsString();
            reader.close();

            byte[] compressed = CompressionUtil.compress(raw);

            var is_success = db.execute("""
                    INSERT INTO Versions (file_id, parent_id, tag, is_delta, compressed, created_at)
                    VALUES ( 1, NULL, ?, FALSE, ?, ?)
                    """,
                    "First version", compressed, LocalDateTime.now().toString()
            );

            if (!is_success) ErrorDialog.showException("Can not insert.");
        } catch (SQLException e) {
            var is_dev_mode = Boolean.parseBoolean(System.getProperty("dev"));

            if (is_dev_mode) {
                ErrorDialog.showDevException(e, "cannot execute database operations");
            } else {
                ErrorDialog.showException("cannot execute database operations");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        try {
            var is_new = prepareFiles();
            if (is_new) prepareDB();
        } catch (IOException e) {
            ErrorDialog.showDevException(e , "Can not the necessary prepare to create app folder and setting file.");
            return;
        }

        seedDB();

        var fxmlLoader = new FXMLLoader(GitextApp.class.getResource("welcome-view.fxml"));
        var scene = new Scene(fxmlLoader.load(), 600, 400);

        ((WelcomeController) fxmlLoader.getController()).stage = stage;

        try (var db = new Schema()){

            ResultSet result = db.table("Files").select("*").get();
            List<FileRecord> files = FileRecord.allFrom(result, FileRecord::new);
            ((WelcomeController) fxmlLoader.getController()).onReady(files);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        stage.setTitle("demo");
        stage.setScene(scene);
        stage.show();

    }
}
