package com.neo;

import com.neo.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestRedis {

  @Autowired
  private StringRedisTemplate stringRedisTemplate;


  @Autowired
  private RedisTemplate redisTemplate;


  @Test
  public void test() throws Exception {
    stringRedisTemplate.opsForValue().set("api-A", "1");
    Assert.assertEquals("111", stringRedisTemplate.opsForValue().get("aaa"));
    System.out.println("11111111111111111111");
  }

  @Test
  public void testObj() throws Exception {
    User user = new User("aa@126.com", "aa", "aa123456", "aa", "123");
    ValueOperations<String, User> operations = redisTemplate.opsForValue();
    operations.set("com.neox", user);
    operations.set("com.neo.f", user, 1, TimeUnit.SECONDS);
    Thread.sleep(1000);
    //redisTemplate.delete("com.neo.f");
    boolean exists = redisTemplate.hasKey("com.neo.f");
    if (exists) {
      System.out.println("exists is true");
    } else {
      System.out.println("exists is false");
    }
    // Assert.assertEquals("aa", operations.get("com.neo.f").getUserName());
  }

  @Test
  public void testHash() {
    String path ="qqqqqqqqq";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    String todayStr = sdf.format(new Date());
    String redisKey = "apiVisitNum_".concat(todayStr);
    // 获取redis
    Map<Object, Object> objectMap = redisTemplate.opsForHash().entries(redisKey);
    // 获取需要取的 item 的值
    System.out.println(redisKey+"---------------"+objectMap.get(path));
    int num = 1;
    if (objectMap == null) {
      // 如果redis里无当日的api访问数据，存入redis
      redisTemplate.opsForHash().put(redisKey, path, String.valueOf(num));
    } else {
      if (objectMap.get(path) != null) {
        // 如果redis里无当前path的访问数据
        num = Integer.valueOf(objectMap.get(path).toString()) + 1;
        redisTemplate.opsForHash().put(redisKey, path, String.valueOf(num));
      } else {
        redisTemplate.opsForHash().put(redisKey, path, String.valueOf(num));
      }
    }
  }
}