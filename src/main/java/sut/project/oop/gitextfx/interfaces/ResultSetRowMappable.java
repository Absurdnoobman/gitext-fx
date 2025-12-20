package sut.project.oop.gitextfx.interfaces;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetRowMappable<T> {
    T map(ResultSet rs) throws SQLException;
}
