package sut.project.oop.gitextfx.clazz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class QueryBuilder {

    public enum SortOrder {
        /// A - Z
        ASC,
        /// Z - A
        DESC
    }

    private final Connection connection;

    private String table;
    private String select = "*";

    private final List<String> joins = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();

    private String orderBy = "";

    public QueryBuilder(Connection connection) {
        this.connection = connection;
    }

    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    public QueryBuilder select(String... columns) {
        this.select = String.join(",", columns);
        return this;
    }

    public QueryBuilder join(String table, String left, String op, String right) {
        joins.add("INNER JOIN " + table + " ON " + left + " " + op + " " + right);
        return this;
    }

    public QueryBuilder leftJoin(String table, String left, String op, String right) {
        joins.add("LEFT JOIN " + table + " ON " + left + " " + op + " " + right);
        return this;
    }

    public QueryBuilder where(String column, String op, Object value) {
        conditions.add(new Condition(column + " " + op + " ?", value));
        return this;
    }

    // Multiple Where conditions
    public QueryBuilder where(Consumer<WhereBuilder> closure) {
        WhereBuilder wb = new WhereBuilder();
        closure.accept(wb);

        if (!wb.clauses.isEmpty()) {
            conditions.add(new Condition("(" + wb.toSql() + ")", null));
            wb.values.forEach(v -> conditions.add(new Condition("?", v)));
        }
        return this;
    }

    public ResultSet get() throws SQLException {
        var sql = new StringBuilder();
        sql.append("SELECT ").append(select)
                .append(" FROM ").append(table);

        for (var j : joins) {
            sql.append(" ").append(j);
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;

            for (var c : conditions) {
                if (c.value != null) {
                    if (!first) sql.append(" AND ");
                    sql.append(c.sql);
                }
                first = false;
            }
        }

        sql.append(orderBy);

        PreparedStatement stmt = connection.prepareStatement(sql.toString());

        int i = 1;
        for (var c : conditions) {
            if (c.value != null) {
                stmt.setObject(i, c.value);
                i++;
            }
        }

        return stmt.executeQuery();
    }

    public List<Map<String, Object>> getAsList() throws SQLException {
        var rs = get();

        List<Map<String, Object>> rows = new ArrayList<>();
        var meta = rs.getMetaData();
        var columnCount = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> first() throws SQLException {
        var rs = get();
        if (!rs.next()) return null;

        Map<String, Object> row = new HashMap<>();
        var meta = rs.getMetaData();

        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnName(i), rs.getObject(i));
        }

        return row;
    }

    public QueryBuilder orderBy(String column) {
        return orderBy(column, SortOrder.ASC);
    }

    public QueryBuilder orderBy(String column, SortOrder order) {
        this.orderBy = "ORDER BY %s %s".formatted(column, order);
        return this;
    }
}
