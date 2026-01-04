package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.models.VersionTag;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class VersionHistory {

    private final List<VersionTag> versions;
    public VersionHistory(List<VersionTag> versions) {
        SortUtil.sortByIntKey(versions, VersionTag::row_id);
        this.versions = versions;
    }

    public VersionTag findBaseOf(int target_id) {
        return versions.stream()
                .filter(v -> !v.is_delta() && v.row_id() < target_id)
                .toList()
                .getLast();
    }

    public VersionTag findById(int id) {
        return versions.stream()
                .filter(v -> v.row_id() == id)
                .findFirst()
                .orElseThrow();
    }

    public List<VersionTag> findDeltas(int base_id, int target_id) {
        return versions.stream()
                .filter(v ->
                        v.is_delta() &&
                                v.row_id() > base_id &&
                                v.row_id() <= target_id
                )
                .toList();
    }

    public Optional<VersionTag> findPreviousOf(int id) {
         try {
             var result = versions.stream()
                     .filter(v -> v.row_id() < id)
                     .toList()
                     .getLast();

             return Optional.of(result);
         } catch (NoSuchElementException _) {return Optional.empty();}
    }

    public int versionCount() {
        return versions.size();
    }
}
