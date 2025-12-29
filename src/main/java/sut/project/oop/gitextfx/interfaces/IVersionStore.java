package sut.project.oop.gitextfx.interfaces;

import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import java.sql.SQLException;
import java.util.List;

public interface IVersionStore {
    String load(int fileId, int versionId) throws Exception;

    List<VersionTag> getVersionTagsOf(int fileId) throws SQLException;

    int insertVersion(
            int file_id,
            boolean is_delta,
            byte[] compressed,
            Integer parent_id,
            String tag
    ) throws SQLException;

    int getNonDeltaInterval(int fileId) throws SQLException;

    void deleteVersion(int id) throws SQLException;

    Version getVersion(int id) throws SQLException;

    List<Integer> getChildrenOf(int parentId, int fileId) throws SQLException;

    void updateParent(int id, Integer newMajorId) throws SQLException;

    void setDeltaToMajor(int id, byte[] content) throws SQLException;
}
