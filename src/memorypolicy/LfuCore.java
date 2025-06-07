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
            newPage.loc = index + 1;
            frequencyMap.put(data, frequencyMap.get(data) + 1);
        } else {
            fault++;
            newPage.loc = frame_window.size() + 1;

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

        cursor = frame_window.size();
        pageHistory.add(newPage);
        return newPage.status;
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
