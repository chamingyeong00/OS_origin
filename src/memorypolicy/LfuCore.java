// LfuCore.java
package memorypolicy;

import java.util.*;

public class LfuCore implements CorePolicy {
    private final int p_frame_size;
    private final List<Page> frame_window;
    private final List<Page> pageHistory;
    private final Map<Character, Integer> frequencyMap;

    private int hit = 0;
    private int fault = 0;
    private int migration = 0;
    private int cursor = 0;

    public LfuCore(int frame_size) {
        this.p_frame_size = frame_size;
        this.frame_window = new ArrayList<>();
        this.pageHistory = new ArrayList<>();
        this.frequencyMap = new HashMap<>();
    }

    @Override
    public Page.STATUS operate(char data) {
        Page newPage = new Page();
        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        boolean found = false;
        int index = -1;

        for (int i = 0; i < frame_window.size(); i++) {
            if (frame_window.get(i).data == data) {
                found = true;
                index = i;
                break;
            }
        }

        if (found) {
            hit++;
            newPage.status = Page.STATUS.HIT;
            frequencyMap.put(data, frequencyMap.get(data) + 1);

            // 정확한 위치 계산
            frame_window.sort(Comparator
                    .comparingInt((Page p) -> frequencyMap.get(p.data))
                    .thenComparingInt(p -> p.pid));

            for (int i = 0; i < frame_window.size(); i++) {
                if (frame_window.get(i).data == data) {
                    newPage.loc = i + 1;
                    break;
                }
            }
        }else {
            fault++;

            if (frame_window.size() >= p_frame_size) {
                char lfuData = frame_window.get(0).data;
                for (Page p : frame_window) {
                    if (frequencyMap.get(p.data) < frequencyMap.get(lfuData)) {
                        lfuData = p.data;
                    }
                }

                Iterator<Page> it = frame_window.iterator();
                while (it.hasNext()) {
                    if (it.next().data == lfuData) {
                        it.remove();
                        break;
                    }
                }

                frequencyMap.remove(lfuData);
                migration++;
                newPage.status = Page.STATUS.MIGRATION;
            } else {
                newPage.status = Page.STATUS.PAGEFAULT;
            }

            frame_window.add(newPage);
            frequencyMap.put(data, 1);
        }

        frame_window.sort(Comparator
                .comparingInt((Page p) -> frequencyMap.get(p.data))
                .thenComparingInt(p -> p.pid));

        for (int i = 0; i < frame_window.size(); i++) {
            if (frame_window.get(i) == newPage) {
                newPage.loc = i + 1;
                break;
            }
        }
        System.out.println("== LFU operate() DEBUG ==");
        System.out.printf("newPage: pid=%d, data=%c, status=%s\n", newPage.pid, newPage.data, newPage.status);
        System.out.println("-- frame_window (after sort):");
        for (int i = 0; i < frame_window.size(); i++) {
            Page p = frame_window.get(i);
            System.out.printf("  [%d] pid=%d, data=%c, freq=%d\n", i, p.pid, p.data, frequencyMap.get(p.data));
        }
        System.out.println("-- Checking identity (frame_window[i] == newPage):");
        boolean matched = false;
        for (int i = 0; i < frame_window.size(); i++) {
            if (frame_window.get(i).data == newPage.data) {
                System.out.printf("  → newPage matched at index %d\n", i);
                matched = true;
                break;
            }
        }
        if (!matched) {
            System.out.println("  → WARNING: newPage not found by identity!");
        }

        cursor = frame_window.size();
        pageHistory.add(newPage);
        return newPage.status;
    }

    @Override
    public List<Page> getFrameStateAtStep(int step) {
        Map<Character, Integer> freq = new HashMap<>();
        List<Page> current = new ArrayList<>();

        for (int i = 0; i <= step && i < pageHistory.size(); i++) {
            Page p = pageHistory.get(i);
            freq.put(p.data, freq.getOrDefault(p.data, 0) + 1);

            // 중복 페이지 무시
            if (current.stream().anyMatch(pg -> pg.data == p.data)) continue;

            if (current.size() >= p_frame_size) {
                current.sort(Comparator
                        .comparingInt((Page pg) -> freq.get(pg.data))
                        .thenComparingInt(pg -> pg.pid));
                current.remove(0);
            }

            // 새로 넣는 Page 객체는 깊은 복사 필요
            Page copied = new Page();
            copied.pid = p.pid;
            copied.data = p.data;
            copied.status = p.status;
            copied.loc = -1;  // 나중에 설정
            current.add(copied);
        }

        // loc 설정
        current.sort(Comparator
                .comparingInt((Page pg) -> freq.get(pg.data))
                .thenComparingInt(pg -> pg.pid));
        for (int i = 0; i < current.size(); i++) {
            current.get(i).loc = i + 1; // loc는 1-based index
        }

        return current;
    }

    @Override
    public int getHitCount() { return hit; }
    @Override
    public int getFaultCount() { return fault; }
    @Override
    public int getMigrationCount() { return migration; }
    @Override
    public List<Page> getPageHistory() { return pageHistory; }

    // 🔽 추가 구현
    @Override
    public Queue<Page> getCurrentFrames() {
        return new LinkedList<>(frame_window);
    }

    @Override
    public int getCursor() {
        return cursor;
    }

    @Override
    public int getFrameSize() {
        return p_frame_size;
    }
}
