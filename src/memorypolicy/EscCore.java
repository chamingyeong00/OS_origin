package memorypolicy;

import java.util.*;

public class EscCore implements CorePolicy {
    private int p_frame_size;
    private int[] escPages;
    private boolean[] referenceBit;
    private boolean[] dirtyBit;
    private int escPointer = 0;

    private List<Page> pageHistory;

    private int hit = 0;
    private int fault = 0;
    private int migration = 0;

    public EscCore(int frame_size) {
        this.p_frame_size = frame_size;
        escPages = new int[frame_size];
        Arrays.fill(escPages, -1);
        referenceBit = new boolean[frame_size];
        dirtyBit = new boolean[frame_size];
        pageHistory = new ArrayList<>();
    }

    @Override
    public Page.STATUS operate(char data) {
        Page newPage = new Page();
        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        // HIT check
        for (int i = 0; i < p_frame_size; i++) {
            if (escPages[i] == data) {
                referenceBit[i] = true;
                if (Math.random() < 0.3) dirtyBit[i] = true;
                newPage.status = Page.STATUS.HIT;
                hit++;
                newPage.loc = i + 1;
                pageHistory.add(newPage);
                return newPage.status;
            }
        }

        // MISS - replacement
        while (true) {
            if (!referenceBit[escPointer] && !dirtyBit[escPointer]) {
                escPages[escPointer] = data;
                referenceBit[escPointer] = true;
                dirtyBit[escPointer] = false;
                newPage.status = pageHistory.size() < p_frame_size ? Page.STATUS.PAGEFAULT : Page.STATUS.MIGRATION;
                if (pageHistory.size() < p_frame_size) fault++;
                else { fault++; migration++; }
                newPage.loc = escPointer + 1;
                escPointer = (escPointer + 1) % p_frame_size;
                pageHistory.add(newPage);
                return newPage.status;
            }

            if (referenceBit[escPointer]) {
                referenceBit[escPointer] = false;
            }
            escPointer = (escPointer + 1) % p_frame_size;
        }
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
