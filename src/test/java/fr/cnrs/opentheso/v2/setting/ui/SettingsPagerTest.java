package fr.cnrs.opentheso.v2.setting.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsPagerTest {

    @Test
    void slice_returnsFirstPageByDefault() {
        SettingsPager pager = new SettingsPager();
        List<Integer> items = numbers(12);

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), pager.slice(items));
        assertEquals(1, pager.from(12));
        assertEquals(10, pager.to(12));
        assertTrue(pager.needed(12));
    }

    @Test
    void next_movesToRemainingRows() {
        SettingsPager pager = new SettingsPager();
        List<Integer> items = numbers(12);

        pager.next(12);

        assertEquals(2, pager.getPage());
        assertEquals(List.of(11, 12), pager.slice(items));
        assertEquals(11, pager.from(12));
        assertEquals(12, pager.to(12));
    }

    @Test
    void slice_clampsWhenListShrinks() {
        SettingsPager pager = new SettingsPager();
        pager.go(3, 25);

        assertEquals(List.of(1, 2, 3, 4, 5), pager.slice(numbers(5)));
        assertEquals(1, pager.getPage());
    }

    @Test
    void needed_isFalseWhenAllRowsFit() {
        SettingsPager pager = new SettingsPager();

        assertFalse(pager.needed(10));
        assertEquals(List.of(1, 2), pager.pages(12));
    }

    private static List<Integer> numbers(int count) {
        return IntStream.rangeClosed(1, count).boxed().toList();
    }
}
