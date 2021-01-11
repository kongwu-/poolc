package cc.leevi.common.poolc.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConcurrentBagTest {

    @Test
    public void borrow() throws InterruptedException {
        ConcurrentBag concurrentBag = new ConcurrentBag();
        for (int i = 0; i < 3; i++) {
            ConcurrentBagEntry entry = new ConcurrentBagEntry();
            concurrentBag.add(entry);
        }
        ConcurrentBagEntry entry = concurrentBag.borrow(1000, TimeUnit.MILLISECONDS);
        System.out.println(entry);
    }

    @Test
    public void requite() throws InterruptedException {
        ConcurrentBag concurrentBag = new ConcurrentBag();
        for (int i = 0; i < 3; i++) {
            ConcurrentBagEntry entry = new ConcurrentBagEntry();
            concurrentBag.add(entry);
        }
        ConcurrentBagEntry entry = concurrentBag.borrow(1000, TimeUnit.MILLISECONDS);
        System.out.println(entry);
        concurrentBag.requite(entry);
        entry = concurrentBag.borrow(1000, TimeUnit.MILLISECONDS);
        System.out.println(entry);
        entry = concurrentBag.borrow(1000, TimeUnit.MILLISECONDS);
        entry = concurrentBag.borrow(1000, TimeUnit.MILLISECONDS);
        entry = concurrentBag.borrow(5000, TimeUnit.MILLISECONDS);
        System.out.println(entry);
    }

    @Test
    public void add() {
        ConcurrentBag concurrentBag = new ConcurrentBag();
        for (int i = 0; i < 300; i++) {
            ConcurrentBagEntry entry = new ConcurrentBagEntry();
            concurrentBag.add(entry);
        }
        System.out.println(concurrentBag.size());
    }

    @Test
    public void remove() {
        ConcurrentBag concurrentBag = new ConcurrentBag();
        List<ConcurrentBagEntry> entries = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            ConcurrentBagEntry entry = new ConcurrentBagEntry();
            concurrentBag.add(entry);

            entries.add(entry);
        }
        System.out.println(concurrentBag.size());
        int removeIndex = 0;
        for (ConcurrentBagEntry entry : entries) {
            if(removeIndex++ >= 10){
                break;
            }
            concurrentBag.remove(entry);
        }
        System.out.println(concurrentBag.size());
    }

    @Test
    public void values() throws InterruptedException {
        ConcurrentBag concurrentBag = new ConcurrentBag();
        for (int i = 0; i < 300; i++) {
            ConcurrentBagEntry entry = new ConcurrentBagEntry();
            concurrentBag.add(entry);
        }

        int notUseSize = concurrentBag.values(ConcurrentBagEntry.STATE_NOT_IN_USE).size();
        System.out.println(notUseSize);
        int inUseSize = concurrentBag.values(ConcurrentBagEntry.STATE_IN_USE).size();
        System.out.println(inUseSize);

        ConcurrentBagEntry entry0 = concurrentBag.borrow(1000, TimeUnit.MILLISECONDS);
        ConcurrentBagEntry entry1 = concurrentBag.borrow(1000,TimeUnit.MILLISECONDS);
        ConcurrentBagEntry entry2 = concurrentBag.borrow(1000,TimeUnit.MILLISECONDS);
        ConcurrentBagEntry entry3 = concurrentBag.borrow(1000,TimeUnit.MILLISECONDS);

        notUseSize = concurrentBag.values(ConcurrentBagEntry.STATE_NOT_IN_USE).size();
        System.out.println(notUseSize);
        inUseSize = concurrentBag.values(ConcurrentBagEntry.STATE_IN_USE).size();
        System.out.println(inUseSize);

        concurrentBag.requite(entry0);
        concurrentBag.requite(entry1);
        concurrentBag.requite(entry2);
        concurrentBag.requite(entry3);

        notUseSize = concurrentBag.values(ConcurrentBagEntry.STATE_NOT_IN_USE).size();
        System.out.println(notUseSize);
        inUseSize = concurrentBag.values(ConcurrentBagEntry.STATE_IN_USE).size();
        System.out.println(inUseSize);
    }
}