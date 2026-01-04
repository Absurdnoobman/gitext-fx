package sut.project.oop.gitextfx.clazz;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class SortUtil {
    /// Sort Ascending Order
    public static <T extends Comparable<T>> void bubble(List<T> list) {
        bubble(list, true);
    }
    /// Sort Ascending Order
    public static <T> void sortByIntKey(List<T> list, ToIntFunction<T> keyExtractor) {
        sortByIntKey(list, keyExtractor, true);
    }

    public static <T extends Comparable<T>> void bubble(List<T> list, boolean isAsc) {
        BiPredicate<T, T> shouldSwap = isAsc
                ? (a, b) -> a.compareTo(b) > 0
                : (a, b) -> a.compareTo(b) < 0;

        sort(list, shouldSwap);
    }

    public static <T> void sortByIntKey(List<T> list, ToIntFunction<T> keyExtractor, boolean isAsc) {
        BiPredicate<T, T> shouldSwap = isAsc
                ? (a, b) -> keyExtractor.applyAsInt(a) > keyExtractor.applyAsInt(b)
                : (a, b) -> keyExtractor.applyAsInt(a) < keyExtractor.applyAsInt(b);

        sort(list, shouldSwap);
    }

    private static <T> void sort(List<T> ls, BiPredicate<T, T> checker) {
        for (int pass = 1; pass < ls.size(); pass++) {
            for (int i = 0; i < ls.size() - pass; i++) {
                if (checker.test(ls.get(i), ls.get(i + 1))) {
                    T temp = ls.get(i);
                    ls.set(i, ls.get(i + 1));
                    ls.set(i + 1, temp);
                }
            }
        }
    }
}
