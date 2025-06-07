package memorypolicy;

import java.util.ArrayList;
import java.util.List;

public class LfuCore implements CorePolicy {
    private int frameSize;
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
        this.count = 1;      // 새 페이지가 들어오면 참조 횟수는 1로 초기화
        this.timeStamp = timeStamp;
    }
}

    public LfuCore(int frameSize) {
        this.frameSize = frameSize;
        this.cacheList = new Node[frameSize];
        this.pageHistory = new ArrayList<>();
    }

    @Override
    public Page.STATUS operate(char data) {
        curTime++;
        Page newPage = new Page();
        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        // 현재 프레임에 데이터가 있는지 확인 (Hit 여부 판단)
        int foundIndex = -1;
        for (int i = 0; i < curSize; i++) {
            if (cacheList[i] != null && cacheList[i].key == data) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex != -1) {
            // HIT: 해당 페이지 참조 횟수 증가, 타임스탬프 갱신
            cacheList[foundIndex].count++;
            cacheList[foundIndex].timeStamp = curTime;
            newPage.status = Page.STATUS.HIT;
            hit++;
            newPage.loc = foundIndex + 1;
        } else {
            // MISS: 프레임 여유 있으면 새 페이지 추가
            if (curSize < frameSize) {
                cacheList[curSize++] = new Node(data, curTime);
                newPage.status = Page.STATUS.PAGEFAULT;
                fault++;
                newPage.loc = curSize;
            } else {
                // 프레임 꽉 찬 경우 교체 대상 찾기 (최소 참조 횟수 & 가장 오래된 것)
                int minCount = Integer.MAX_VALUE;
                int minTime = Integer.MAX_VALUE;
                int minIndex = -1;

                for (int i = 0; i < curSize; i++) {
                    if (cacheList[i].count < minCount) {
                        minCount = cacheList[i].count;
                        minTime = cacheList[i].timeStamp;
                        minIndex = i;
                    } else if (cacheList[i].count == minCount) {
                        if (cacheList[i].timeStamp < minTime) {
                            minTime = cacheList[i].timeStamp;
                            minIndex = i;
                        }
                    }
                }
                // 교체: 기존 Node 값 직접 갱신
                cacheList[minIndex].key = data;
                cacheList[minIndex].count = 1;  // 새 페이지 참조 횟수는 1로 초기화
                cacheList[minIndex].timeStamp = curTime;

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
