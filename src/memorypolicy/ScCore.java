package memorypolicy;

import java.util.*;

public class ScCore implements CorePolicy {
    private final int frameSize;
    private final List<Page> pageHistory;
    private final int[] frames;
    private final boolean[] secondChance;
    private int pointer = 0;
    private int hit = 0, fault = 0, migration = 0;
    private int currentTime = 0;

    public ScCore(int frameSize) {
        this.frameSize = frameSize;
        this.frames = new int[frameSize];
        Arrays.fill(frames, -1); // 초기 빈 프레임 표시
        this.secondChance = new boolean[frameSize];
        this.pageHistory = new ArrayList<>();
    }

    @Override
    public Page.STATUS operate(char dataChar) {
        currentTime++;
        int pageNum = dataChar;  // char → int ASCII
        Page page = new Page();
        page.pid = Page.CREATE_ID++;
        page.data = dataChar;

        System.out.printf("[Time %d] Request Page: %c (%d)\n", currentTime, dataChar, pageNum);

        // HIT 체크
        for (int i = 0; i < frameSize; i++) {
            if (frames[i] == pageNum) {
                secondChance[i] = true;
                hit++;
                page.status = Page.STATUS.HIT;
                page.loc = i + 1;
                pageHistory.add(page);
                System.out.printf("→ HIT at Frame[%d]\n", i);
                printFrames();
                return page.status;
            }
        }

        // MISS 발생
        // 빈 프레임 있는지 먼저 확인
        for (int i = 0; i < frameSize; i++) {
            if (frames[i] == -1) {
                frames[i] = pageNum;
                secondChance[i] = false;
                fault++;
                page.status = Page.STATUS.PAGEFAULT;
                page.loc = i + 1;
                pageHistory.add(page);
                System.out.printf("→ PAGE FAULT (empty frame at %d)\n", i);
                printFrames();
                return page.status;
            }
        }

        // Second Chance 교체 로직
        while (true) {
            if (!secondChance[pointer]) {
                System.out.printf("→ REPLACE at Frame[%d] with %c\n", pointer, dataChar);
                frames[pointer] = pageNum;
                secondChance[pointer] = false;

                page.status = Page.STATUS.MIGRATION;
                page.loc = pointer + 1;
                pointer = (pointer + 1) % frameSize;

                fault++;
                migration++;
                break;
            } else {
                System.out.printf("→ Frame[%d] has second chance. Skipping...\n", pointer);
                secondChance[pointer] = false;
                pointer = (pointer + 1) % frameSize;
            }
        }

        pageHistory.add(page);
        printFrames();
        return page.status;
    }

    private void printFrames() {
        System.out.print("  [Frames]     : ");
        for (int f : frames) {
            System.out.print((f == -1 ? "." : (char) f) + " ");
        }
        System.out.print("\n  [2nd Chance] : ");
        for (boolean b : secondChance) {
            System.out.print((b ? "1" : "0") + " ");
        }
        System.out.printf("   <Pointer: %d>\n", pointer);
        System.out.println("----------------------------------------------------");
    }

    @Override
    public int getHitCount() {
        return hit;
    }

    @Override
    public int getFaultCount() {
        return fault;
    }

    @Override
    public int getMigrationCount() {
        return migration;
    }

    @Override
    public List<Page> getPageHistory() {
        return pageHistory;
    }
}
