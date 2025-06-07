package memorypolicy;

import java.util.List;

public interface CorePolicy {
    enum STATUS {
        HIT, PAGEFAULT, MIGRATION
    }

    Page.STATUS operate(char data);
    int getHitCount();
    int getFaultCount();
    int getMigrationCount();
    List<Page> getPageHistory();
}
