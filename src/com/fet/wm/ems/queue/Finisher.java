package com.fet.wm.ems.queue;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.LobHandler;

import com.fet.wm.ems.service.ProcessService;
import com.fet.wm.ems.service.impl.ProcessServiceImpl;
import com.fet.wm.ems.service.impl.TaskServiceImpl;
import com.fet.wm.tasks.util.DateConvert;

public class Finisher implements Runnable {
	
private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(Finisher.class);
	
	private static JdbcTemplate wmJdbcTemplate;
	
	private final LobHandler lobHandler;
	
	private ProcessService processService;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	private EMSQueue processingQueue;
	
	public Finisher(EMSQueue processing){
		processingQueue = processing;
		
		
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		wmJdbcTemplate = (JdbcTemplate)ctx.getBean("wmJdbcTemplate");
		lobHandler = (LobHandler) ctx.getBean("lobHandler");
		processService = new ProcessServiceImpl();
	}
	
	public void updateTaskStatus(String taskId, int status){
		java.util.Date date = new java.util.Date(System.currentTimeMillis());
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("update ems_task set status = ").append(status).append(",");
		sqlStr.append(" last_updtime =").append("TO_DATE('").append(DateConvert.DateToStr(date, DateConvert.LongDatePattern)).append("','yyyy/MM/dd hh24:mi:ss')");
		sqlStr.append(" where taskid ='").append(taskId).append("'");
		
		wmJdbcTemplate.update(sqlStr.toString());
	}
	
	public String getWpresourceNameByResId(String resId){
		String resname = "";
		String sqlStr= "select resname from wp_resource where res_id ='"+resId+"'";
		List<Map<String, Object>> result = wmJdbcTemplate.queryForList(sqlStr);
		if(result!=null && result.size()>0){
			Map<String, Object> row = result.get(0);
			resname = (String)row.get("RESNAME");
		}
		return resname;
	}
	
	public void addTaskErrorMsg(String taskId, String errorMsg){
		
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("select taskid from ems_task_msg where taskid = '").append(taskId).append("'");
		
		List tmpList = wmJdbcTemplate.queryForList(sqlStr.toString());
		
		sqlStr = new StringBuffer();
		
		if(tmpList!=null && tmpList.size()>0){
			
			String ed040 = getWpresourceNameByResId("ED-040");
			
			String ed041 = getWpresourceNameByResId("ED-041");
			java.util.Date date = new java.util.Date(System.currentTimeMillis());
			
			char[] LF = { 0x0A };
			String seperator = new String(LF);
			
			sqlStr.append("update ems_task_msg set msg = '");
			sqlStr.append(ed040).append("TaskEngine").append(" ");
			sqlStr.append(ed041).append(DateConvert.DateToStr(date, DateConvert.LongDatePattern));
			sqlStr.append(seperator).append(errorMsg).append(seperator);
			sqlStr.append("----------------------------------").append(seperator);
			sqlStr.append("' || msg ");
			sqlStr.append("where taskid =").append("'").append(taskId).append("'");
		}
		else{
			sqlStr.append("insert into ems_task_msg(taskid, msg) values(");
			sqlStr.append("'").append(taskId).append("'").append(",");
			sqlStr.append("'").append(errorMsg).append("'");
			sqlStr.append(")");
		}
		
		wmJdbcTemplate.execute(sqlStr.toString());
	}
	
	
	public void run() {
		
		try {
		      while(!Thread.interrupted()) {
		    	   
		        Map<String, Object> row = processingQueue.take();
		        
		        //取得 status:6 處理中 的資料
		        String status = (String)row.get("STATUS");
		        if(!"6".equals(status)){
		        	continue;
		        }
		        
		        final String taskid = (String)row.get("TASKID");
		        
		        String sql = "select * from ems_task where taskid ='"+taskid+"'";
		        
		        logger.info("Finisher take data==>"+sql);
				
		        wmJdbcTemplate.query(sql,
			      new RowMapper(){
		             public Object mapRow(ResultSet rs, int rowNum){
		            	 
		            	 String data = null;
		            	 String msg = null;
		            	 try{
		            		 
		            		 data = lobHandler.getClobAsString(rs, "DATA");
		            		 
		            		 logger.info("Finisher clob data==>"+data);
		            		 
		            		 msg = processService.processData(data);
		            		 
		            		 if(msg == null){
			            		 updateTaskStatus(taskid, 1);
		            		 }
		            		 
		            		 else{
		            			 updateTaskStatus(taskid, 7);
			            		 addTaskErrorMsg(taskid, msg);
		            		 }
		            		
		            	 }/* edn try */
		            	 catch(Exception e){
		            		 e.printStackTrace();
		            		 updateTaskStatus(taskid, 7);
		            		 addTaskErrorMsg(taskid, e.getMessage());
		            	 } 
						 return data;
					 }
			      }
				);
				
		        
				if(processingQueue.isEmpty()){
					logger.info("processingQueue is empty!!");
				}
				
		      }/* end while for Thread is not interrupted */
		      
		    } catch(InterruptedException e) {
		    	
		    	logger.info("Finisher interrupted");
		    }
		
			logger.info("Finisher off");

	}

}
