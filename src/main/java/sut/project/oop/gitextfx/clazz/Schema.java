package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.AppPath;

import java.sql.*;

public class Schema implements AutoCloseable {
    private static Connection connection;

    public Schema() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + AppPath.DB_PATH + "?busy_timeout=5000");
    }

    public Schema(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path + "?busy_timeout=5000");
    }

    // SELECT raw SQL
    public ResultSet selectRaw(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    public ResultSet selectRaw(String sql, Object ...args) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);

        if (args != null) {
            for (int i = 1 ; i <= args.length; i++) {
                stmt.setObject(i, args[i - 1]);
            }
        }

        return stmt.executeQuery();
    }

    // Non-SELECT raw SQL
    public boolean execute(String sql, Object ...args) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            if (args != null) {
                for (int i = 1 ; i <= args.length; i++) {
                    stmt.setObject(i, args[i - 1]);
                }
            }

            stmt.execute();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    ///  @return id of inserted row or -1 if failed to insert.
    public long insertAndReturnID(String sql, Object ...args) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < args.length; i++) {
                stmt.setObject(i + 1, args[i]);
            }

            int affected = stmt.executeUpdate();
            if (affected == 0) return -1;

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1;
    }

    // Query builder root (Optional for readability of code)
    public QueryBuilder query() {
        return new QueryBuilder(connection);
    }

    public QueryBuilder table(String table) {
        return new QueryBuilder(connection).from(table);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
        if (!connection.isClosed()) throw new SQLException("Can not close the connection");
    }
}
