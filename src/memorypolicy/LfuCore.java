package memorypolicy;

import java.util.*;

public class LfuCore implements CorePolicy {
    private int p_frame_size;
    private Node[] cacheList;
    private int curSize = 0;
    private int curTime = 0;
    private List<Page> pageHistory;

    private int hit = 0;
    private int fault = 0;
    private int migration = 0;

    private static class Node {
        int key;
        int count;
        int timeStamp;
        Node(int key, int timeStamp) {
            this.key = key;
            this.count = 1;
            this.timeStamp = timeStamp;
        }
    }

    public LfuCore(int frame_size) {
        this.p_frame_size = frame_size;
        cacheList = new Node[frame_size];
        pageHistory = new ArrayList<>();
    }

    @Override
    public Page.STATUS operate(char data) {
        curTime++;
        Page newPage = new Page();
        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        int foundIndex = -1;
        for (int i = 0; i < curSize; i++) {
            if (cacheList[i] != null && cacheList[i].key == data) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex != -1) {
            cacheList[foundIndex].count++;
            cacheList[foundIndex].timeStamp = curTime;
            newPage.status = Page.STATUS.HIT;
            hit++;
            newPage.loc = foundIndex + 1;
        } else {
            if (curSize < p_frame_size) {
                cacheList[curSize++] = new Node(data, curTime);
                newPage.status = Page.STATUS.PAGEFAULT;
                fault++;
                newPage.loc = curSize;
            } else {
                int minCount = Integer.MAX_VALUE;
                int minTime = Integer.MAX_VALUE;
                int minIndex = -1;
                for (int i = 0; i < curSize; i++) {
                    if (cacheList[i].count < minCount ||
                            (cacheList[i].count == minCount && cacheList[i].timeStamp < minTime)) {
                        minCount = cacheList[i].count;
                        minTime = cacheList[i].timeStamp;
                        minIndex = i;
                    }
                }
                cacheList[minIndex] = new Node(data, curTime);
                newPage.status = Page.STATUS.MIGRATION;
                migration++;
                fault++;
                newPage.loc = minIndex + 1;
            }
        }

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
}
