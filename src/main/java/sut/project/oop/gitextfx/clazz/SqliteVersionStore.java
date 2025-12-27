package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.interfaces.IVersionStore;
import sut.project.oop.gitextfx.models.VersionTag;

import java.sql.ResultSet;
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
}
