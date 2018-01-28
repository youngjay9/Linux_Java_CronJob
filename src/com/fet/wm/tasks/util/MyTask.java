package com.fet.wm.tasks.util;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.LobHandler;

import com.fet.wm.ems.service.ProcessService;
import com.fet.wm.ems.service.impl.ProcessServiceImpl;

public class MyTask extends TimerTask {

private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(MyTask.class);
	
	private static JdbcTemplate wmJdbcTemplate;
	
	private static final  String selectSQL = "select taskid from ems_task where channel_type = 'NCP' and status = 6 and scheduledon >= TO_DATE ('2016-12-02 00:00:00', 'YYYY-MM-DD HH24:MI:SS')  and scheduledon <= TO_DATE ('2016-12-02 23:59:59', 'YYYY-MM-DD HH24:MI:SS')";
	
	private final LobHandler lobHandler;
	
	private ProcessService processService;
	
	
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	public  MyTask(){
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		wmJdbcTemplate = (JdbcTemplate)ctx.getBean("wmJdbcTemplate");
		lobHandler = (LobHandler) ctx.getBean("lobHandler");
		processService = new ProcessServiceImpl();
		
	}
	
	public void updateProcessingStatus(List<Map<String, Object>> list){
		java.util.Date date = new java.util.Date(System.currentTimeMillis());
		 
		for(Map<String, Object> row:list){
			String taskid = (String)row.get("TASKID");
			 StringBuffer sqlStr = new StringBuffer();
			sqlStr.append("update ems_task set status = ").append(6).append(",");
			sqlStr.append(" last_updtime =").append("TO_DATE('").append(DateConvert.DateToStr(date, DateConvert.LongDatePattern)).append("','yyyy/MM/dd hh24:mi:ss')");
			sqlStr.append(" where taskid ='").append(taskid).append("'");
			
			wmJdbcTemplate.update(sqlStr.toString());
		}
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
	
	public void updateTaskStatus(String taskId, int status){
		java.util.Date date = new java.util.Date(System.currentTimeMillis());
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("update ems_task set status = ").append(status).append(",");
		sqlStr.append(" last_updtime =").append("TO_DATE('").append(DateConvert.DateToStr(date, DateConvert.LongDatePattern)).append("','yyyy/MM/dd hh24:mi:ss')");
		sqlStr.append(" where taskid ='").append(taskId).append("'");
		
		wmJdbcTemplate.update(sqlStr.toString());
	}
	
public void addTaskErrorMsg(String taskId, String errorMsg){
		
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("select taskid from ems_task_msg where taskid = '").append(taskId).append("'");
		//檢查?�否已�? errotMsg
		List tmpList = wmJdbcTemplate.queryForList(sqlStr.toString());
		
		sqlStr = new StringBuffer();
		//追�? error msg
		if(tmpList!=null && tmpList.size()>0){
			//修改??
			String ed040 = getWpresourceNameByResId("ED-040");
			//修改?��?
			String ed041 = getWpresourceNameByResId("ED-041");
			java.util.Date date = new java.util.Date(System.currentTimeMillis());
			//?��?符�?
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
		
		List<Map<String, Object>> list = wmJdbcTemplate.queryForList(selectSQL);

		
		
		String sql = null;
		
		for(Map<String, Object> row:list){
			
			final String taskid = (String)row.get("TASKID");
			
			sql = "select * from ems_task where taskid ='"+taskid+"'";
			
			try{
				wmJdbcTemplate.query(sql,
			      new RowMapper(){
		             public Object mapRow(ResultSet rs, int rowNum){
		            	 
		            	 String data = null;
		            	 String msg = null;
		            	 try{
		            		 
		            		 data = lobHandler.getClobAsString(rs, "DATA");
		            		 msg = processService.processData(data);
		            		 
		            		 if(msg == null){
			            		 updateTaskStatus(taskid, 1);
		            		 }
		            		 //??errorMsg
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
			}
			catch(Exception e){
				 e.printStackTrace();
       		 	updateTaskStatus(taskid, 7);
       		 	addTaskErrorMsg(taskid, e.getMessage());
			}
			
		}/*end for*/
	}

}
