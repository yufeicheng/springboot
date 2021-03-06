package cyf.gradle.api.configuration;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import cyf.gradle.dao.mapper.UserMapper;
import cyf.gradle.dao.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * Guava -Cache 及 ThreadPoolExecutor 线程池配置
 *
 * @author Cheng Yufei
 * @create 2018-06-08 10:58
 **/
@Configuration
@Slf4j
public class GuavaCache {

    @Autowired
    private AsyncPoolConfig asyncPoolConfig;
    @Autowired
    private UserMapper userMapper;

    /**
     * 缓存失效
     *
     * @return
     */
    @Bean(name = "expireLoadingCache")
    public LoadingCache<String, String> getLoadingCacheExpire() {

        log.info("<=================初始化 Expire LoadingCache<String, String>=================>");
        LoadingCache cache_Expire = CacheBuilder.newBuilder()
                //基于容量回收：当数量逼近限定值时，回收最近很少使用或总体很少使用的缓存
                .maximumSize(100)
                //过期时间,当缓存失效时，多个线程用相同key获取缓存时，只有一个线程进入load,用于缓存生成（机制有效避免了缓存穿透），其他线程阻塞等待缓存生成，但此时某个缓存过期会导致大量请求线程阻塞，引入refreshAfterWrite刷新机制
                .expireAfterWrite(1, TimeUnit.MINUTES)
                //移除监听器
                .removalListener(removalListener())
                //开启统计信息
                .recordStats()
                //.expireAfterAccess() 在被访问多少秒后失效
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        log.info("<================= 缓存过期，加载 - load key: {}=================>", key);
                        TimeUnit.MILLISECONDS.sleep(600);
                        User user = userMapper.selectByPrimaryKey(Integer.valueOf(key));
                        String userJsonString = JSONObject.toJSONString(user);
                        return "Expire_" + key + ":" + userJsonString;
                    }
                });
        return cache_Expire;
    }

    /**
     * 缓存定时刷新（有线程触发）
     * 刷新的好处是可以返回旧值而不必阻塞等待新缓存值的生成
     *
     * @return
     */
    @Bean(name = "refreshLoadingCache")
    public LoadingCache<String, String> getLoadingCacheRefresh() {

        log.info("<=================初始化 Refresh LoadingCache<String, String>=================>");
        LoadingCache<String, String> cache_Refresh = CacheBuilder.newBuilder()
                .maximumSize(100)
                //为了避免很多线程阻塞等待缓存生成，此刷新会只有load一个线程阻塞用于生成缓存，其他线程不阻塞会给返回旧的缓存值，但是只有有线程触发时才会刷新
                //但多个线程获取多个key值的缓存时，因load还是会阻塞多个load线程，压力也会穿透到数据库层，此时也需要给这个线程返回旧值，更新操作交由线程池异步完成 -> 进阶异步刷新
                .refreshAfterWrite(1, TimeUnit.MINUTES)
                .recordStats()
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        log.info("<================= Refresh缓存 - load :key:{}=================>", key);
                        //更新缓存时，延时600ms，观察其他并发线程是否返回旧缓存值还是处于阻塞状态（已证实返回旧值）
                        TimeUnit.MILLISECONDS.sleep(600);
                        User user = userMapper.selectByPrimaryKey(Integer.valueOf(key));
                        String userJsonString = JSONObject.toJSONString(user);
                        return "Refresh_" + key + ":" + userJsonString;

                    }
                });
        return cache_Refresh;
    }


    /**
     * 异步线程执行reload 进行缓存刷新, 解决多个key获取时多个load线程阻塞，刷新交由异步线程完成，load线程不阻塞，返回缓存旧值（有线程触发）
     *
     * @return
     */
    @Bean(name = "asyncRefreshLoadingCache")
    public LoadingCache<String, String> getLoadingCacheAsyncRefresh() {

        log.info("<=================初始化 AsyncRefresh LoadingCache<String, String>=================>");
        LoadingCache<String, String> cache_Refresh = CacheBuilder.newBuilder()
                //指缓存的key的个数而不是大小【如 size \ length 等】；
                //eg:现存 1，2，3 三个，当缓存4 时，会删除1
                .maximumSize(3)
                .refreshAfterWrite(1, TimeUnit.MINUTES)
                .recordStats()
                .build(new CacheLoader<String, String>() {
                    @Override
                    @SneakyThrows(value = {InterruptedException.class})
                    public String load(String key) {
                        log.info("<================= AsyncRefresh -load 缓存 - load :key:{}=================>", key);
                        TimeUnit.MILLISECONDS.sleep(600);
                        User user = userMapper.selectByPrimaryKey(Integer.valueOf(key));
                        String userJsonString = JSONObject.toJSONString(user);
                        return "AsyncRefresh_load_" + key + ":" + userJsonString;
                    }

                    @Override
                    public ListenableFuture<String> reload(String key, String oldValue) {
                        log.info("<================= AsyncRefresh -reload 缓存 - reload :key:{}=================>", key);

                        return MoreExecutors.listeningDecorator(getThreadPoolExecutor()).submit(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                log.info("<================= Thread: {}=================>", Thread.currentThread().getName());
                                TimeUnit.MILLISECONDS.sleep(600);
                                User user = userMapper.selectByPrimaryKey(Integer.valueOf(key));
                                String userJsonString = JSONObject.toJSONString(user);
                                return "AsyncRefresh_reload_" + key + ":" + userJsonString;
                            }
                        });
                    }
                });
        return cache_Refresh;
    }

    /**
     * java.util.concurrent ThreadPoolExecutor 线程池使用
     * <p>
     * Executor(Interface)、ExecutorService(Interface) 、Executors :
     * 1.ExecutorService继承Executor
     * 2.Executor提供execute() 接受Runnable参数，不返回结果； ExecutorService提供submit(), 接受Runnable，Callable 参数，有Future返回
     * 3.ExecutorService提供对线程池操作的方法
     * <p>
     * 4.Executors提供创建线程池的方法，但是不建议使用
     *
     * @return
     */
    @Bean(name = "threadPoolExecutor")
    public ThreadPoolExecutor getThreadPoolExecutor() {

        log.info("<=================初始化 ThreadPoolExecutor=================>");

        /**
         * 不建议Executors.创建线程池：
         * newFixedThreadPool、newSingleThreadExecutor :
         * 堆积的请求处理队列会耗费大量内存，OOM
         * newScheduledThreadPool、newCachedThreadPool ：
         * 可创建线程数 Integer.MAX_VALUE ,会创建大量线程导致OOM
         *
         * jmeter 测试：
         *
         * 池中线程数 < 核心线程数 ，请求创建新线程去执行
         *
         * 池中线程数  == 核心线程数 ，请求进入队列（小于队列长度），已创建的核心线程去处理请求
         *
         * 已达到核心线程数，队列已满，池中线程数 < maxPoolSize  ，创建新线程处理请求
         *
         *已达到核心线程数，队列已满，池中线程数 == maxPoolSize ，新请求根据策略处理
         *
         */
        ThreadFactory build = new ThreadFactoryBuilder().setNameFormat("demo_pool_%d").build();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(asyncPoolConfig.getCorePoolSize(), asyncPoolConfig.getMaxPoolSize(),
                asyncPoolConfig.getKeepAliveSeconds(), TimeUnit.SECONDS,
                //LinkedBlockingQueue：链表存储数据，有两把锁，放数据锁与取数据锁，放入数据的线程和取出数据的线程可以同时操作；不指定容器大小时为Integer.MAX_VALUE
                //ArrayBlockingQueue：放数据线程与取数据线程是互斥的，需指定容器大小
                new LinkedBlockingQueue<>(asyncPoolConfig.getQueueCapacity()), build, new ThreadPoolExecutor.CallerRunsPolicy());

        return threadPoolExecutor;
    }

    /**
     * 缓存移除监听器，缓存移除时收到原因，键值等信息，做一些额外的操作
     *
     * @return
     */
    public RemovalListener removalListener() {
        RemovalListener removalListener = new RemovalListener() {
            @Override
            public void onRemoval(RemovalNotification notification) {
                String cause = notification.getCause().name();
                log.info("缓存移除监听，原因：{}", cause);
            }
        };
        return removalListener;
    }

}
