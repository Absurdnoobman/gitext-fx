package sut.project.oop.gitextfx.models;

import sut.project.oop.gitextfx.AppDateFormat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class File extends Model {
    private long id;
    private String filePath;
    private LocalDateTime lastedEdit;

    private int nonDeltaInterval;

    public File(long id, String filePath, LocalDateTime lastedEdit) {
        this.id = id;
        this.filePath = filePath;
        this.lastedEdit = lastedEdit;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getLastedEdit() {
        return lastedEdit;
    }

    public void setLastedEdit(LocalDateTime lastedEdit) {
        this.lastedEdit = lastedEdit;
    }


    @Override
    public void fromResultSet(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.filePath = rs.getString("file_path");
        String lasted_edit = rs.getString("lasted_edit");

        if (lasted_edit != null) {
            this.lastedEdit = LocalDateTime.parse(lasted_edit, AppDateFormat.SQLITE_DT);
        }
    }
}
