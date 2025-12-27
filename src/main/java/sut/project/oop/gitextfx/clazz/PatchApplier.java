package sut.project.oop.gitextfx.clazz;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import java.util.Arrays;

public final class PatchApplier {

    public String apply(String base, String unifiedDiff) throws PatchFailedException {

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(Arrays.asList(unifiedDiff.split("\n", -1)));

        return String.join("\n", patch.applyTo(base.lines().toList()));
    }
}
