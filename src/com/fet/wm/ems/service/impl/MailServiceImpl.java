package com.fet.wm.ems.service.impl;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.fet.wm.ems.service.MailService;

public class MailServiceImpl implements MailService {
	
	private static final String CONTEXT_NAME = "ems-queue-daemon-context.xml";
	
	private static Log logger = LogFactory.getLog(MailServiceImpl.class);
	
	private static JavaMailSenderImpl mailSender;
	
	private static final String fromMail = "wmEmsQueue@guide.fetnet.net";
	
	private static final String[] toMail = {"yachhung@fareastone.com.tw"};
	
	public MailServiceImpl(){
		ApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_NAME);
		mailSender = (JavaMailSenderImpl)ctx.getBean("mailSender");
	}
	
	
	public void sendMail(String emailSubject, String emailContent) {
		
		try {
			MimeMessage message = mailSender.createMimeMessage();
			
			MimeMessageHelper helper = new MimeMessageHelper(message);
			helper.setSubject(emailSubject);
			helper.setFrom(fromMail);
			helper.setTo(toMail);
			helper.setText(emailContent, false);
			
			mailSender.send(message);
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public  void testSendingMail(){
		sendMail("WM_EMSQueue", "JayTest Hello PingPing!!");
	}
	
	public static void main(String[] args){
		
		MailServiceImpl mailService = new MailServiceImpl();
		mailService.testSendingMail();
	}
	
	
}
