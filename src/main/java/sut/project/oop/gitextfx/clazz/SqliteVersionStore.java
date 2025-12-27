package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.interfaces.IVersionStore;
import sut.project.oop.gitextfx.models.VersionTag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteVersionStore implements IVersionStore {
    @Override
    public String load(int fileId, int versionId) throws Exception {
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
    public List<VersionTag> getVersionsOf(int fileId) throws Exception {
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

        try (var db = new Schema()) {
            return Math.toIntExact(db.insertAndReturnID("""
                        INSERT INTO Versions (file_id, is_delta, compressed, parent_id, tag)
                        VALUES (?, ?, ?, ?, ?)
                    """, file_id, is_delta, compressed, parent_id, tag));
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
}
