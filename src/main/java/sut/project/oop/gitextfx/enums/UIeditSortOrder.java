package sut.project.oop.gitextfx.enums;

public enum UIeditSortOrder {
    LAST_EDIT_NEWEST("Last edited (newest)"),
    LAST_EDIT_OLDEST("Last edited (oldest)");

    private final String label;

    UIeditSortOrder(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
