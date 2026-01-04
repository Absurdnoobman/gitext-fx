package sut.project.oop.gitextfx.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Version extends Model {
    private int id;
    private int fileId;
    private String tag;
    private boolean isDelta;
    private Integer parentId;
    private byte[] compressed;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isDelta() {
        return isDelta;
    }

    public void setDelta(boolean delta) {
        isDelta = delta;
    }

    public Integer getParentId() {
        return this.parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public byte[] getCompressed() {
        return compressed;
    }

    public void setCompressed(byte[] compressed) {
        this.compressed = compressed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public void fromResultSet(ResultSet rs) throws SQLException {
        this.id = rs.getInt("id");
        this.fileId = rs.getInt("file_id");

        int pid = rs.getInt("parent_id");
        this.parentId = rs.wasNull() ? null : pid;

        this.tag = rs.getString("tag");
        this.isDelta = rs.getBoolean("is_delta");
        this.compressed = rs.getBytes("compressed");

        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            this.createdAt = LocalDateTime.parse(createdAt);
        }
    }


}
