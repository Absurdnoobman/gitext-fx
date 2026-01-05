package sut.project.oop.gitextfx.clazz;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import java.util.Arrays;

public final class PatchService {

    public String apply(String base, String unifiedDiff) throws PatchFailedException {
        Patch<String> patch = parseFromStr(unifiedDiff);
        return String.join("\n", patch.applyTo(base.lines().toList()));
    }

    public Patch<String> parseFromStr(String diffStr) {
        return UnifiedDiffUtils.parseUnifiedDiff(Arrays.asList(diffStr.split("\n", -1)));
    }

}
