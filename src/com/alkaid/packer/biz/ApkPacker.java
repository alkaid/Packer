package com.alkaid.packer.biz;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import com.alkaid.packer.model.ApkInfo;
import com.alkaid.packer.util.IOUtil;
import com.alkaid.packer.util.Log;
import com.alkaid.packer.util.Log.LogInfo;
import com.alkaid.packer.util.Log.Tag;

public class ApkPacker {
	/** Apk icon替换时将要拷贝的新的icon的临时文件的后缀*/
	private static final String APK_ICON_MODIFIED_EX=".temp_modified.png";
	
	private static final String PATH_LIB="lib/";
	private static final String PATH_APKTOOL=PATH_LIB+"apktool.jar";
	private static final String PATH_KEYSTORE=PATH_LIB+"mykj.keystore";
	private static final String KEYSTORE_ALIAS="mykj.keystore";
	private static final String KEYSTORE_PASSWORD="yueguangbaohe";
	private File apk;
	/** 解压路径*/
	private File extractDir;
	private ApkInfo originApkInfo;
	private String apkName;
	private File orginApkIcon;
	/** Apk icon替换时将要拷贝的新的icon的临时文件*/
	private File modifiedTempApkIcon;
	public File getApk() {
		return apk;
	}
	public File getExtractDir() {
		return extractDir;
	}
	public ApkInfo getOriginApkInfo() {
		return originApkInfo;
	}
	public File getOrginApkIcon() {
		return orginApkIcon;
	}
	public File getModifiedTempApkIcon() {
		return modifiedTempApkIcon;
	}

	private ProcessBuilder mBuilder;
	
	public ApkPacker(File apk,ApkInfo originApkInfo) {
		mBuilder = new ProcessBuilder();
		mBuilder.redirectErrorStream(true);
		this.originApkInfo=originApkInfo;
		this.apk=apk;
		apkName=apk.getName().substring(0,apk.getName().lastIndexOf("."));
		this.extractDir=new File(apk.getParentFile().getAbsoluteFile()+"/"+apkName);
		orginApkIcon=new File(extractDir.getAbsolutePath()+"/"+originApkInfo.getApplicationIcon());
		modifiedTempApkIcon=new File(orginApkIcon.getAbsolutePath()+APK_ICON_MODIFIED_EX);
	}
	
	public void extract(final Callback callback){
		SwingWorker<Boolean, LogInfo> work=new SwingWorker<Boolean, LogInfo>(){
			@Override
			protected Boolean doInBackground() throws Exception {
				boolean deleted=true;
				if(extractDir.exists()){
					boolean delete=callback.onConfirmDeleteAlreadyExistFile(extractDir);
					if(delete){
						publish(new LogInfo(Tag.info,"正在删除已存在目录:"));
						publish(new LogInfo(Tag.info,extractDir.getAbsolutePath()));
						deleted=IOUtil.delFileDir(extractDir);
					}else{
						//删除临时icon
						if(modifiedTempApkIcon.exists()){
							publish(new LogInfo(Tag.debug,"正在删除临时icon..."));
							if(!IOUtil.delFileDir(modifiedTempApkIcon)){
								publish(new LogInfo(Tag.error,"临时icon删除失败："));
								publish(new LogInfo(Tag.error,modifiedTempApkIcon.getAbsolutePath()));
							}
						}
						return null;
					}
				}
				if(!deleted){
					publish(new LogInfo(Tag.error,"无法删除已存在目录，可能有其他进程正在占用该目录或其下文件。"));
					return null;
				}
				 
				callback.onPrepare();
				publish(new LogInfo(Tag.info,"正在解压apk..."));
				String command =String.format("java -jar \"%s\" d %s -o %s", PATH_APKTOOL,apk.getAbsolutePath(),extractDir.getAbsolutePath());
				publish(new LogInfo(Tag.debug,"正在执行命令："));
				publish(new LogInfo(Tag.debug,command));
				Process process = Runtime.getRuntime().exec(command);  
				/*Process process = mBuilder.command(PATH_APKTOOL, "d", 
						apk.getAbsolutePath(), 
						extractDir.getAbsolutePath())
						.start();*/
				InputStream is = null;
				is = process.getInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is,"gbk"));
				String tmp = br.readLine();
				boolean success=false;
				try {
					if(tmp==null){
						success=false;
					}else{
						if (!tmp.startsWith("I: Using Apktool")) {
							success=false;
							throw new Exception("无法正常解析APK包。输出结果为:\n" + tmp + "...");
						}else{
							do {
								publish(new LogInfo(Tag.debug,tmp));
								success=tmp.contains("I: Copying original files...")?true:success;
								success=tmp.contains("error")?false:success;
							} while ((tmp = br.readLine()) != null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					publish(new LogInfo(Tag.error,"发生异常："+e.getMessage()));
					success=false;
				} finally {
					process.destroy();
					closeIO(is);
					closeIO(br);
				}
				return success;
			}
			@Override
			protected void process(List<LogInfo> list) {
				for (LogInfo logInfo : list) {
					Log.log(logInfo);
				}
			}
			@Override
			protected void done() {
				Boolean success=false;
				try {
					if(get()==null){
						success=null;
						Log.i("已取消解压APK");
					}else if(get()){
						success=true;
						Log.i("apk解压成功!解压路径：");
						Log.i(extractDir.getAbsolutePath());
					}else{
						Log.e("apk解压失败，详细请查看以上日志信息");
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
					Log.e("apk解压失败，详细请查看以上日志信息");
				}
				callback.onDone(success);
				super.done();
			}
		};
		work.execute();
		
	}

	public void pack(final Callback callback){
		pack(null, callback);
	}
	
	public void pack(final String apkPath,final Callback callback){
        SwingWorker<Boolean, LogInfo> work=new SwingWorker<Boolean, LogInfo>(){
        	@Override
			protected Boolean doInBackground() throws Exception {
        		callback.onPrepare();
        		publish(new LogInfo(Tag.info,"正在生成未签名的apk..."));
        		//替换icon
        		if(modifiedTempApkIcon.exists()){
        			publish(new LogInfo(Tag.info,"正在替换新的icon..."));
        			File orginIconBackup=new File(orginApkIcon.getAbsolutePath()+".backup");
        			boolean success=orginApkIcon.renameTo(orginIconBackup);
        			if(success){
        				success=modifiedTempApkIcon.renameTo(orginApkIcon);
        				if(success){
        					IOUtil.delFileDir(orginIconBackup);
        				}else{
        					orginIconBackup.renameTo(orginApkIcon);
        					publish(new LogInfo(Tag.error,"替换icon失败！原因：新icon重命名失败"));
        					return false;
        				}
        			}else{
        				publish(new LogInfo(Tag.error,"替换icon失败！原因：备份旧icon(重命名)失败"));
        				return false;
        			}
        		}
        		// 打包 - 生成未签名的包  
        		String unsignApk = extractDir.getParent()+"/"+ apkName+"_unsinged.apk";  
        		String signApk=apkPath!=null?apkPath:extractDir.getParent()+"/"+ apkName+"_singed.apk";
        		String command = String.format("java -jar \"%s\" b %s -o %s",PATH_APKTOOL, extractDir, unsignApk);  
        		publish(new LogInfo(Tag.debug,"正在执行命令："));
        		publish(new LogInfo(Tag.debug,command));
				Process process = Runtime.getRuntime().exec(command);  
				/*Process process = mBuilder.command(PATH_APKTOOL, "d", 
						apk.getAbsolutePath(), 
						extractDir.getAbsolutePath())
						.start();*/
				InputStream is = null;
				is = process.getInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is,"gbk"));
				String tmp = br.readLine();
				boolean success=false;
				try {
					if(tmp==null){
						success=false;
					}else{
						if (!tmp.startsWith("I: Using Apktool")) {
							success=false;
							throw new Exception("无法生成APK包。输出结果为:\n" + tmp + "...");
						}else{
							do {
								publish(new LogInfo(Tag.debug,tmp));
								success=tmp.contains("I: Building apk file...")?true:success;
								success=tmp.contains("error")?false:success;
							} while ((tmp = br.readLine()) != null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					publish(new LogInfo(Tag.error,"发生异常："+e.getMessage()));
					return false;
				} finally {
					process.destroy();
					closeIO(is);
					closeIO(br);
				}
				if(!success) return success;
				publish(new LogInfo(Tag.info,"成功生成未签名的APK包!"));
				//签名
				command=String.format("jarsigner -storepass %s -digestalg SHA1 -sigalg SHA1withRSA -verbose -keystore \"%s\" -signedjar %s %s %s", 
						KEYSTORE_PASSWORD,PATH_KEYSTORE, signApk, unsignApk, KEYSTORE_ALIAS);
				publish(new LogInfo(Tag.info,"开始签名..."));
				publish(new LogInfo(Tag.debug,"正在执行命令："+command));
				process = Runtime.getRuntime().exec(command);
				is = process.getInputStream();
				br = new BufferedReader(
						new InputStreamReader(is,"gbk"));
				tmp = br.readLine();
				success=false;
				try {
					if(tmp==null){
						success=false;
					}else{
						if (!tmp.trim().startsWith("正在添加： META-INF")) {
							success=false;
							throw new Exception("签名失败。输出结果为:\n" + tmp + "...");
						}else{
							success=true;
							do {
								publish(new LogInfo(Tag.debug,tmp));
//								success=tmp.contains("I: Copying unknown files/dir...")?true:success;
//								success=tmp.contains("error")?false:success;	//TODO 还不知道如何判断失败
							} while ((tmp = br.readLine()) != null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					publish(new LogInfo(Tag.error,"发生异常："+e.getMessage()));
					return false;
				} finally {
					process.destroy();
					closeIO(is);
					closeIO(br);
				}
				if(success){
					publish(new LogInfo(Tag.info, "签名成功！"));
					publish(new LogInfo(Tag.info, "APK导出成功！路径："));
					publish(new LogInfo(Tag.info, signApk));
				}
				IOUtil.delFileDir(unsignApk);
				return success;
			}
			@Override
			protected void process(List<LogInfo> list) {
				for (LogInfo logInfo : list) {
					Log.log(logInfo);
				}
			}
			@Override
			protected void done() {
				boolean success=false;
				try {
					if(get()){
						success=true;
					}else{
						Log.e("Apk导出失败，详细请查看以上日志信息");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
					Log.e("Apk导出失败，详细请查看以上日志信息");
				}
				callback.onDone(success);
				super.done();
			}
		};
		work.execute();
        
	}
	
	public void createModifiedTempIcon(File source) throws IOException{
		IOUtil.copy(source, modifiedTempApkIcon);
	}
	
	/**
	 * 释放资源。
	 * 
	 * @param c
	 *            将关闭的资源
	 */
	private final void closeIO(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*public static interface ConsoleOutputListener{
		public void onConsoleOutput(Log.Tag tag,String msg);
	}*/
	public static interface Callback{
		public void onPrepare();
		public void onDone(Boolean success);
		public boolean onConfirmDeleteAlreadyExistFile(File extractDir);
	}
}
