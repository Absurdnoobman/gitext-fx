package sut.project.oop.gitextfx.components;

import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/// DEPRECATED
public class CustomTextArea extends Pane {
    private static final double LINE_HEIGHT = 20;  // Approximate line height
    private static final double FONT_SIZE = 14;

    private final List<String> lines;
    private int cursorLine;
    private int cursorColumn;

    private final Text lineNumbersText;
    private final TextFlow textFlow;

    public CustomTextArea() {
        lines = new ArrayList<>();
        lines.add(""); // Start with an empty line
        cursorLine = 0;
        cursorColumn = 0;

        // Set up the custom rendering area
        lineNumbersText = new Text();
        lineNumbersText.setFont(Font.font("Monospaced", FONT_SIZE));
        lineNumbersText.setFill(Color.GRAY);

        textFlow = new TextFlow();
        textFlow.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: " + FONT_SIZE + "px;");

        // Set the height and width of the pane
        this.setMinHeight(200);
        this.setMinWidth(600);

        // Add the line numbers and the text flow
        this.getChildren().addAll(lineNumbersText, textFlow);

        // Listen to key events for text manipulation
        this.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        // Update UI when text changes
        updateUI();
    }

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER:
                insertLine();
                break;
            case BACK_SPACE:
                deleteCharacter();
                break;
            case LEFT:
                moveCursorLeft();
                break;
            case RIGHT:
                moveCursorRight();
                break;
            case UP:
                moveCursorUp();
                break;
            case DOWN:
                moveCursorDown();
                break;
            case SPACE:
                insertCharacter(" ");
                break;
            default:
                if (!event.getText().isEmpty()) {
                    insertCharacter(event.getText());
                }
                break;
        }
        updateUI();
    }

    private void insertLine() {
        if (cursorLine == lines.size()) {
            lines.add("");
        } else {
            lines.add(cursorLine + 1, "");
        }
        cursorLine++;
        cursorColumn = 0;
    }

    private void deleteCharacter() {
        if (cursorColumn > 0) {
            String line = lines.get(cursorLine);
            lines.set(cursorLine, line.substring(0, cursorColumn - 1) + line.substring(cursorColumn));
            cursorColumn--;
        } else if (cursorLine > 0) {
            String line = lines.get(cursorLine);
            String prevLine = lines.get(cursorLine - 1);
            lines.set(cursorLine - 1, prevLine + line);
            lines.remove(cursorLine);
            cursorLine--;
            cursorColumn = prevLine.length();
        }
    }

    private void moveCursorLeft() {
        if (cursorColumn > 0) {
            cursorColumn--;
        } else if (cursorLine > 0) {
            cursorLine--;
            cursorColumn = lines.get(cursorLine).length();
        }
    }

    private void moveCursorRight() {
        if (cursorColumn < lines.get(cursorLine).length()) {
            cursorColumn++;
        } else if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorColumn = 0;
        }
    }

    private void moveCursorUp() {
        if (cursorLine > 0) {
            cursorLine--;
            cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length());
        }
    }

    private void moveCursorDown() {
        if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length());
        }
    }

    private void insertCharacter(String character) {
        String line = lines.get(cursorLine);
        lines.set(cursorLine, line.substring(0, cursorColumn) + character + line.substring(cursorColumn));
        cursorColumn++;
    }

    private void updateUI() {
        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= lines.size(); i++) {
            lineNumbers.append(i).append("\n");
        }
        lineNumbersText.setText(lineNumbers.toString());

        StringBuilder textContent = new StringBuilder();
        for (String line : lines) {
            textContent.append(line).append("\n");
        }

        textFlow.getChildren().clear();
        for (String line : lines) {
            textFlow.getChildren().add(new Text(line + "\n"));
        }

        // Update the layout
        layoutText();
    }

    private void layoutText() {
        // Position the line numbers and the textFlow next to each other
        double lineNumbersWidth = lineNumbersText.getBoundsInLocal().getWidth();
        lineNumbersText.setLayoutX(0);
        lineNumbersText.setLayoutY(0);

        textFlow.setLayoutX(lineNumbersWidth + 5);
        textFlow.setLayoutY(0);
    }
}
