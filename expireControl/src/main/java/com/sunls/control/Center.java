package com.sunls.control;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.sunls.util.Constants;

@Component
public class Center {

	private static Logger logger = LoggerFactory.getLogger(Center.class);
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Value("#{configProperties['checkTime']}")  
	private Integer value = 0;

	/**
	 * 1秒钟可以干很多事情，每秒扫描过期的key，分发到其他服务器执行,
	 * 该扫描服务是单点的，可能发生单点故障，
	 * 所以需要部署几个监控程序，修改checkTime > 0 表示扫描多少秒之前的过期key
	 * 这块redis最好用集群，因为单个redis压力过大，访问时间大于1秒，将会出现线程阻塞的问题
	 * 目前测试，每秒1万个key没有问题
	 */
	@Scheduled(cron = "0/1 * * * * ?")
	public void center() {
		try {
			execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Async
	public void execute() {
		try {
			//扫描当前过期的key，value大于0的时候作为监控程序，扫描value秒之前过期的key
			long time = System.currentTimeMillis() / 1000 - value;
			logger.info("center执行时间:" + time);
			String key = Constants.EXPIRE_KEY_TASK + time;
			Set<String> keys = redisTemplate.opsForSet().members(key);
			if (null != keys) {
				for (String expireKey : keys) {
					Map<String, String> map = new HashMap<String, String>(2);
					map.put("key", key);
					map.put("expireKey", expireKey);
//					这块有两种方案，
// 					方案一：基于redis过期key通知，需要修改redis的配置，过期key会有延时，
//					过期key失效后，再次调用后会让过期key事件通知立刻执行，这个redis是单点的，目前的集群都不支持过期key的事件通知
//					redisTemplate.opsForValue().get(expireKey);
//					方案二：扫描到的key，分发到其他服务器执行，这块暂时以redis pub/Sub 为例
					redisTemplate.convertAndSend("topic.channel", JSON.toJSONString(map));
					logger.info("发送过期key:" + expireKey);
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
