package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.AppPath;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public class SettingManager {
    private static String read(String key, String default_val) throws IOException {
        Properties settings = new Properties();
        settings.loadFromXML(new FileInputStream(AppPath.PROP_PATH));
        return settings.getProperty(key, default_val);
    }

    public static Optional<Integer> getDeltaInterval() {
        try {
            var result = read("defaultNonDeltaInterval", String.valueOf(5));
            var result_i = Integer.parseInt(result);

            return Optional.of(result_i);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static boolean setDeltaInterval(int new_value) {
        Properties settings = new Properties();
        settings.setProperty("defaultNonDeltaInterval", String.valueOf(new_value));
        try {
            settings.storeToXML(new FileOutputStream(AppPath.PROP_PATH), LocalDateTime.now().toString());

            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
