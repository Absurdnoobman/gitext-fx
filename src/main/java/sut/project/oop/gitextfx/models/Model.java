package sut.project.oop.gitextfx.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Class representation of a row from database
 */
public abstract class Model {

    /**
     * Map the current ResultSet row into this model
     * ResultSet must already be positioned on a valid row
     */
    protected abstract void fromResultSet(ResultSet rs) throws SQLException;

    /**
     * Factory-style helper to map one row
     */
    public static <T extends Model> T from(ResultSet rs, Supplier<T> supplier) throws SQLException {
        T model = supplier.get();
        model.fromResultSet(rs);
        return model;
    }

    /**
     * Map entire ResultSet into a List of models
     */
    public static <T extends Model> List<T> allFrom(ResultSet rs, Supplier<T> supplier) throws SQLException {
        List<T> list = new ArrayList<>();

        while (rs.next()) {
            list.add(from(rs, supplier));
        }

        return list;
    }
}
