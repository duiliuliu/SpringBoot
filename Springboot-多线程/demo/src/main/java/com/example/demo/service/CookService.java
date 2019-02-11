package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
