package cc.leevi.common.poolc.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentBag {

    /**
     * 共享的资源池
     *
     * 对于shardList来说，更新的频率很低，因为使用threadLocal来缓存释放的连接
     * 所以只有在移除空闲连接后，填充连接池时才会进行添加连接，所以使用shardList，“无锁化”的线程安全
     */
    private final CopyOnWriteArrayList<ConcurrentBagEntry> sharedList;

    /**
     * 线程独享的资源池，释放的资源会添加道独享的资源池
     */
    private final ThreadLocal<List<WeakReference<ConcurrentBagEntry>>> threadList;

    private final SynchronousQueue<ConcurrentBagEntry> handoffQueue;

    public ConcurrentBag() {
        this.sharedList = new CopyOnWriteArrayList<>();
        threadList = ThreadLocal.withInitial(() -> new ArrayList<>(16));
        this.handoffQueue = new SynchronousQueue<>(true);
    }

    public ConcurrentBagEntry borrow(long timeout, TimeUnit timeUnit) throws InterruptedException {
        //优先从独享的资源池获取连接，这样不会有竞争
        List<WeakReference<ConcurrentBagEntry>> entryList = threadList.get();
        //反序循环，动态删除
        for (int i = entryList.size()-1; i>=0; i--) {
            //移除并获取
            ConcurrentBagEntry entry = entryList.remove(i).get();
            //因为没有锁，所以获取道的资源可能已经被其他线程获取并使用了，使用cas再检查一遍
            if(entry != null && entry.compareAndSet(ConcurrentBagEntry.STATE_NOT_IN_USE,ConcurrentBagEntry.STATE_IN_USE)){
                return entry;
            }
        }

        //独享资源池内无法获取，再从共享资源池中获取
        for (ConcurrentBagEntry entry : sharedList) {
            if(entry.compareAndSet(ConcurrentBagEntry.STATE_NOT_IN_USE,ConcurrentBagEntry.STATE_IN_USE)){
                return entry;
            }
        }

        //共享资源池还是没有资源，阻塞线程等待其他线程的释放
        ConcurrentBagEntry entry = handoffQueue.poll(timeout, timeUnit);
        return entry;
    }

    public void requite(ConcurrentBagEntry entry){
        //这里不需要cas，因为释放的连接不可能会被其他线程占用
        entry.setState(ConcurrentBagEntry.STATE_NOT_IN_USE);
        handoffQueue.offer(entry);

        List<WeakReference<ConcurrentBagEntry>> weakReferenceList = threadList.get();
        weakReferenceList.add(new WeakReference<>(entry));
    }

    public class ConcurrentBagEntry
    {
        /**
         * 空闲
         */
        public static final int STATE_NOT_IN_USE = 0;
        /**
         * 使用中
         */
        public static final int STATE_IN_USE = 1;
        /**
         * 已移除
         */
        public static final int STATE_REMOVED = -1;
        /**
         * 移除中（保留状态）
         */
        public static final int STATE_RESERVED = -2;

        private AtomicInteger state = new AtomicInteger(STATE_NOT_IN_USE);

        public boolean compareAndSet(int expectState, int newState){
            return state.compareAndSet(expectState,newState);
        }
        void setState(int newState){

        }
        int getState(){
            return state.get();
        }
    }

}
