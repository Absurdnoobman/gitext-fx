package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.interfaces.IVersionStore;
import sut.project.oop.gitextfx.models.VersionTag;

public class VersionResolver {

    private final IVersionStore store;
    private final PatchApplier patcher;

    public VersionResolver(IVersionStore store, PatchApplier patcher) {
        this.store = store;
        this.patcher = patcher;
    }

    public String resolve(int fileId, int versionId) throws Exception {
        VersionHistory history = new VersionHistory(store.getVersionTagsOf(fileId));

        VersionTag target = history.findById(versionId);

        if (!target.is_delta()) {
            return store.load(fileId, target.row_id());
        }

        VersionTag parent = history.findBaseOf(versionId);
        String base = store.load(fileId, parent.row_id());
        String patch_str = store.load(fileId, target.row_id());

        return patcher.apply(base, patch_str);
    }
}