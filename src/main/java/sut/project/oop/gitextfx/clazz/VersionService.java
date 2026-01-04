package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.interfaces.IVersionStore;
import sut.project.oop.gitextfx.models.Version;
import sut.project.oop.gitextfx.models.VersionTag;

import java.sql.SQLException;
import java.util.List;

public class VersionService {

    private final IVersionStore store;
    private final DiffService diffService;

    public VersionService(IVersionStore store) {
        this.store = store;
        this.diffService = new DiffService();
    }

    public int createNewVersion(
            int file_id,
            List<VersionTag> tags,
            List<String> content,
            String new_tag
    ) throws Exception {

        var last_major = tags.stream()
                .filter(t -> !t.is_delta())
                .toList()
                .getLast();

        var is_delta = should_be_delta(file_id, tags, last_major);

        var compressed = is_delta
                ? diffService.createDelta(store, file_id, last_major.row_id(), content)
                : CompressionUtil.compress(String.join("\n", content));

        var parent_id = is_delta ? last_major.row_id() : null;

        return store.insertVersion(
                file_id,
                is_delta,
                compressed,
                parent_id,
                new_tag
        );
    }

    private List<Integer> find_dependents(int fileId, int parentId) throws SQLException {
        return store.getChildrenOf(fileId, parentId);
    }

    public void deleteVersion(Version version) throws Exception {
        if (can_delete_directly(version)) {
            store.deleteVersion(version.getId());
            return;
        }

        var dependent_ids = find_dependents(version.getFileId(), version.getId());
        SortUtil.bubble(dependent_ids);

        if (dependent_ids.isEmpty()) {
            store.deleteVersion(version.getId());
            return;
        }

        var new_major_id = dependent_ids.getFirst();
        dependent_ids.removeFirst();

        var new_content = rebuild_major_content(
                version.getFileId(),
                version.getId(),
                new_major_id
        );

        promote_to_major(new_major_id, new_content);

        for (var id : dependent_ids) {
            store.updateParent(id, new_major_id);
        }

        deleteVersion(version.getId());

    }

    private void promote_to_major(int new_major_id, String content) throws Exception {
        store.setDeltaToMajor(new_major_id, CompressionUtil.compress(content));
    }

    private String rebuild_major_content(int fileId, int old_major_id, int new_major_id) throws Exception {
        var applier = new PatchService();
        var delta = store.load(fileId, new_major_id);
        var base_content = store.load(fileId, old_major_id);
        return applier.apply(base_content, delta);
    }

    private boolean can_delete_directly(Version version) {
        return version.isDelta() && version.getParentId() != null;
    }

    public void deleteVersion(int id) throws SQLException {
        store.deleteVersion(id);
    }

    private boolean should_be_delta(int file_id, List<VersionTag> tags, VersionTag last_major) throws SQLException {
        var distance = tags.stream()
                .filter(t -> t.row_id() > last_major.row_id())
                .count();

        var interval = store.getNonDeltaInterval(file_id);

        return distance < interval - 1;
    }

    public List<VersionTag> loadTags(int fileId) throws SQLException {
        return store.getVersionTagsOf(fileId);
    }
}
