package sut.project.oop.gitextfx.interfaces;

import sut.project.oop.gitextfx.models.VersionTag;

import java.sql.SQLException;
import java.util.List;

public interface IVersionStore {
    String load(int fileId, int versionId) throws Exception;

    List<VersionTag> getVersionsOf(int fileId) throws Exception;

    public int insertVersion(
            int file_id,
            boolean is_delta,
            byte[] compressed,
            Integer parent_id,
            String tag
    ) throws SQLException;

    public int getNonDeltaInterval(int file_id) throws SQLException;
}
