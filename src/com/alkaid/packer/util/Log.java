package com.alkaid.packer.util;

import java.awt.Color;

import javax.swing.JTextArea;

import com.alkaid.packer.view.JTextPaneAdv;


public class Log {
	private static boolean ShowDebugInfo=true;
	public static enum Tag{
		debug,info,warn,error
	}
	public static interface OnLogListener{
		public void onLog(LogInfo logInfo);
	}
	public static class DefaultOnLogListener implements OnLogListener{
		public void onLog(LogInfo logInfo) {
			Log.log(logInfo);
		}
	}
	private JTextArea talog=null;
	private JTextPaneAdv tplog=null;
	private static Log instance=null;
	
	private Log(){}
	
	public static Log init(JTextArea talog){
		if(instance==null){
			instance=new Log();
		}
		instance.talog=talog;
		instance.tplog=null;
		return instance;
	}
	public static Log init(JTextPaneAdv tplog){
		if(instance==null){
			instance=new Log();
		}
		instance.talog=null;
		instance.tplog=tplog;
		return instance;
	}
	public static Log getInstance(){
		return instance;
	}
	
	public static void d(String msg){
		log(Tag.debug,msg);
	}
	public static void i(String msg){
		log(Tag.info,msg);
	}
	public static void w(String msg){
		log(Tag.warn,msg);
	}
	public static void e(String msg){
		log(Tag.error,msg);
	}
	
	public static void log(LogInfo logInfo){
		log(logInfo.tag,logInfo.msg);
	}
	
	public static void log(Tag tag,String msg){
		if(!ShowDebugInfo&&tag==Tag.debug)
			return;
		Color color=Color.BLACK;
		String a="";
		switch (tag) {
		case debug:
			color=Color.BLACK;
			break;
		case info:
			color=Color.BLUE;
			a="   ";
			break;
		case warn:
			color=Color.ORANGE;
			a="   ";
			break;
		case error:
			color=Color.RED;
		default:
			break;
		}
		msg=tag.name()+a+" | "+msg+"\n";
		if(null!=instance.talog){
			instance.talog.append(msg);
		}
		if(null!=instance.tplog){
			instance.tplog.setColor(color);
			instance.tplog.append(msg);
		}
	}
	
	public static class LogInfo{
		public Tag tag;
		public String msg;
		public LogInfo(Tag tag,String msg){
			this.tag=tag;
			this.msg=msg;
		}
	}
	
}
