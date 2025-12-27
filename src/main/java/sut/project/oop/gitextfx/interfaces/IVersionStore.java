package sut.project.oop.gitextfx.interfaces;

import sut.project.oop.gitextfx.models.VersionTag;

import java.util.List;

public interface IVersionStore {
    String load(int fileId, int versionId) throws Exception;

    List<VersionTag> getVersionsOf(int fileId) throws Exception;
}
