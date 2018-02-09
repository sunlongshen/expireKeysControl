package com.sunls.demo.consumer;

import com.alibaba.fastjson.JSON;
import com.sunls.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RedisMessageListener implements MessageListener {

	private static Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);
	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			RedisSerializer<?> serializer = redisTemplate.getValueSerializer();
			Object body = serializer.deserialize(message.getBody());
			if (null != body) {
				logger.info("接收到过期消息：" + body);
				Map<String, String> map = JSON.parseObject(body.toString(), HashMap.class);
				if(null != map && !map.isEmpty()) {
					String key = map.get("key");
					String expireKey = map.get("expireKey");
					int hashCode = expireKey.hashCode();
					String lockKey = hashCode+"";
					byte[] keybytes = lockKey.getBytes();
					//锁定15分钟
					if (redisTemplate.getConnectionFactory().getConnection().setNX(keybytes, Boolean.TRUE.toString().getBytes())) {
						try {
							redisTemplate.expire(lockKey,Constants.EXPIRE,TimeUnit.SECONDS);
							execute(key, expireKey);
						}finally {
							redisTemplate.delete(hashCode+"");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void execute(String key,String expireKey) {
		try {
			handleMessage(expireKey);
			redisTemplate.opsForSet().remove(key, expireKey);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("handleMessage失败：" + expireKey);
		}
	}

	public void handleMessage(String message) {
		logger.info("handleMessage接收到要执行的定时任务：" + message);
		// 如果有这个key，
		String key = "myexpirekey:";//业务key前缀
		if (message.indexOf(key) != -1) {
			String str = message.substring(key.length());
			if(null != str && str.length() > 0){
				String[] strs = str.split("_");
				if(strs.length == 2){
					test(strs[0],strs[1]);
				}
			}
			return;
		}

	}

	private void test(String args1,String args2){
		logger.info(args1 + " " + args2);
	}
}
