package fr.cnrs.opentheso.v2.preview.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreviewTablePager implements Serializable {

    public static final int PAGE_SIZE = 10;

    private int page = 1;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public <T> List<T> slice(List<T> items) {
        if (items == null || items.isEmpty()) {
            page = 1;
            return List.of();
        }
        int total = items.size();
        int count = pageCount(total);
        if (page > count) {
            page = count;
        }
        int from = (page - 1) * PAGE_SIZE;
        return new ArrayList<>(items.subList(from, Math.min(from + PAGE_SIZE, total)));
    }

    public int pageCount(int total) {
        if (total <= 0) {
            return 1;
        }
        return (total + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public boolean needed(int total) {
        return total > PAGE_SIZE;
    }

    public int from(int total) {
        if (total <= 0) {
            return 0;
        }
        return (page - 1) * PAGE_SIZE + 1;
    }

    public int to(int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(page * PAGE_SIZE, total);
    }

    public void next(int total) {
        if (page < pageCount(total)) {
            page++;
        }
    }

    public void prev() {
        if (page > 1) {
            page--;
        }
    }

    public void go(int target, int total) {
        int count = pageCount(total);
        page = Math.min(Math.max(1, target), count);
    }

    public List<Integer> pages(int total) {
        int count = pageCount(total);
        if (total <= 0) {
            return Collections.emptyList();
        }
        List<Integer> pages = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            pages.add(i);
        }
        return pages;
    }
}
