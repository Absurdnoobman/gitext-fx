package sut.project.oop.gitextfx.clazz;

import java.util.ArrayList;
import java.util.List;

public class WhereBuilder {
    final List<String> clauses = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    public WhereBuilder where(String column, String op, Object value) {
        clauses.add(column + " " + op + " ?");
        values.add(value);
        return this;
    }

    public WhereBuilder orWhere(String column, String op, Object value) {
        clauses.add("OR " + column + " " + op + " ?");
        values.add(value);
        return this;
    }

    String toSql() {
        return String.join(" ", clauses);
    }
}
