package sut.project.oop.gitextfx;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Path;

final public class AppPath {
    public static final String APP_PATH = System.getProperty("user.home") + "/.gitext";
    public static final String DB_PATH = Path.of(APP_PATH + "/app_db.db").toString();
    public static final File DOCUMENT_FILE = FileSystemView.getFileSystemView().getDefaultDirectory();
    public static final String PROP_PATH = Path.of(APP_PATH + "/setting.xml").toString();
}
