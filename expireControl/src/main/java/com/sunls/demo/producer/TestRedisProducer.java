package com.sunls.demo.producer;

import com.sunls.util.Constants;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import redis.clients.jedis.Jedis;

import java.util.Date;

public class TestRedisProducer {
    
    public static void main(String[] args) {
		Jedis jedis = new Jedis("192.168.111.111", 6379);
        jedis.auth("xcRed.,0505");
		long time =new Date().getTime()/1000 + 10;//10秒之后执行
		String args1 = "hello";
		String args2 = "sunls";
		String expireKey = "myexpirekey:"+args1+"_"+args2;
		JdkSerializationRedisSerializer s = new JdkSerializationRedisSerializer();
		jedis.sadd((Constants.EXPIRE_KEY_TASK+time).getBytes(),s.serialize(expireKey));
    }
}