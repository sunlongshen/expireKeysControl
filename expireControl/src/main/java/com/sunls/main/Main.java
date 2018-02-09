package com.sunls.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;

public class Main {
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	public static void main(String[] args) {
		try {
			logger.info("spring init");
			GenericXmlApplicationContext context = new GenericXmlApplicationContext();
			context.setValidating(false);
			context.load("classpath*:/applicationContext.xml");
			context.refresh();
		}catch (Exception e) {
			logger.info("spring init error");
			e.printStackTrace();
		}
	}
}