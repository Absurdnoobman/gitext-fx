package sut.project.oop.gitextfx.clazz;

import sut.project.oop.gitextfx.interfaces.IVersionStore;
import sut.project.oop.gitextfx.models.VersionTag;

import java.sql.SQLException;
import java.util.List;

public class VersionService {

    private final IVersionStore store;
    private final DiffService diff_service;

    public VersionService(IVersionStore store) {
        this.store = store;
        this.diff_service = new DiffService();
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
                ? diff_service.create_delta(store, file_id, last_major, content)
                : CompressionUtil.compress(String.join("\n", content));

        var parent_id = is_delta ? last_major.row_id() : null;

        var inserted_id = store.insertVersion(
                file_id,
                is_delta,
                compressed,
                parent_id,
                new_tag
        );

        tags.add(new VersionTag(inserted_id, new_tag, is_delta));
        return inserted_id;
    }

    private boolean should_be_delta(int file_id, List<VersionTag> tags, VersionTag last_major) throws SQLException {

        var distance = tags.stream()
                .filter(t -> t.row_id() > last_major.row_id())
                .count();

        var interval = store.getNonDeltaInterval(file_id);

        return distance < interval - 1;
    }
}
