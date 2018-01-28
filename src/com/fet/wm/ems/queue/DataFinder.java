package com.fet.wm.ems.queue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class DataFinder implements Runnable {
	
	private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(DataFinder.class);
	
	private Random rand = new Random(47);
	
	private static JdbcTemplate wmJdbcTemplate;
	
	private EMSQueue emsQueue;
	
	public DataFinder(EMSQueue eq){
		emsQueue = eq;
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		wmJdbcTemplate = (JdbcTemplate)ctx.getBean("wmJdbcTemplate");
	}
	
	
	public void run() {
		
		try {
		      while(!Thread.interrupted()) {
		    	  
		        TimeUnit.MILLISECONDS.sleep(100 + rand.nextInt(500));
		          
		        // Find data
		        String sql = "select taskid from ems_task where channel_type = 'NCP' and status = 4 and rownum <=500 order by scheduledon asc";

				List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sql);
		        
				for(Map<String, Object> row:list){
					 // Insert into queue
					emsQueue.put(row);
				}
				
		      }/* end while for Thread is not interrupted */
		      
		} catch(InterruptedException e) {
		    	logger.info("DataFinder interrupted");
		}
		logger.info("DataFinder off");

	}

}
