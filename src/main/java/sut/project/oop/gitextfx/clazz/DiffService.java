package sut.project.oop.gitextfx.clazz;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import sut.project.oop.gitextfx.interfaces.IVersionStore;

import java.util.List;

public class DiffService {
    public byte[] createDelta(
            IVersionStore store,
            int file_id,
            int base_id,
            List<String> new_content
    ) throws Exception {

        var base_content = store.load(file_id, base_id);
        var base_lines = base_content.lines().toList();

        var patch = DiffUtils.diff(base_lines, new_content);

        var unified = UnifiedDiffUtils.generateUnifiedDiff(
                "old",
                "new",
                base_lines,
                patch,
                3
        );

        return CompressionUtil.compress(String.join("\n", unified));
    }
}
