package com.neo;

import com.neo.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;
import org.springframework.util.StringUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestRedis {

  @Autowired
  private StringRedisTemplate stringRedisTemplate;


  @Autowired
  private RedisTemplate redisTemplate;


  @Test
  public void test() {
    stringRedisTemplate.opsForValue().set("api-A", "1");
    Assert.assertEquals("111", stringRedisTemplate.opsForValue().get("aaa"));
  }

  @Test
  public void testObj() throws Exception {
    User user = new User("aa@126.com", "aa", "aa123456", "aa", "123");
    ValueOperations<String, User> operations = redisTemplate.opsForValue();
    operations.set("com.neox", user);
    operations.set("com.neo.f", user, 1, TimeUnit.SECONDS);
    Thread.sleep(1000);
    boolean exists = redisTemplate.hasKey("com.neo.f");
    if (exists) {
      System.out.println("exists is true");
    } else {
      System.out.println("exists is false");
    }
  }

  @Test
  public void testHash() {
    String path = "qqqqqqqqq";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String todayStr = sdf.format(new Date());
    String redisKey = "apiVisitNum_".concat(todayStr);
    // 获取redis
    Map<Object, Object> objectMap = redisTemplate.opsForHash().entries(redisKey);
    // 获取需要取的 item 的值
    System.out.println(redisKey + "---------------" + objectMap.get(path));
    int num = 1;
    if (objectMap == null) {
      // 如果redis里无当日的api访问数据，存入redis
    } else {
      if (objectMap.get(path) != null) {
        // 如果redis里无当前path的访问数据
        num = Integer.valueOf(objectMap.get(path).toString()) + 1;
      }
    }
    redisTemplate.opsForHash().put(redisKey, path, String.valueOf(num));
  }


  /**
   * 从数据库中获取数据
   *
   * @return
   */
  public String queryFromDB() {
    return "";
  }

  /**
   * 缓存穿透：是指用户查询数据，在数据库没有，自然在缓存中也不会有。这样就导致用户查询的时候，
   * 在缓存中找不到对应key的value，每次都要去数据库再查询一遍，然后返回空(相当于进行了两次 无用的查询)。这样请求就绕过缓存直接查数据库。
   * <p>
   * 解决方案：缓存空值，如果一个查询返回的数据为空(不管是数据不存在，还是系统故障)我们仍然把这个空结果进行缓存，
   * 但它的过期时间会很短，最长不超过五分钟。 通过这个直接设置的默认值存放到缓存，这样第二次到缓冲中获取就有值了，而不会继续访问数据库。
   * <p>
   */
  @Test
  public String testCachePenetration() {
    ValueOperations<String, String> ops = redisTemplate.opsForValue();
    //1、从缓存中读取数据
    String s = ops.get("Jessie");
    if (StringUtils.isEmpty(s)) {
      //2、模拟从数据库中读取数据
      String sFromDB = queryFromDB();
      if (StringUtils.isEmpty(sFromDB)) {
        //库中没有此数据，存入一个空值,过期时间为5分钟
        ops.set("Jessie", "", 5, TimeUnit.MINUTES);
        //返回数据，我这里是测试方法，所以返回空，
        return "";
      } else {
        ops.set("Jessie", sFromDB);
        return sFromDB;
      }
    }
    return "";
  }

  /**
   * 缓存雪崩：如果缓存集中在一段时间内失效，发生大量的缓存穿透，所有的查询都落在数据库上，造成了缓存雪崩。
   * 由于原有缓存失效，新缓存未存储，期间所有原本应该访问缓存的请求都去查询数据库了，而对数据库CPU 和内存造成巨大压力，
   * 严重的会造成数据库宕机。
   * <p>
   * 解决方案：设置不同的过期时间，让缓存失效的时间点尽量均匀。
   * <p>
   */
  @Test
  public void testCacheAvalanche() {
    ValueOperations<String, String> ops = redisTemplate.opsForValue();
    //1、从缓存中读取数据
    String s = ops.get("Jessie");
    if (StringUtils.isEmpty(s)) {
      //2、模拟从数据库中读取数据
      String sFromDB = queryFromDB();
      if (StringUtils.isEmpty(sFromDB)) {
        //库中没有此数据，解决缓存穿透问题,存入一个空值,过期时间为5分钟
        ops.set("Jessie", "", 5, TimeUnit.MINUTES);
        //返回数据，我这里是测试方法，所以返回空，
        return;
      } else {
        //将数据写入缓存，并设置一个随机的过期时间，解决缓存雪崩问题
        //生成5-15之间的一个随机数,设置缓存随机在5-15个小时内过期
        Random random = new Random();
        int randomNum = random.nextInt(10) + 5;
        ops.set("Jessie", sFromDB, randomNum, TimeUnit.HOURS);
        return;
      }
    }
    //缓存中有数据，直接返回
    return;
  }


  /**
   * 缓存击穿是指缓存中没有但数据库中有的数据（一般是缓存时间到期），这时由于并发用户特别多，同时读缓存没读到数据，
   * 又同时去数据库去取数据，引起数据库压力瞬间增大，造成过大压力。
   * <p>
   * 解决方案：加互斥锁，保证同一时刻，只能有一个线程去访问数据库。类似线程安全的懒汉单例模式实现。
   */
  @Test
  public void testRedis() {
    ValueOperations<String, String> ops = redisTemplate.opsForValue();
    //1、从缓存中读取数据
    String s = ops.get("Jessie");
    if (StringUtils.isEmpty(s)) {
      //2、缓存中没有，读取数据库加锁
      synchronized (this) {
        //获取到锁之后，第一个进来的线程，把数据存入缓存，后面得到锁的线程需要判断缓存中有没有数据
        // 避免缓存击穿时，大量请求访问数据库
        String sFormCache = ops.get("Jessie");
        if (!StringUtils.isEmpty(sFormCache)) {
          //缓存中有数据,直接返回数据，我这里是测试方法，就返回null了。
        } else {
          //缓存中没有时，再去数据库中读取数据，模拟从数据库中读取数据
          String sFromDB = queryFromDB();
          if (StringUtils.isEmpty(sFromDB)) {
            //库中没有此数据，解决缓存穿透问题,存入一个空值,过期时间为5分钟
            ops.set("Jessie", "", 5, TimeUnit.MINUTES);
          } else {
            //将数据写入缓存，并设置一个随机的过期时间，避免缓存雪崩问题
            //生成5-15之间的一个随机数,设置缓存随机在5-15个小时内过期
            Random random = new Random();
            int randomNum = random.nextInt(10) + 5;
            ops.set("Jessie", sFromDB, randomNum, TimeUnit.HOURS);
          }
        }
      }
    }
  }

}