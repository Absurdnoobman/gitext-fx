package sut.project.oop.gitextfx;

import java.time.format.DateTimeFormatter;

public final class AppDateFormat {
    public static final DateTimeFormatter SQLITE_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
}
