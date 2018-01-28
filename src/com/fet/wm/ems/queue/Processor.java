package com.fet.wm.ems.queue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fet.wm.tasks.util.DateConvert;

public class Processor implements Runnable {
	
	private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(Processor.class);
	
	private EMSQueue initQueue, processingQueue;
	
	private static JdbcTemplate wmJdbcTemplate;
	
	
	public Processor(EMSQueue init, EMSQueue processing){
		initQueue = init;
		processingQueue = processing;
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		wmJdbcTemplate = (JdbcTemplate)ctx.getBean("wmJdbcTemplate");
	}

	
	public void run() {
		
		try {
		      while(!Thread.interrupted()) {
		    	   
		        Map<String, Object> row = initQueue.take();
		        
		        //更改 status:6 處理中
		        String taskId = (String)row.get("TASKID");
		        
		        java.util.Date date = new java.util.Date(System.currentTimeMillis());
		        StringBuffer sqlStr = new StringBuffer();
				sqlStr.append("update ems_task set status = ").append(6).append(",");
				sqlStr.append(" last_updtime =").append("TO_DATE('").append(DateConvert.DateToStr(date, DateConvert.LongDatePattern)).append("','yyyy/MM/dd hh24:mi:ss')");
				sqlStr.append(" where taskid ='").append(taskId).append("'");
				
				wmJdbcTemplate.update(sqlStr.toString());
				
				logger.info("Processor update status==>"+sqlStr.toString());
				
				row.put("STATUS", "6");
				
				processingQueue.put(row);
				
				if(initQueue.isEmpty()){
					logger.info("initQueue is empty!!");
				}
				
		      }/* end while for Thread is not interrupted */
		      
		    } catch(InterruptedException e) {
		    	
		    	logger.info("Processor interrupted");
		    }
		
			logger.info("Processor off");


	}

}
