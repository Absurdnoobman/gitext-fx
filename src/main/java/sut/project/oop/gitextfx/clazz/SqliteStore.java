package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.interfaces.IFileRecordStore;
import sut.project.oop.gitextfx.interfaces.IVersionStore;
import sut.project.oop.gitextfx.models.FileRecord;
import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteStore implements IVersionStore, IFileRecordStore {
    @Override
    public String load(int fileId, int versionId) throws SQLException, IOException {
        try (var db = new Schema()) {
            ResultSet rs = db.table("Versions")
                    .select("compressed")
                    .where("file_id", "=", fileId)
                    .where("id", "=", versionId)
                    .get();

            return CompressionUtil.decompress(rs.getBytes(1));
        }
    }

    @Override
    public List<VersionTag> getVersionTagsOf(int fileId) throws SQLException {
        try (var db = new Schema()) {
            ResultSet rs = db.table("Versions")
                    .select("id", "is_delta", "tag")
                    .where("file_id", "=", fileId)
                    .orderBy("id")
                    .get();

            List<VersionTag> list = new ArrayList<>();

            while (rs.next()) {
                list.add(new VersionTag(rs.getInt("id"), rs.getString("tag"), rs.getBoolean("is_delta")));
            }

            return list;
        }
    }

    @Override
    public int insertVersion(
            int file_id,
            boolean is_delta,
            byte[] compressed,
            Integer parent_id,
            String tag
    ) throws SQLException {
        return insertVersion(file_id, is_delta, compressed, parent_id, tag, LocalDateTime.now());
    }

    @Override
    public int insertVersion(
            int file_id,
            boolean is_delta,
            byte[] compressed,
            Integer parent_id,
            String tag,
            LocalDateTime dateTime
    ) throws SQLException {
        try (var db = new Schema()) {
            return Math.toIntExact(db.insertAndReturnID("""
                        INSERT INTO Versions (file_id, is_delta, compressed, parent_id, tag, created_at)
                        VALUES (?, ?, ?, ?, ?)
                    """, file_id, is_delta, compressed, parent_id, tag, dateTime));
        }
    }

    @Override
    public int getNonDeltaInterval(int file_id) throws SQLException {
        try (var db = new Schema()) {
            return db.table("Files")
                    .select("non_delta_interval")
                    .where("id", "=", file_id)
                    .get()
                    .getInt(1);
        }
    }

    @Override
    public void deleteVersion(int id) throws SQLException {
        try (var db = new Schema()) {
            db.execute("DELETE FROM Versions WHERE id = ?", id);
        }
    }

    @Override
    public Version getVersion(int id) throws SQLException {
        try (var db = new Schema()) {
            ResultSet rs = db.table("Versions")
                    .select("*")
                    .where("id", "=", id)
                    .get();

            return Version.from(rs, Version::new);
        }
    }

    @Override
    public List<Integer> getChildrenOf(int parent_id, int file_id) throws SQLException {
        try (var db = new Schema()) {
            List<Integer> ids = new ArrayList<>();

            ResultSet rs = db.query()
                    .select("id")
                    .from("Versions")
                    .where("file_id", "=", file_id)
                    .where("parent_id", "=", parent_id)
                    .get();

            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            rs.close();

            return ids;
        }
    }

    @Override
    public void updateParent(int id, Integer newMajorId) throws SQLException {
        try (var db = new Schema()) {
            db.execute("UPDATE Versions SET parent_id = ? WHERE id = ?", newMajorId, id);
        }
    }

    @Override
    public void setDeltaToMajor(int id, byte[] new_content) throws SQLException {
        try (var db = new Schema()) {
            db.execute(
                    "UPDATE Versions SET parent_id = null, compressed = ?, is_delta = false WHERE id = ?",
                    new_content,
                    id
            );
        }
    }

    @Override
    public FileRecord get(int id) throws SQLException {
        try (var db = new Schema()) {
            ResultSet rs = db.table("Files")
                    .where("id", "=", id)
                    .get();

            return FileRecord.from(rs, FileRecord::new);
        }
    }

    @Override
    public List<FileRecord> getAll() throws SQLException {
        try (var db = new Schema()) {
            ResultSet rs = db.query()
                    .select("*")
                    .from("Files")
                    .get();

            return FileRecord.allFrom(rs, FileRecord::new);
        }
    }

    @Override
    public void insert(String path, LocalDateTime lasted_edit) throws SQLException {
        try (var db = new Schema()) {
            db.execute("""
                    INSERT INTO Files (file_path, lasted_edit) VALUES (?, ?)
                    """, path, lasted_edit);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (var db = new Schema()) {
            db.execute("""
                    DELETE FROM Files WHERE id = ?
                    """, id);
        }
    }
}
