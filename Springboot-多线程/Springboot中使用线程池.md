# Springboot 中使用线程池

在很多场景下，我们需要多线程更加快速高效的执行任务，池化资源可以更好地对线程进行管理控制。

    如果并发的请求数量非常多，但每个线程执行的时间很短，这样就会频繁的创建和销毁线程，如此一来会大大降低系统的效率。可能出现服务器在为每个请求创建新线程和销毁线程上花费的时间和消耗的系统资源要比处理实际的用户请求的时间和资源更多。

springboot 中提供了简单高效的线程池配置方案。不过我们需要先了解下线程池

## Java 线程池

java.uitl.concurrent.ThreadPoolExecutor 类是线程池中最核心的一个类

我们来看一下 ThreadPoolExecutor 的具体实现

```
public class ThreadPoolExecutor extends AbstractExecutorService {
    .....
    public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,
            BlockingQueue<Runnable> workQueue);

    public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,
            BlockingQueue<Runnable> workQueue,ThreadFactory threadFactory);

    public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,
            BlockingQueue<Runnable> workQueue,RejectedExecutionHandler handler);

    public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,
        BlockingQueue<Runnable> workQueue,ThreadFactory threadFactory,RejectedExecutionHandler handler);
    ...
}
```

可以看到 ThreadPoolExecutor 继承自 AbstractExecutorService，有四个构造器。这四个构造器中有 5 个参数是必须有，有两个是可有可不有的。

这些参数的含义分别是：

- int corePoolSize

  核心池的大小。在创建了线程池后，默认情况下，线程池中并没有任何线程，而是等待有任务到来才创建线程去执行任务，除非调用了 prestartAllCoreThreads()或者 prestartCoreThread()方法，这两个方法从名字可以吧看出，是预创建线程的意思。

  默认情况下，在创建了线程池后，线程池中的线程数为 0 ，当有任务来之后，就会创建一个线程去执行任务，当线程池中线程数达到 corePoolSize 后，就会把到达的任务添加进缓冲队列中。

- int maximumPoolSize

  线程池最大线程数，这个参数也是一个非常重要的参数，它表示在线程池中最多能创建多少个线程；

- long keepAliveTime

  表示非核心线程没有任务执行时最多保持多久时间会终止；默认情况下，是作用于非核心线程的，也就是说，只有当线程池中的线程数大于 corePoolSize 时，keepAliveTime 才会起作用，直到线程池中的线程数不大于 corePoolSize，即当线程池中的线程数大于 corePoolSize 时，如果一个线程空闲的时间达到 keepAliveTime，则会终止，直到线程池中的线程数不超过 corePoolSize。

  但是如果调用了 allowCoreThreadTimeOut(boolean)方法，在线程池中的线程数不大于 corePoolSize 时，keepAliveTime 参数也会起作用，直到线程池中的线程数为 0；

- TimeUnit unit

  参数 keepAliveTime 的时间单位，有 7 种取值，在 TimeUnit 类中有 7 种静态属性：

        TimeUnit.DAYS;               //天
        TimeUnit.HOURS;             //小时
        TimeUnit.MINUTES;           //分钟
        TimeUnit.SECONDS;           //秒
        TimeUnit.MILLISECONDS;      //毫秒
        TimeUnit.MICROSECONDS;      //微妙
        TimeUnit.NANOSECONDS;       //纳秒

- BlockingQueue<Runnable> workQueue

  等待队列，当任务提交时，如果线程池中的线程数量大于等于 corePoolSize 的时候，把该任务封装成一个 Worker 对象放入等待队列；

  当提交一个新的任务到线程池以后, 线程池会根据当前线程池中正在运行着的线程的数量来决定对该任务的处理方式，主要有以下几种处理方式(排队策略):

  1. 直接提交

     这种方式常用的队列是 SynchronousQueue，但现在还没有研究过该队列，这里暂时还没法介绍；

  2. 使用无界队列

     一般使用基于链表的阻塞队列 LinkedBlockingQueue。如果使用这种方式，那么线程池中能够创建的最大线程数就是 corePoolSize，而 maximumPoolSize 就不会起作用了（后面也会说到）。当线程池中所有的核心线程都是 RUNNING 状态时，这时一个新的任务提交就会放入等待队列中

  3. 使用有界队列

     一般使用 ArrayBlockingQueue。使用该方式可以将线程池的最大线程数量限制为 maximumPoolSize，这样能够降低资源的消耗，但同时这种方式也使得线程池对线程的调度变得更困难，因为线程池和队列的容量都是有限的值，所以要想使线程池处理任务的吞吐率达到一个相对合理的范围，又想使线程调度相对简单，并且还要尽可能的降低线程池对资源的消耗，就需要合理的设置这两个数量

     - 如果要想降低系统资源的消耗（包括 CPU 的使用率，操作系统资源的消耗，上下文环境切换的开销等）, 可以设置较大的队列容量和较小的线程池容量, 但这样也会降低线程处理任务的吞吐量。
     - 如果提交的任务经常发生阻塞，那么可以考虑通过调用 setMaximumPoolSize() 方法来重新设定线程池的容量。
     - 如果队列的容量设置的较小，通常需要将线程池的容量设置大一点，这样 CPU 的使用率会相对的高一些。但如果线程池的容量设置的过大，则在提交的任务数量太多的情况下，并发量会增加，那么线程之间的调度就是一个要考虑的问题，因为这样反而有可能降低处理任务的吞吐量。

- ThreadFactory threadFactory

  它是 ThreadFactory 类型的变量，用来创建新线程。默认使用 Executors.defaultThreadFactory() 来创建线程。使用默认的 ThreadFactory 来创建线程时，会使新创建的线程具有相同的 NORM_PRIORITY 优先级并且是非守护线程，同时也设置了线程的名称。

- RejectedExecutionHandler handler

  它是 RejectedExecutionHandler 类型的变量，表示线程池的饱和策略。如果阻塞队列满了并且没有空闲的线程，这时如果继续提交任务，就需要采取一种策略处理该任务。线程池提供了 4 种策略(拒绝策略)：

  1. AbortPolicy 直接抛出异常，这是默认策略；
  2. CallerRunsPolicy 用调用者所在的线程来执行任务；
  3. DiscardOldestPolicy 丢弃阻塞队列中靠最前的任务，并执行当前任务；
  4. DiscardPolicy 直接丢弃任务；

其他几个构造器分别依赖这个构造器，我们只需看这个构造器就可以了

```
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

这段代码的逻辑：

- 判断核心线程数：
  1. 如果运行的线程少于 corePoolSize，则创建新线程来处理任务，即使线程池中的其他线程是空闲的；
  2. 如果线程池中的线程数量大于等于 corePoolSize 且小于 maximumPoolSize，则只有当 workQueue 满时才创建新的线程去处理任务；
  3. 如果设置的 corePoolSize 和 maximumPoolSize 相同，则创建的线程池的大小是固定的，这时如果有新任务提交，若 workQueue 未满，则将请求放入 workQueue 中，等待有空闲的线程去从 workQueue 中取任务并处理；
  4. 如果运行的线程数量大于等于 maximumPoolSize，这时如果 workQueue 已经满了，则通过 handler 所指定的策略来处理任务；
- 判断任务队列对象或者线程工厂对象或者拒绝处理对象是否为空
- 赋值

## springboot 中线程池配置与使用

springboot 中通常是使用@Configuration 配置线程池，然后使用@Bean 注解将线程池添加到 spring 容器中

```
@Configuration
public class ThreadPoolConfig{

    @Bean
    public ExecutorService getThreadPool(){
        return newFixedThreadPool(5);
    }
}
```

对于线程池的使用，只需通过指定名称进行加载

```
@Autowired
private ExecutorService downloadQueueThreadPool;

public void todo(){
    executorService.execute(()->{
        System.out.println("线程池打印");
        // 此处内部类引入外部变量需要为泪下按量或者使用final修饰的变量，所以若需要迭代输出，可通过内部类的方式进行输出
    });
}
```

**此处有另外的引入方式,即通过注解@Autowired**
<!-- 
// 在config中配置 @Bean(name = "downloadQueueThreadPool")
@resource(name="downloadQueueThreadPool")
private ExecutorService downloadQueueThreadPool;
 

注解@Autowired 与@resource 的区别是一个是 spring 提供，另一个是 Java 规范，作用类似，
@Autowired注释进行byType注入。
@Resource的作用相当于@Autowired，只不过@Autowired按byType自动注入，而@Resource默认按 byName自动注入罢了

@Resource有两个属性是比较重要的，分是name和type，Spring将@Resource注解的name属性解析为bean的名字，而type属性则解析为bean的类型。
所以如果使用name属性，则使用byName的自动注入策略，
而使用type属性时则使用byType自动注入策略。
如果既不指定name也不指定type属性，这时将通过反射机制使用byName自动注入策略。
 -->
**@Configuration 的作用**

<!-- 
@Configuration 注解本质上还是 @Component

都可以作为配置类

 -->

**@Bean 的作用**

<!-- 
Spring的@Bean注解用于告诉方法，产生一个Bean对象，然后这个Bean对象交给Spring管理。产生这个Bean对象的方法Spring只会调用一次，随后这个Spring将会将这个Bean对象放在自己的IOC容器中。
  -->

**同步与异步**

所谓异步则是：你在餐厅带你了一份饭，然后告诉他们你去外面做其他事情去了,让他们饭做好了叫你(回调)
而同步得分执行则是：你一直呆在餐厅中等待饭做好才去吃饭，中间时间段内并不能去做其他的事情

这儿就需要使用注解 @Async 与@EnableAsync

## demo 示例

- 建立 springboot 项目

  这儿我们我们选择依赖 Rest Repositories

  点击完成便构建了一个 Springboot 项目。

  **Rest Repositories 依赖的作用**

  <!-- 
     RestController注解会返回json，而Controller可以返回jsp页面
   -->

  新建一个 DemoController 类

  新建立一个 DemoController

  ```
    @RestController
    @RequestMapping("/demo")
    public class DemoController {

        @GetMapping
        public Map<String,Object> sayHello(){
            Map<String,Object> map = new HashMap<>();
            map.put("message","Hello world");
            return map;
        }
    }
  ```

- 配置线程池

  在 config 包下建立 ThreadPoolConfig

  配置我们需要的线程池

  ```
    @Configuration
    public class ThreadPoolConfig {

        @Bean
        public ExecutorService getThreadPool() {
            return Executors.newFixedThreadPool(5);
        }
    }
  ```

- 使用线程池

  这里我们有可以看看同步、异步、线程池的效果

  ```
  @Service
  public class CookService {

      @Autowired
      private ExecutorService executorService;
  //	@Bean(name= "downloadQueueThreadPool")
  //	@Resource(name = "downloadQueueThreadPool")
  //	private ExecutorService downloadQueueThreadPool;

      public String cookOneFood() {
          return "同步完成：  " + cook();
      }

      @Async
      public String cookAsyncFood() {
          return "异步完成：  " + cook();
      }

      public List<String> cookMultiFood(int count) {
          final List<Future> futureList = new ArrayList<>();
          final List<String> resultList = new ArrayList<>();

          for(int i = 0; i < count; i++) {
              futureList.add(
                  executorService.submit(()->{
                      return cook();
                  })
              );
          }

          for(Future future: futureList) {
              try {
                  resultList.add("多线程完成： " + future.get());
              } catch (InterruptedException | ExecutionException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              }
          }

          return resultList;
      }

      private String cook() {
          try {
              Thread.sleep(3000);
          } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }
          return "一份煮饭";
      }

  }

  ```

  然后修改相应的 controller
