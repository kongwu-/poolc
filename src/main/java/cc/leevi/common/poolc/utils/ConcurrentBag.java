package cc.leevi.common.poolc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cc.leevi.common.poolc.utils.ConcurrentBag.ConcurrentBagEntry.*;


public class ConcurrentBag {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentBag.class);

    /**
     * 共享的资源池
     *
     * 对于shardList来说，更新的频率很低，因为使用threadLocal来缓存释放的连接
     * 所以只有在移除空闲连接后，填充连接池时才会进行添加连接，所以使用shardList，“无锁化”的线程安全
     */
    private final CopyOnWriteArrayList<ConcurrentBagEntry> sharedList;

    private final AtomicInteger waiters;
    /**
     * 线程独享的资源池，释放的资源会添加道独享的资源池
     */
    private final ThreadLocal<List<WeakReference<ConcurrentBagEntry>>> threadList;

    private final SynchronousQueue<ConcurrentBagEntry> handoffQueue;

    public ConcurrentBag() {
        this.sharedList = new CopyOnWriteArrayList<>();
        threadList = ThreadLocal.withInitial(() -> new ArrayList<>(16));
        this.handoffQueue = new SynchronousQueue<>(true);
        waiters = new AtomicInteger();
    }

    public ConcurrentBagEntry borrow(long timeout, TimeUnit timeUnit) throws InterruptedException {
        //优先从独享的资源池获取连接，这样不会有竞争
        List<WeakReference<ConcurrentBagEntry>> entryList = threadList.get();
        //反序循环，动态删除
        for (int i = entryList.size()-1; i>=0; i--) {
            //移除并获取
            ConcurrentBagEntry entry = entryList.remove(i).get();
            //因为没有锁，所以获取道的资源可能已经被其他线程获取并使用了，使用cas再检查一遍
            if(entry != null && entry.compareAndSet(STATE_NOT_IN_USE,STATE_IN_USE)){
                return entry;
            }
        }
        waiters.getAndIncrement();
        try {
            //独享资源池内无法获取，再从共享资源池中获取
            for (ConcurrentBagEntry entry : sharedList) {
                if(entry.compareAndSet(STATE_NOT_IN_USE,STATE_IN_USE)){
                    return entry;
                }
            }

            //共享资源池还是没有资源，阻塞线程等待其他线程的释放
            ConcurrentBagEntry entry = handoffQueue.poll(timeout, timeUnit);
            return entry;
        }finally {
            waiters.getAndDecrement();
        }
    }

    public void requite(ConcurrentBagEntry entry){
        //这里不需要cas，因为还未释放的连接不可能会被其他线程占用
        entry.setState(STATE_NOT_IN_USE);
        //但是修改状态和放回连接池这一步，是非原子的

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
        //换个思路，offer一定会成功，那么只有在该资源被其他线程获取时，才不会唤醒，应该此时该资源是失效状态，暂且忽略offer返回值
        //如果并没有被其他线程获取，那么该资源有效，唤醒线程
        //会存在没有被其他线程获取，同时offer失败的场景吗？
        //只有再offer中有元素时，offer才会返回false
        for (int i = 0; waiters.get() > 0; i++) {
            if (entry.getState() != STATE_NOT_IN_USE || handoffQueue.offer(entry)) {
                return;
            }
        }

        List<WeakReference<ConcurrentBagEntry>> weakReferenceList = threadList.get();
        weakReferenceList.add(new WeakReference<>(entry));
    }


    public void add(final ConcurrentBagEntry entry){
        sharedList.add(entry);

        //自旋等待，唤醒等待线程
        while (waiters.get() >0 && entry.getState() == STATE_NOT_IN_USE && !handoffQueue.offer(entry)){
            Thread.yield();
        }
    }


    public boolean remove(final ConcurrentBagEntry entry){
        if(!entry.compareAndSet(STATE_NOT_IN_USE,STATE_REMOVED)){
            LOGGER.warn("尝试从资源池中删除一个已占用的资源：{}",entry);
            return false;
        }

        final boolean removed = sharedList.remove(entry);
        if (!removed) {
            LOGGER.warn("尝试从资源池中删除一个不存在的资源：{}", entry);
        }

        //这句可能没必要
        threadList.get().remove(entry);

        return removed;
    }

    public List<ConcurrentBagEntry> values(int state){
        return sharedList.stream().filter(e -> e.getState() == state).collect(Collectors.toList());
    }

    public int getWaitingThreadCount()
    {
        return waiters.get();
    }

    public int size()
    {
        return sharedList.size();
    }

    public static class ConcurrentBagEntry
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

        private Connection connection;

        private AtomicInteger state = new AtomicInteger(STATE_NOT_IN_USE);

        public boolean compareAndSet(int expectState, int newState){
            return state.compareAndSet(expectState,newState);
        }
        void setState(int newState){
            state.set(newState);
        }
        int getState(){
            return state.get();
        }

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }
    }

}
