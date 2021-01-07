package cc.leevi.common.poolc.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

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
        //这里不需要cas，因为还未释放的连接不可能会被其他线程占用
        entry.setState(ConcurrentBagEntry.STATE_NOT_IN_USE);
        //但是修改状态和放回连接池这一步，是非原子的


        handoffQueue.offer(entry);

        //思路
        //刚释放回连接池的链接，已修改状态为未使用，但未添加到独享资源池中时
        //因为可能会有多个线程等待资源池的释放，所以这里需要唤醒其他线程
        //但是此时，该资源可能会被分配给其他线程，分配后状态会修改为IN_USE或者其他
        //如果已经分配给了其他线程，那么这里就再唤醒了，因为该连接不能被共享
        //不过……这里检查所有waiter是什么意思？
        //如果有三个线程等待中，那么检查3遍？WTF!
        //好像懂了
        //因为handoffQueue容量只有1，如果多个线程同时等待，那么一次offer只能唤醒一个……

        //比如现在maximumPoolSize 10，已获取IN_USE 10
        //又来5个新线程获取，那么此时这5个线程都是会被阻塞，waiters是5
        //此时释放资源时，offer一定是成功吗？是的
        //是不是可以理解为offer和环境是非原子？
        //offer返回false，只会在没有等待者的情况下，那么没有等待者的话，waiters也应该没有吧
        //换个思路，offer一定会成功，那么只有在该资源被其他线程获取时，才不会唤醒，应该此时该资源是失效状态
        //如果并没有被其他线程获取，那么该资源有效，唤醒线程
        //会存在没有被其他线程获取，同时offer失败的场景吗？
        for (int i = 0; waiters.get() > 0; i++) {
            if (bagEntry.getState() != STATE_NOT_IN_USE || handoffQueue.offer(bagEntry)) {
                return;
            }
            else if ((i & 0xff) == 0xff) {
                parkNanos(MICROSECONDS.toNanos(10));
            }
            else {
                Thread.yield();
            }
        }




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
