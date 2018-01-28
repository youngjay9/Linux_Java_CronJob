package com.fet.wm.tasks.engine;

import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fet.wm.tasks.util.EMSTask;

public class TaskEngine {
	
	private static Log logger = LogFactory.getLog(TaskEngine.class);
	
	public void startProcess(){
		
		try{
			int delaySec = 1;
			int cycleSec = 10;
			int threadSleepSec = 3;
			
			Timer timer = new Timer();
			timer.schedule(new EMSTask(), delaySec*1000, cycleSec * 1000);
			
			
			while (true){
				Thread.sleep(threadSleepSec*1000);
			}
			
			
		}
		catch (InterruptedException ex) {
			logger.error("Stop thread failed!!", ex);
		} catch (Exception ex) {
			logger.error("Other Exception failed!", ex);
		}
		
		
	}

	public static void main(String[] args) {
		TaskEngine engine = new TaskEngine();
		engine.startProcess();

	}

}
