package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.CookService;

@RestController
@RequestMapping("/demo")
public class DemoController {
	
	@Autowired 
	private CookService cookService;
	
	@GetMapping("/one")
	public Map<String,Object> cookOne(
			@RequestParam("name") final String name
			){
		long start = System.currentTimeMillis();
		
		Map<String,Object> map = new HashMap<>();
		map.put("顾客",name);
		map.put("份数",1);
		map.put("食物",cookService.cookOneFood());
		map.put("耗时",System.currentTimeMillis()-start);
		
		return map;
	}
	
	@GetMapping("/async")
	public Map<String,Object> cookAsync(
			@RequestParam("name") final String name
			){
		long start = System.currentTimeMillis();
		
		Map<String,Object> map = new HashMap<>();
		map.put("顾客",name);
		map.put("份数",1);
		map.put("食物",cookService.cookAsyncFood());
		map.put("耗时",System.currentTimeMillis()-start);
		
		return map;
	}
	
	@GetMapping("/multi")
	public Map<String,Object> cookMulti(
			 @RequestParam("name") final String name,
			 @RequestParam("count") final int count
	            ){
		long start = System.currentTimeMillis();
		
		Map<String,Object> map = new HashMap<>();
		map.put("顾客",name);
		map.put("份数",count);
		map.put("食物",cookService.cookMultiFood(count));
		map.put("耗时",System.currentTimeMillis()-start);
		
		return map;
	}
}