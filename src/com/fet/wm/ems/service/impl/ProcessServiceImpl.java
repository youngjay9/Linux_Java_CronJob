package com.fet.wm.ems.service.impl;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.ParseException;
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

import com.fet.wm.ems.service.ProcessService;
import com.fet.wm.tasks.util.DateConvert;

public class ProcessServiceImpl implements ProcessService {
	
	private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(ProcessServiceImpl.class);
	
	private static JdbcTemplate wmJdbcTemplate;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private static SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd");
	
	public ProcessServiceImpl(){
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		wmJdbcTemplate = (JdbcTemplate)ctx.getBean("wmJdbcTemplate");
	}
	
	public String null2Str(Object o){
        if (o == null){
            return "";
        }else{
            return o.toString().trim();
        }
    }
	
	public  String replace(String origin, String oldStr, String newStr) {
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		int oldLength = oldStr.length();
		do {
			int j = origin.indexOf(oldStr, i);
			if (j >= 0) {
				buffer.append(origin.substring(i, j));
				buffer.append(newStr);
				i = j + oldLength;
			} else {
				buffer.append(origin.substring(i));
				return buffer.toString();
			}
		} while (true);
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
	
	public String getStaffLogNameByStaffId(Long staffId){
		String logName = "";
		String sqlStr= "select logname from staff where staff_id ="+staffId;
		List<Map<String, Object>> result = wmJdbcTemplate.queryForList(sqlStr);
		if(result!=null && result.size()>0){
			Map<String, Object> row = result.get(0);
			logName = (String)row.get("LOGNAME");
		}
		return logName;
	}
	
	public long getStaffIdByEmployId(String u_empl_id){
		
		long staffId = 0;
		
		String sqlStr = " select s.* from users u, staff s  where u.u_empl_id ='" + u_empl_id + "' and u.u_login_name = s.amdocs_id and s.status =1";
		
		logger.info("getStaffIdByEmployId==>"+sqlStr);
		
		List<Map<String, Object>> result = wmJdbcTemplate.queryForList(sqlStr);
		
		if(result!=null && result.size()>0){
			Map<String, Object> row = result.get(0);
			
			Number tmpStaffId = (Number)row.get("STAFF_ID");
			
			staffId = tmpStaffId.longValue();
			
		}
		
		
		return staffId;
	}
	
	public String getCustomerNumberBySubscrId(long subscrId){
		
		String customerNumber = null;
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("SELECT CI.CUSTOMER_NUMBER");
		sqlStr.append(" FROM CUSTOMER_INFO CI,CRM_MSIS CM ");
		sqlStr.append(" WHERE CI.CUSTOMER_NUMBER = CM.MSIS_CUSTOMER_NUM ");
		sqlStr.append(" AND CI.MOBILE_PHONE_NUM = CM.MSIS_MSISDN ");
		sqlStr.append(" AND CI.NUMBERFIELD2 = ").append(subscrId);
		
		
		List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sqlStr.toString());
		if(list !=null && list.size()>0){
			list = list.subList(0, 1);
			Map<String, Object> row = list.get(0);
			customerNumber = (String)row.get("CUSTOMER_NUMBER");
		}
		
		return customerNumber;
	}
	
	public String getWMCustomerCommentByCustomerNumber(String customerNumber){
		String comment = null;
		
		StringBuffer sqlStr = new StringBuffer();
		sqlStr.append("select customer_comment from customer_info where customer_number =");
		sqlStr.append("'").append(customerNumber).append("'");
		
		logger.debug("getWMCustomerCommentByCustomerNumber_SQL==>"+sqlStr.toString());
		
		List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sqlStr.toString());
		if(list !=null && list.size()>0){
			list = list.subList(0, 1);
			Map<String, Object> row = list.get(0);
			comment = (String)row.get("CUSTOMER_COMMENT");
		}
		
		return comment;
	}
	
	public String addCustomerInfoComments(Element rootElement){
		String msg = null;
		Node nodeSubsNum = rootElement.selectSingleNode("SubsNum");
		long subscriberID = Long.valueOf(nodeSubsNum.getStringValue());
		
		//Comments sended from EMS Queue
		Node nodeCustomerComment = rootElement.selectSingleNode("CustomerComment");
		String addComment = null2Str(nodeCustomerComment.getStringValue());
		//process Comments content
		addComment = replace(addComment, "'","''");
		
		//get WM customerNumber by subscriberId
		String customerNumber = getCustomerNumberBySubscrId(subscriberID);
		
		if(customerNumber!=null && !"".equals(customerNumber)){
			String newComment = null;
			
			//Comments from WM
			String old_comment = null2Str(getWMCustomerCommentByCustomerNumber(customerNumber));
			
			if(!"".equals(old_comment)){
				StringBuffer sb = new StringBuffer();
				sb.append(addComment).append("&").append(old_comment);
				newComment = sb.toString();
				int length = 0;
				try {
					length = newComment.getBytes("UTF-8").length;
					
					if(length > 250){
						length = 250;
					}
					newComment = newComment.substring(0, length);
				} catch (Exception e) {
					msg = e.getMessage();
					e.printStackTrace();
				}
			}
			else{
				newComment = addComment;
			}
			
			java.util.Date date = new java.util.Date(System.currentTimeMillis());
			
			StringBuffer sql = new StringBuffer(); 
			sql.append(" UPDATE CUSTOMER_INFO SET CUSTOMER_COMMENT = '").append(null2Str(newComment)).append("'").append(",");
			sql.append(" STRINGFIELD10 = ").append("'").append(DateConvert.DateToStr(date, DateConvert.LongDatePattern)).append("'");
			sql.append(" where CUSTOMER_NUMBER = '").append(customerNumber).append("'");
			
			logger.info("addCustomerInfoComments sqlStr==>"+sql.toString());
			
			wmJdbcTemplate.update(sql.toString());
			
		}/* end if customerNumber != null */
		
		
		return msg;
	}
	
		
	
	
	public String addOldEventComments(Long eventNumber, Element rootElement){
		String msg = null;
		Node commentsNode = rootElement.selectSingleNode("Comments");
		Node createdByNode = rootElement.selectSingleNode("CreatedBy");
		
		String comments = null;
		if(commentsNode!=null){
			comments = null2Str(commentsNode.getStringValue());
			//comments is null
			if(comments.equals("") || comments.equals("\"\"")){
				logger.info("JayTest_Comments_is_null");
				return msg = "comments is null";
			}
			
			comments = replace(comments, "'","''");
		}
		
		
		String createdBy = null;
		if(createdByNode!=null){
			createdBy = null2Str(createdByNode.getStringValue());
			if(createdBy.equals("IVR")){
				createdBy = "82891";
			}
		}
		long staffId = 0;
		if(createdBy!=null && !"".equals(createdBy)){
			if("IVR".equals(createdBy)){
				createdBy = "82891";
			}
			staffId = getStaffIdByEmployId(createdBy);
		}
		
		java.util.Date date = new java.util.Date(System.currentTimeMillis());
		
		char[] LF = { 0x0A };
		String seperator = new String(LF);
		
		//update event table
		String sqlStr = "UPDATE event" + " SET MODIFIED_BY = "
				+ staffId + ",MODIFIED_DATE = TO_DATE('"
				+ DateConvert.DateToStr(date, DateConvert.LongDatePattern)
				+ "','yyyy/MM/dd hh24:mi:ss') WHERE EVENT_NUMBER =" + eventNumber;
		
		logger.info("addOldEventComments sqlStr==>"+sqlStr);
		
		int modifyCount = wmJdbcTemplate.update(sqlStr);
		if(modifyCount == 0){
			return msg = "can not find eventNumber";
		}
		
		String logName = null2Str(getStaffLogNameByStaffId(staffId));
		String ed040 = getWpresourceNameByResId("ED-040");
		String ed041 = getWpresourceNameByResId("ED-041");
		
		sqlStr =  "update EVENT_COMMENT set comments = '"+comments +"' where EVENT_NUMBER = "+eventNumber; 
		
		logger.info("addOldEventComments sqlStr==>"+sqlStr);
		
		wmJdbcTemplate.update(sqlStr);
		
		return msg;
	}
	
	public String addNewEvent(Element rootElement){
		String msg = null;
		
		try {
			Node nodeCieId = rootElement.selectSingleNode("CIEId");
			Node nodeCIEDetailId = rootElement.selectSingleNode("CIEDetailId");
			//Node nodeAccountId = rootElement.selectSingleNode("AccountId");
			Node nodeSubsNum = rootElement.selectSingleNode("SubsNum");
			
			
			if(nodeCieId == null || nodeCIEDetailId == null
					 || nodeSubsNum == null){
				return msg = "cieId or cie_dtl_id or subscriberId";
			}
			
			long cieId = Long.valueOf(nodeCieId.getStringValue());
			long cie_dtl_id =  Long.valueOf(nodeCIEDetailId.getStringValue());
			if(cieId == 0 || cie_dtl_id == 0){
				return msg = "cieId or cie_dtl_id = 0";
			}
			
			
			//long accountId = Long.valueOf(nodeAccountId.getStringValue());
			long subscriberID = Long.valueOf(nodeSubsNum.getStringValue());
			String customerNumber = null2Str(this.getCustomerNumberBySubscrId(subscriberID));
			if(customerNumber.equals("")){
				logger.debug("customerNumber is null");
				return msg = "customerNumber is null";
			}
			

			Node nodeCreatedBy = rootElement.selectSingleNode("CreatedBy");
			
			//2016/08/01 modified by jay for CIE Add New Tag --- start
			Node nodeHandleGroup = rootElement.selectSingleNode("HandelGroup");
			Node nodeIdEventCategory = rootElement.selectSingleNode("IdEventCategory");
			//2016/08/01 modified by jay for CIE Add New Tag --- end
			
			
			Node nodeActualStartDate = rootElement.selectSingleNode("ActualStartDate");
			Node nodeActualCompDate = rootElement.selectSingleNode("ActualCompDate");
			Node nodeCreateDate = rootElement.selectSingleNode("CreateDate");
			Node nodeModifiedDate = rootElement.selectSingleNode("ModifiedDate");
			Node nodeHandleBy = rootElement.selectSingleNode("HandleBy");
			Node nodeComments = rootElement.selectSingleNode("Comments");
			Node nodeCampaignDesc = rootElement.selectSingleNode("CampaignDesc");
			Node nodeIdEventType = rootElement.selectSingleNode("IdEventType");
			
			
			long staffId = 0;
			if(nodeCreatedBy!=null){
//				staffId = Long.valueOf(nodeCreatedBy.getStringValue());
				String createdBy = null2Str(nodeCreatedBy.getStringValue());
				if("IVR".equals(createdBy)){
					createdBy = "82891";
				}
				staffId = getStaffIdByEmployId(createdBy);
				
			}
			
			//2016/08/01 modified by jay for CIE Add New Tag --- start
			long handleGroup = 0;
			if(nodeHandleGroup != null){
				handleGroup = Long.valueOf(nodeHandleGroup.getStringValue());
			}
			
			long eventCategory = 0;
			if(nodeIdEventCategory!=null){
				eventCategory = Long.valueOf(nodeIdEventCategory.getStringValue());
			}
			//2016/08/01 modified by jay for CIE Add New Tag --- end
			
			
			//actualStartDate
			String str_actualStartDate = null;
			String actualStartDate = null;
			Date date = null;
			if(nodeActualStartDate !=null){
				str_actualStartDate = null2Str(nodeActualStartDate.getStringValue());
				date = sdf.parse(str_actualStartDate);
				actualStartDate = null2Str(sdf2.format(date));
			}
			//actualCompDate
			String str_actualCompDate = null;
			String actualCompDate = null;
			if(nodeActualCompDate!=null){
				str_actualCompDate = null2Str(nodeActualCompDate.getStringValue());
				date = sdf.parse(str_actualCompDate);
				actualCompDate = null2Str(sdf2.format(date));
			}
			//createdDate
//			String str_createdDate = null;
//			String createdDate = null;
//			if(nodeCreateDate!=null){
//				str_createdDate = null2Str(nodeCreateDate.getStringValue());
//				date = sdf3.parse(str_createdDate);
//				createdDate = null2Str(sdf2.format(date));
//			}
//			if(createdDate == null){
//				createdDate = actualCompDate; 
//			}
			
			
			
			//modifiedDate
			String str_modifiedDate = null;
			String modifiedDate = null;
			if(nodeModifiedDate!=null){
				str_modifiedDate = null2Str(nodeModifiedDate.getStringValue());
				date = sdf.parse(str_modifiedDate);
				modifiedDate = null2Str(sdf2.format(date));
			}
			
//			long handleBy = 0;
//			if(nodeHandleBy!= null){
//				handleBy = Long.valueOf(nodeHandleBy.getStringValue());
//			}
			
			String comments = null;
			if(nodeComments!=null){
				comments = null2Str(nodeComments.getStringValue());
				
				comments = replace(comments, "'","''");
			}
			String campaignDesc = null;
			if(nodeCampaignDesc!=null){
				campaignDesc = null2Str(nodeCampaignDesc.getStringValue());
			}
			
			//2016/08/01 modified by jay for CIE Add New Tag
			int idEventType = 1;
			if(nodeIdEventType!=null){
				idEventType = Integer.valueOf(nodeIdEventType.getStringValue());
			}
			
			//======================== insert event?�event_comment(start) =======================//		
			StringBuffer sqlStr = new StringBuffer();
			
			//?��? event number
			long event_number = wmJdbcTemplate.queryForInt("select SEQMICHELLE.nextVal from dual");
			
			sqlStr.append("Insert into EVENT(EVENT_NUMBER, CAMPAIGN_DESC, SUBS_NUM, CUSTOMER_NUMBER, ID_EVENT_STATUS, ID_EVENT_TYPE, HANDLE_BY, HANDLE_GROUP, ACTUAL_START_DATE, ACTUAL_COMP_DATE, CREATED_BY, CREATED_DATE, MODIFIED_DATE, ID_EVENT_CATEGORY) ");
		    sqlStr.append("Values(");
		    //EVENT_NUMBER
		    sqlStr.append(event_number).append(",");
		    //CAMPAIGN_DESC
		    sqlStr.append("'").append(campaignDesc).append("'").append(",");
		    //SUBS_NUM
		    sqlStr.append(subscriberID).append(",");
		    //CUSTOMER_NUMBER
		    sqlStr.append("'").append(customerNumber).append("'").append(",");
		    //ID_EVENT_STATUS
		    sqlStr.append(2).append(",");
		    //ID_EVENT_TYPE
		    sqlStr.append(idEventType).append(",");
		    //HANDLE_BY
		    sqlStr.append(staffId).append(",");
		    //HANDLE_GROUP
		    sqlStr.append(handleGroup).append(",");
		    //ACTUAL_START_DATE
		    sqlStr.append("TO_DATE('").append(actualStartDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //ACTUAL_COMP_DATE
		    sqlStr.append("TO_DATE('").append(actualCompDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //CREATED_BY
		    sqlStr.append(staffId).append(",");
		    //CREATED_DATE
		    sqlStr.append("TO_DATE('").append(actualCompDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //MODIFIED_DATE
		    sqlStr.append("TO_DATE('").append(modifiedDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //ID_EVENT_CATEGORY
		    sqlStr.append(eventCategory);
		    sqlStr.append(")");
		    
		    logger.info("addNewEvent_insert_event==>"+sqlStr);
		    wmJdbcTemplate.execute(sqlStr.toString());
		    
		    //event_comment
		    sqlStr = new StringBuffer();
		    sqlStr.append("Insert into EVENT_COMMENT(EVENT_NUMBER, COMMENTS) Values(");
		    sqlStr.append(event_number).append(",");
		    sqlStr.append("'").append(comments).append("'");
		    sqlStr.append(")");
		    
		    logger.info("addNewEvent_insert_event_comment==>"+sqlStr); 
		    wmJdbcTemplate.execute(sqlStr.toString());
		    //======================== insert event?�event_comment(end) ====================//
		 
		    //======================== insert cie_wm_mapping(start) =======================//
		    sqlStr = new StringBuffer();
		    sqlStr.append("Insert into CIE_WM_MAPPING(EVENT_NUMBER, CIE_ID, CIE_DTL_ID) Values(");
		    sqlStr.append(event_number).append(",");
		    sqlStr.append(cieId).append(",");
		    sqlStr.append(cie_dtl_id);
		    sqlStr.append(")");
		    
		    logger.info("addNewEvent_cie_wm_mapping==>"+sqlStr); 
		    wmJdbcTemplate.execute(sqlStr.toString());
		    //======================== insert cie_wm_mapping(end) =======================//
		    
		} catch (ParseException e) {
			msg = e.getMessage();
			e.printStackTrace();
		}catch (Exception e) {
			msg = e.getMessage();
			e.printStackTrace();
		}
		
		
		return msg;
	}
	

	public String addNewEventForCIEThirdRemark(Element rootElement){
		String msg = null;
		
		try {
			Node nodeCieId = rootElement.selectSingleNode("CIEId");
			Node nodeCIEDetailId = rootElement.selectSingleNode("CIEDetailId");
			//Node nodeAccountId = rootElement.selectSingleNode("AccountId");
			Node nodeSubsNum = rootElement.selectSingleNode("SubsNum");
			
			
			if(nodeCieId == null || nodeCIEDetailId == null
					 || nodeSubsNum == null){
				return msg = "cieId or cie_dtl_id or subscriberId is null";
			}
			
			long cieId = Long.valueOf(nodeCieId.getStringValue());
			long cie_dtl_id =  Long.valueOf(nodeCIEDetailId.getStringValue());
			if(cieId == 0 || cie_dtl_id == 0){
				return msg = "cieId or cie_dtl_id = 0";
			}
			
			
			//long accountId = Long.valueOf(nodeAccountId.getStringValue());
			long subscriberID = Long.valueOf(nodeSubsNum.getStringValue());
			String customerNumber = null2Str(this.getCustomerNumberBySubscrId(subscriberID));
			if(customerNumber.equals("")){
				logger.debug("customerNumber is null");
				return msg = "customerNumber is null";
			}
			

			Node nodeCreatedBy = rootElement.selectSingleNode("CreatedBy");
			
			//2016/08/01 modified by jay for CIE Add New Tag --- start
			Node nodeHandleGroup = rootElement.selectSingleNode("HandelGroup");
			Node nodeIdEventCategory = rootElement.selectSingleNode("IdEventCategory");
			//2016/08/01 modified by jay for CIE Add New Tag --- end
			
			
			Node nodeActualStartDate = rootElement.selectSingleNode("ActualStartDate");
			Node nodeActualCompDate = rootElement.selectSingleNode("ActualCompDate");
			Node nodeCreateDate = rootElement.selectSingleNode("CreateDate");
			Node nodeModifiedDate = rootElement.selectSingleNode("ModifiedDate");
			Node nodeHandleBy = rootElement.selectSingleNode("HandleBy");
			Node nodeComments = rootElement.selectSingleNode("SysComments");
			Node nodeCampaignDesc = rootElement.selectSingleNode("CampaignDesc");
			Node nodeIdEventType = rootElement.selectSingleNode("IdEventType");
			
			
			long staffId = 0;
			if(nodeCreatedBy!=null){
//				staffId = Long.valueOf(nodeCreatedBy.getStringValue());
				String createdBy = null2Str(nodeCreatedBy.getStringValue());
				if("IVR".equals(createdBy)){
					createdBy = "82891";
				}
				staffId = getStaffIdByEmployId(createdBy);
			}
			
			//2016/08/01 modified by jay for CIE Add New Tag --- start
			long handleGroup = 0;
			if(nodeHandleGroup != null){
				handleGroup = Long.valueOf(nodeHandleGroup.getStringValue());
			}
			
			long eventCategory = 0;
			if(nodeIdEventCategory!=null){
				eventCategory = Long.valueOf(nodeIdEventCategory.getStringValue());
			}
			//2016/08/01 modified by jay for CIE Add New Tag --- end
			
			
			//actualStartDate
			String str_actualStartDate = null;
			String actualStartDate = null;
			Date date = null;
			if(nodeActualStartDate !=null){
				str_actualStartDate = null2Str(nodeActualStartDate.getStringValue());
				date = sdf.parse(str_actualStartDate);
				actualStartDate = null2Str(sdf2.format(date));
			}
			//actualCompDate
			String str_actualCompDate = null;
			String actualCompDate = null;
			if(nodeActualCompDate!=null){
				str_actualCompDate = null2Str(nodeActualCompDate.getStringValue());
				date = sdf.parse(str_actualCompDate);
				actualCompDate = null2Str(sdf2.format(date));
			}
			//createdDate
			String str_createdDate = null;
//			String createdDate = null;
//			if(nodeCreateDate!=null){
//				str_createdDate = null2Str(nodeCreateDate.getStringValue());
//				date = sdf3.parse(str_createdDate);
//				createdDate = null2Str(sdf2.format(date));
//			}
//			if(createdDate == null){
//				createdDate = actualCompDate; 
//			}
			
			//modifiedDate
			String str_modifiedDate = null;
			String modifiedDate = null;
			if(nodeModifiedDate!=null){
				str_modifiedDate = null2Str(nodeModifiedDate.getStringValue());
				date = sdf.parse(str_modifiedDate);
				modifiedDate = null2Str(sdf2.format(date));
			}
			
//			long handleBy = 0;
//			if(nodeHandleBy!= null){
//				handleBy = Long.valueOf(nodeHandleBy.getStringValue());
//			}
			
			String comments = null;
			if(nodeComments!=null){
				comments = null2Str(nodeComments.getStringValue());
				
				comments = replace(comments, "'","''");
			}
			String campaignDesc = null;
			if(nodeCampaignDesc!=null){
				campaignDesc = null2Str(nodeCampaignDesc.getStringValue());
			}
			
			//2016/08/01 modified by jay for CIE Add New Tag
			int idEventType = 1;
			if(nodeIdEventType!=null){
				idEventType = Integer.valueOf(nodeIdEventType.getStringValue());
			}
			
			//======================== insert event?�event_comment(start) =======================//		
			StringBuffer sqlStr = new StringBuffer();
			
			//?��? event number
			long event_number = wmJdbcTemplate.queryForInt("select SEQMICHELLE.nextVal from dual");
			
			sqlStr.append("Insert into EVENT(EVENT_NUMBER, CAMPAIGN_DESC, SUBS_NUM, CUSTOMER_NUMBER, ID_EVENT_STATUS, ID_EVENT_TYPE, HANDLE_BY, HANDLE_GROUP, ACTUAL_START_DATE, ACTUAL_COMP_DATE, CREATED_BY, CREATED_DATE, MODIFIED_DATE, ID_EVENT_CATEGORY) ");
		    sqlStr.append("Values(");
		    //EVENT_NUMBER
		    sqlStr.append(event_number).append(",");
		    //CAMPAIGN_DESC
		    sqlStr.append("'").append(campaignDesc).append("'").append(",");
		    //SUBS_NUM
		    sqlStr.append(subscriberID).append(",");
		    //CUSTOMER_NUMBER
		    sqlStr.append("'").append(customerNumber).append("'").append(",");
		    //ID_EVENT_STATUS
		    sqlStr.append(2).append(",");
		    //ID_EVENT_TYPE
		    sqlStr.append(idEventType).append(",");
		    //HANDLE_BY
		    sqlStr.append(staffId).append(",");
		    //HANDLE_GROUP
		    sqlStr.append(handleGroup).append(",");
		    //ACTUAL_START_DATE
		    sqlStr.append("TO_DATE('").append(actualStartDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //ACTUAL_COMP_DATE
		    sqlStr.append("TO_DATE('").append(actualCompDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //CREATED_BY
		    sqlStr.append(staffId).append(",");
		    //CREATED_DATE
		    sqlStr.append("TO_DATE('").append(actualCompDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //MODIFIED_DATE
		    sqlStr.append("TO_DATE('").append(modifiedDate).append("', 'MM/DD/YYYY HH24:MI:SS')").append(",");
		    //ID_EVENT_CATEGORY
		    sqlStr.append(eventCategory);
		    sqlStr.append(")");
		    
		    logger.info("addNewEventForCIEThirdRemark_insert_event==>"+sqlStr);
		    wmJdbcTemplate.execute(sqlStr.toString());
		    
		    //event_comment
		    sqlStr = new StringBuffer();
		    sqlStr.append("Insert into EVENT_COMMENT(EVENT_NUMBER, COMMENTS) Values(");
		    sqlStr.append(event_number).append(",");
		    sqlStr.append("'").append(comments).append("'");
		    sqlStr.append(")");
		    
		    logger.info("addNewEventForCIEThirdRemark_insert_event_comment==>"+sqlStr); 
		    wmJdbcTemplate.execute(sqlStr.toString());
		    //======================== insert event?�event_comment(end) ====================//
		 
		} catch (ParseException e) {
			msg = e.getMessage();
			e.printStackTrace();
		}catch (Exception e) {
			msg = e.getMessage();
			e.printStackTrace();
		}
		
		
		return msg;
	}
	
			
	
	
	public String processData(String data){
		
		String msg = null;
		
		try {
			StringReader in = new StringReader(data);
			SAXReader reader = new SAXReader();
			Document document = reader.read(in);
			Element rootElmt = document.getRootElement();

			Node nodeCieId = rootElmt.selectSingleNode("CIEId");
			Node nodeCieDtlId = rootElmt.selectSingleNode("CIEDetailId");
			
			
			Node nodeComments = rootElmt.selectSingleNode("Comments");
			Node nodeSysComments = rootElmt.selectSingleNode("SysComments");
			Node nodeCustomerComment = rootElmt.selectSingleNode("CustomerComment");
			
			if(nodeCustomerComment!=null){
				msg = addCustomerInfoComments(rootElmt);
				return msg;
			}
			if(nodeComments == null && nodeSysComments == null){
				return msg = "noComments";
			}
			
			if(nodeCieId!=null){
				
				Long cieId = Long.valueOf(nodeCieId.getStringValue());
				Long cieDtlId = Long.valueOf(nodeCieDtlId.getStringValue());
				
				String sql = "select * from cie_wm_mapping where cie_id ="+cieId + "  and cie_dtl_id ="+cieDtlId;
				List<Map<String, Object>> list = wmJdbcTemplate.queryForList(sql);
				
				
				if(list!=null && list.size()>0){
					Map<String, Object> row = list.get(0);
					long eventNumber = ((BigDecimal)row.get("EVENT_NUMBER")).longValue();
					
					if(nodeComments != null){
						msg = addOldEventComments(eventNumber, rootElmt);
					}
				}
				
				else{
					if(nodeComments != null){
						msg = addNewEvent(rootElmt);
					}
				}
				
				
				if(nodeSysComments!=null){
					msg = addNewEventForCIEThirdRemark(rootElmt);
				}
				
				
			}/* end if nodeCieId!=null */
			
		}/* end try */ 
		catch (DocumentException e) {
			msg = e.getMessage();
			e.printStackTrace();
		}/* end catch */
		
		return msg;
	}

}
