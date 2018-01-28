package com.fet.wm.ems.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.LobHandler;

import com.fet.wm.ems.service.ProcessService;
import com.fet.wm.ems.service.TaskService;
import com.fet.wm.tasks.util.DateConvert;

public class TaskServiceImpl implements TaskService {
	
	private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(TaskServiceImpl.class);
	
	private static JdbcTemplate wmJdbcTemplate;
	
	private final LobHandler lobHandler;
	
	private ProcessService processService;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	
	public TaskServiceImpl(){
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		wmJdbcTemplate = (JdbcTemplate)ctx.getBean("wmJdbcTemplate");
		lobHandler = (LobHandler) ctx.getBean("lobHandler");
		processService = new ProcessServiceImpl();
	}
	
	public void testRead(){
		
		String sql = " select TASKID from ems_task where status = 4 and channel_type = 'NCP' ";
		
		sql += " and scheduledon >= TO_DATE ('2016-12-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') and rownum <=1000 ";
		
		
		List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sql);
		
		for(Map<String, Object> row: list){
			
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
		            		 
		            		 StringReader in = new StringReader(data);
		         			 SAXReader reader = new SAXReader();
		         			 Document document = reader.read(in);
		         			 Element rootElmt = document.getRootElement();
		         			 
		         			 Node nodeCreatedBy = rootElmt.selectSingleNode("CreatedBy");
		         			 
		         			 String strCreatedBy = null;
		    				 
		    				 if(nodeCreatedBy != null){
		    					 
		    					 strCreatedBy = nodeCreatedBy.getStringValue();
		    					 
		    					 String updSql = null;
		    					 
		    					 if("IVR".equals(strCreatedBy)){
		    						 updSql = " update ems_task set channel_type = 'IVR' where taskid = '" + taskid + "'";
		    						 
		    						 wmJdbcTemplate.update(updSql);
		    						 
		    						 logger.info("updSql:"+updSql);
		    					 }
		    					 else{
		    						 updSql = " update ems_task set channel_type = 'NCP' where taskid = '" + taskid + "'";
		    						 
		    						 wmJdbcTemplate.update(updSql);
		    						 
		    						 logger.info("updSql:"+updSql);
		    					 }
		    				 }
		            		 
		            		
		            	 }/* edn try */
		            	 catch(Exception e){
		            		 e.printStackTrace();
		            	 } 
						 return data;
					 }
			      }
				);
			}
			catch(Exception e){
				 e.printStackTrace();
			}
			
			
			
		}/* end for */
	}
	
	public void testRead2(){
		
		File file = new File("c:\\tmp\\ems.txt");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line = null;
			
			while( (line = br.readLine()) != null){
				
				final String taskid = line.trim();
				
				String sql = "select * from r_ems_task where taskid ='"+taskid+"' and status =4";
				
				System.out.println("sql==>"+sql);
				
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
			            			 
			            			 System.out.println("Success!!");
			            			 
				            		 updateTaskStatus(taskid, 1);
			            		 }
			            		 //??errorMsg
			            		 else{
			            			 System.out.println("Fail!!");
			            			 
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
				
				
			}/* end while */
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	
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
	
	public List<Map<String, Object>> getProcessingList(){
		List<Map<String, Object>> result = null;
		
		String sql = " select * from ems_task where channel_type = 'NCP' and status = 6";
		
		result = wmJdbcTemplate.queryForList(sql);
		
		return result;
	}
	
	public void readTask(){
		
		String sql = "select taskid from ems_task where channel_type = 'NCP' and status = 4 and rownum <=500 order by scheduledon asc";

		List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sql);
		
		updateProcessingStatus(list);
		
		List<Map<String, Object>> processingList = getProcessingList();
		
		for(Map<String, Object> row:processingList){
			
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
	
public void readIVRTask(){
		
		String sql = "select taskid from ems_task where status = 4 and  channel_type = 'IVR' and rownum <500  order by scheduledon desc";
		
		List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sql);
		
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
	
	/**
	 * ?�改 ems_task 每�?記�??��??��?
	 * 4:pending 1:success 7:frozen
	 * @param taskId
	 * @param status
	 */
	public void updateTaskStatus(String taskId, int status){
		java.util.Date date = new java.util.Date(System.currentTimeMillis());
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("update ems_task set status = ").append(status).append(",");
		sqlStr.append(" last_updtime =").append("TO_DATE('").append(DateConvert.DateToStr(date, DateConvert.LongDatePattern)).append("','yyyy/MM/dd hh24:mi:ss')");
		sqlStr.append(" where taskid ='").append(taskId).append("'");
		
		wmJdbcTemplate.update(sqlStr.toString());
	}
	
	/**
	 * 紀??ems_task 每�?記�??�錯誤�?�?
	 * @param taskId
	 * @param errorMsg
	 */
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
	
	public static void main(String[] args){
		
		TaskServiceImpl service = new TaskServiceImpl();
		
		service.testRead2();
		
	}
}
