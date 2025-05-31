/*package memorypolicy;

import java.util.*;

public class Core {
    public enum Policy {
        FIFO,
        LFU,
        ESC  // Enhanced Second Chance
    }

    private int cursor;
    public int p_frame_size;
    public Policy policy;

    // FIFO 관련
    public Queue<Page> frame_window;

    // LFU 관련
    private Node[] cacheList;
    private int curSize;
    private int curTime;

    // ESC 관련
    private int[] escPages;         // 페이지 데이터 저장
    private boolean[] referenceBit; // 참조 비트 R
    private boolean[] dirtyBit;     // 수정 비트 D
    private int escPointer;         // 원형 포인터

    public List<Page> pageHistory;

    public int hit;
    public int fault;
    public int migration;

    private static class Node {
        int key; // Page data를 key로 사용
        int count;
        int timeStamp;

        Node(int key, int timeStamp) {
            this.key = key;
            this.count = 1;
            this.timeStamp = timeStamp;
        }
    }

    public Core(int get_frame_size, Policy policy) {
        this.cursor = 0;
        this.p_frame_size = get_frame_size;
        this.policy = policy;

        if (policy == Policy.FIFO) {
            this.frame_window = new LinkedList<>();
        } else if (policy == Policy.LFU) {
            this.cacheList = new Node[get_frame_size];
            this.curSize = 0;
            this.curTime = 0;
        } else if (policy == Policy.ESC) {
            this.escPages = new int[get_frame_size];
            Arrays.fill(escPages, -1);
            this.referenceBit = new boolean[get_frame_size];
            this.dirtyBit = new boolean[get_frame_size];
            this.escPointer = 0;
        }

        this.pageHistory = new ArrayList<>();
        this.hit = 0;
        this.fault = 0;
        this.migration = 0;
    }

    public Page.STATUS operate(char data) {
        if (policy == Policy.FIFO) {
            return operateFIFO(data);
        } else if (policy == Policy.LFU) {
            return operateLFU(data);
        } else if (policy == Policy.ESC) {
            return operateESC(data);
        }
        throw new IllegalArgumentException("Unsupported policy");
    }

    private Page.STATUS operateFIFO(char data) {
        Page newPage = new Page();
        boolean found = false;
        int locIndex = -1;

        int index = 0;
        for (Page page : frame_window) {
            if (page.data == data) {
                found = true;
                locIndex = index;
                break;
            }
            index++;
        }

        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        if (found) {
            newPage.status = Page.STATUS.HIT;
            hit++;
            newPage.loc = locIndex + 1;
        } else {
            if (frame_window.size() >= p_frame_size) {
                newPage.status = Page.STATUS.MIGRATION;
                frame_window.poll(); // FIFO: 제일 오래된 페이지 제거
                cursor = p_frame_size;
                migration++;
                fault++;
            } else {
                newPage.status = Page.STATUS.PAGEFAULT;
                cursor++;
                fault++;
            }
            newPage.loc = cursor;
            frame_window.offer(newPage);
        }

        pageHistory.add(newPage);
        return newPage.status;
    }

    private Page.STATUS operateLFU(char data) {
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
                    if (cacheList[i].count < minCount
                            || (cacheList[i].count == minCount && cacheList[i].timeStamp < minTime)) {
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

    private Page.STATUS operateESC(char data) {
        Page newPage = new Page();
        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        // ESC 찾기
        for (int i = 0; i < p_frame_size; i++) {
            if (escPages[i] == data) {
                // HIT: 참조 비트 1로 설정
                referenceBit[i] = true;
                // 30% 확률로 dirty bit 갱신 (예시)
                if (Math.random() < 0.3) dirtyBit[i] = true;
                newPage.status = Page.STATUS.HIT;
                hit++;
                newPage.loc = i + 1;
                pageHistory.add(newPage);
                return newPage.status;
            }
        }

        // MISS: 교체 필요
        // 우선순위 (R,D): 00 > 01 > 10 > 11
        while (true) {
            if (!referenceBit[escPointer] && !dirtyBit[escPointer]) {
                // 교체 후보 발견
                escPages[escPointer] = data;
                referenceBit[escPointer] = true;
                dirtyBit[escPointer] = false;
                newPage.status = pageHistory.size() < p_frame_size ? Page.STATUS.PAGEFAULT : Page.STATUS.MIGRATION;
                if (pageHistory.size() < p_frame_size) fault++; else {fault++; migration++;}
                newPage.loc = escPointer + 1;
                escPointer = (escPointer + 1) % p_frame_size;
                pageHistory.add(newPage);
                return newPage.status;
            }

            // 참고 안된 페이지가 없으면 참조 비트 리셋
            if (referenceBit[escPointer]) {
                referenceBit[escPointer] = false;
            }

            escPointer = (escPointer + 1) % p_frame_size;
        }
    }

    public List<Page> getPageInfo(Page.STATUS status) {
        List<Page> pages = new ArrayList<>();
        for (Page page : pageHistory) {
            if (page.status == status) {
                pages.add(page);
            }
        }
        return pages;
    }
}
*/