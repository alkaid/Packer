package com.alkaid.packer.biz;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.alkaid.packer.common.Constants;
import com.alkaid.packer.model.ApkInfo;
import com.alkaid.packer.model.ApkInfo.Extension;
import com.alkaid.packer.model.ApkInfo.Manifest;
import com.alkaid.packer.util.IOUtil;
import com.alkaid.packer.util.Log;
import com.alkaid.packer.util.Log.LogInfo;
import com.alkaid.packer.util.Log.Tag;
import com.alkaid.packer.util.Properties;

import fr.xgouchet.axml.CompressedXmlParser;
/**
 * Apk解包打包类
 * @author lc
 *
 */
public class ApkPacker {
	/** Apk icon替换时将要拷贝的新的icon的临时文件的后缀 */
	private static final String APK_ICON_MODIFIED_EX = ".temp_modified.png";
	private static final String TEMP_PATH_PRE = "temp_";
	private static final String DATA_PATH_PRE = "data_";
	private static final String TEMPCHANNEL_PATH_PRE = "tempchannel_";

	private static final String PATH_LIB = "lib"+File.separator;
	private static final String PATH_7Z = PATH_LIB + "7z";
	private static final String PATH_APKTOOL = PATH_LIB + "apktool.jar";
	private static final String PATH_KEYSTORE = PATH_LIB + "mykj.keystore";
	private static final String KEYSTORE_ALIAS = "mykj.keystore";
	private static final String KEYSTORE_PASSWORD = "yueguangbaohe";
	private File apk;
	/** 解压路径 */
	private File tempDir;
	/** 文件解压 数据读取用的路径*/
	private File dataDir;
	/** 批量渠道属性文件临时存放目录*/
	private File tempChannelDir;
	private File extractDir;
	private ApkInfo originApkInfo;
	private String apkName;
	private File originApkIcon;
	private File originChannelProp;

	private File tempIcon;
	private File dataChannelProp;
	private File dataAndroidManifest;
	private ProcessBuilder mBuilder;
	
	private List<File> tempAddedFiles=new ArrayList<File>();
	private List<File> tempModifiedFiles=new ArrayList<File>();
	private List<String> tempDeletedFiles=new ArrayList<String>();

	public File getApk() {
		return apk;
	}

	public File getExtractDir() {
		return extractDir;
	}

	public ApkInfo getOriginApkInfo() {
		return originApkInfo;
	}

	public File getOriginApkIcon() {
		return originApkIcon;
	}

	public File getDataChannelProp() {
		return dataChannelProp;
	}

	public File getTempAndroidManifest() {
		return dataAndroidManifest;
	}

	public File getTempIcon() {
		return tempIcon;
	}

	public File getTempDir() {
		return tempDir;
	}

	public File getTempChannelDir() {
		return tempChannelDir;
	}

	public ApkPacker(File apk, ApkInfo originApkInfo) {
		mBuilder = new ProcessBuilder();
		mBuilder.redirectErrorStream(true);
		this.originApkInfo = originApkInfo;
		this.apk = apk;
		apkName = apk.getName().substring(0, apk.getName().lastIndexOf("."));
		// this.extractDir=new
		// File(apk.getParentFile().getAbsoluteFile()+File.separator+apkName);
		this.extractDir = new File(Constants.PATH_WORKSPACE + apkName);
		this.tempDir = new File(Constants.PATH_WORKSPACE + TEMP_PATH_PRE
				+ apkName);
		this.tempChannelDir = new File(Constants.PATH_WORKSPACE + TEMPCHANNEL_PATH_PRE
				+ apkName);
		this.dataDir = new File(Constants.PATH_WORKSPACE + DATA_PATH_PRE
				+ apkName);
		this.dataAndroidManifest = new File(dataDir.getAbsolutePath() + File.separator
				+ ApkInfo.Manifest.fileName);
		this.dataChannelProp = new File(dataDir.getAbsolutePath() + File.separator
				+ ApkInfo.Extension.PROP_FILE_CHANNEL);
		this.tempIcon = new File(tempDir.getAbsolutePath() + File.separator
				+ originApkInfo.getApplicationIcon());
		originApkIcon = new File(extractDir.getAbsolutePath() + File.separator
				+ originApkInfo.getApplicationIcon());
		originChannelProp = new File(extractDir.getAbsolutePath() + File.separator
				+ ApkInfo.Extension.PROP_FILE_CHANNEL);
	}

	/**
	 * 载入APK信息
	 * @return
	 */
	public boolean loadApk() {
		IOUtil.delFileDir(this.tempDir);
		IOUtil.delFileDir(this.dataDir);
		if (!unzip()) {
			return false;
		}
		if (!loadChannelProp()) {
			return false;
		}
		if (!loadManifest()) {
			return false;
		}
		return true;
	}
	/**
	 * 载入AndroidManifest.xml
	 * @return
	 */
	private boolean loadManifest() {
		Log.d("正在加载" + ApkInfo.Manifest.fileName + "...");
		InputStream is = null;
		try {
			is = new FileInputStream(dataAndroidManifest);
			Document doc = new CompressedXmlParser().parseDOM(is);
			// doc.normalize();
			Element root = doc.getDocumentElement();
			// Element applicationNode=(Element)
			// root.getElementsByTagName(Manifest.application.application).item(0);
			NodeList metadataNodes = root
					.getElementsByTagName(Manifest.application.meta_data.meta_data);
			originApkInfo.metaDatas.clear();
			for (int i = 0; i < metadataNodes.getLength(); i++) {
				Element e = (Element) metadataNodes.item(i);
				if (e.getParentNode().getNodeName()
						.equals(Manifest.application.application)) {
					String name = e
							.getAttribute(Manifest.application.meta_data.name);
					String value = e
							.getAttribute(Manifest.application.meta_data.value);
					originApkInfo.metaDatas.put(name, value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e(ApkInfo.Manifest.fileName + "加载失败...");
			return false;
		} finally {
			IOUtil.closeIO(is);
		}
		return true;
	}
	/**
	 * 载入渠道属性
	 * @return
	 */
	private boolean loadChannelProp() {
		Extension extend = originApkInfo.extension;
		InputStream in = null;
		Log.i("正在加载Apk明细项:" + ApkInfo.Extension.PROP_FILE_CHANNEL + "...");
		try {
			Properties pps = new Properties();
			in = new BufferedInputStream(new FileInputStream(dataChannelProp));
			pps.load(in);
			extend.channelId = pps.getProperty(Extension.PROP_KEY_CHANNEL_ID);
			extend.childChannelId = pps
					.getProperty(Extension.PROP_KEY_CHILD_CHANNEL_ID);
			extend.versionName = pps
					.getProperty(Extension.PROP_KEY_VERSION_NAME);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e(ApkInfo.Extension.PROP_FILE_CHANNEL + "加载失败...");
			return false;
		} finally {
			IOUtil.closeIO(in);
		}
		return true;
	}
	/**
	 * 添加文件
	 * @param file
	 * @param zipEntryPath
	 * @return
	 */
	public boolean addFile(File file,String zipEntryPath){
		File data=entryPath2DataPath(zipEntryPath);
		File temp=entryPath2TempPath(zipEntryPath);
		if(data.exists())
			IOUtil.delFileDir(data);
		if(temp.exists())
			IOUtil.delFileDir(temp);
		try {
			IOUtil.copyFiles(file, data);
			IOUtil.copyFiles(data, temp);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e("添加文件失败！");
			return false;
		}
		this.tempAddedFiles.add(temp);
		return true;
	}
	/**
	 * 删除文件
	 * @param zipEntryPath
	 * @return
	 */
	public void delFile(String zipEntryPath){
		File temp=entryPath2DataPath(zipEntryPath);
		IOUtil.delFileDir(temp);
		tempDeletedFiles.add(zipEntryPath);
	}
	/**
	 * 根据apk中的文件路径获得文件内容，若临时目录中存在，则加载，否则先解压至临时目录
	 * @param zipEntryPath
	 * @return
	 */
	public String getFileContent(String zipEntryPath){
		File data=entryPath2DataPath(zipEntryPath);
		if(!data.exists()){
			//解压
			if(!data.getParentFile().exists()){
				data.getParentFile().mkdirs();
			}
			boolean success = commandUnzip(data.getParentFile(), apk, zipEntryPath);
			if(!success) {
				Log.e("获得文件内容失败！");
				return null;
			}
		}
		FileInputStream is = null;
		BufferedReader br = null;
		StringBuilder sb=new StringBuilder();
		try {
			is = new FileInputStream(data);
			br = new BufferedReader(new InputStreamReader(is, "utf-8"),
					1024);
			String tmp = null;
			while ((tmp = br.readLine()) != null) {
				sb.append(tmp).append("\n");
			}
			if(sb.length()>0) sb.deleteCharAt(sb.length()-1);
		}catch(Exception e){
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e("获得文件内容失败！");
			return null;
		}finally{
			IOUtil.closeIO(is);
			IOUtil.closeIO(br);
		}
		return sb.toString();
	}
	/**
	 * 修改文件内容，这里是修改临时文件
	 * @param content
	 * @param zipEntryPath
	 * @return
	 */
	public boolean setFileContent(String content,String zipEntryPath){
		File data=entryPath2DataPath(zipEntryPath);
		File temp=entryPath2TempPath(zipEntryPath);
		if(!data.exists()){
			Log.e("写入文件内容失败：临时文件不存在！");
			return false;
		}
		FileOutputStream fos=null;
		Writer os=null;
		try {
			fos = new FileOutputStream(data);
			os = new OutputStreamWriter(fos, "utf-8");
			os.write(content);
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e("写入文件内容失败！");
			return false;
		} finally{
			IOUtil.closeIO(fos);
			IOUtil.closeIO(os);
		}
		if(temp.exists())
			IOUtil.delFileDir(temp);
		try {
			IOUtil.copy(data, temp);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e("写入文件内容失败！");
			return false;
		}
		tempModifiedFiles.add(temp);
		Log.i("修改文件内容成功!");
		return true;
	}
	/**
	 * apk中的路径转为临时工作路径
	 * @param entryPath
	 * @return
	 */
	private File entryPath2TempPath(String entryPath){
		File epf=new File(entryPath);
		File tpf=new File(tempDir.getAbsolutePath()+File.separator+epf.getPath());
		return tpf;
	}
	/**
	 * apk中的路径转为临时数据读取路径
	 * @param entryPath
	 * @return
	 */
	private File entryPath2DataPath(String entryPath){
		File epf=new File(entryPath);
		File tpf=new File(dataDir.getAbsolutePath()+File.separator+epf.getPath());
		return tpf;
	}
	/**
	 * 临时工作路径转为apk中的路径
	 * @param tempPath
	 * @return
	 */
	private String tempPath2EntryPath(File tempPath){
		String path=tempPath.getAbsolutePath().replace(tempDir.getAbsolutePath()+File.separator, "");
		path=path.replace("\\", "/");
		return path;
	}
	/**
	 * 临时数据读取路径转为apk中的路径
	 * @param dataPath
	 * @return
	 */
	private String dataPath2EntryPath(File dataPath){
		String path=dataPath.getAbsolutePath().replace(dataDir.getAbsolutePath()+File.separator, "");
		path=path.replace("\\", "/");
		return path;
	}
	private boolean unzip() {
		IOUtil.delFileDir(this.tempDir);
		Log.i("正在解压apk...");
		if (!commandUnzip(dataChannelProp.getParentFile(), apk,
				ApkInfo.Extension.PROP_FILE_CHANNEL))
			return false;
		/*ZipFile zFile;
		try {
			zFile = new ZipFile(apk);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			return false;
		} 
		Enumeration<? extends ZipEntry> zes = zFile.entries();
		List<String> assetsSrcPaths=new ArrayList<String>();
		while(zes.hasMoreElements()){
			ZipEntry ze=zes.nextElement();
			if(!ze.isDirectory()&&ze.getName().startsWith("assets/")){
				if(new File(ze.getName()).getParent().equals("assets")){
					assetsSrcPaths.add(ze.getName());//ze.getSize()<8k
					tempAssetsFiles.add(new File(unzipDir.getAbsolutePath()+File.separator+ ze.getName()));
				}
			}
		}*/
		if (!commandUnzip(dataAndroidManifest.getParentFile(), apk,
				ApkInfo.Manifest.fileName))
			return false;
//		if (!commandUnzip(this.unzipDir, apk,
//				"assets"))
//			return false;
		Log.i("成功解压apk");
		return true;
	}

	/**
	 * 命令行添加文件到zip
	 * @param apk
	 * @param src
	 * @return
	 */
	private boolean commandZip(File apk, File[] src) {
		if(src.length<=0)
			return true;
		List<String> params=new ArrayList<String>();
		params.add(PATH_7Z);
		params.add("a");
		params.add(apk.getAbsolutePath());
		String srcPath="";
		for (File f : src) {
			srcPath+=f.getAbsolutePath()+" ";
			params.add(f.getAbsolutePath());
		}
		String command = String.format("%s a %s %s",PATH_7Z,apk.getAbsolutePath(), srcPath);
		Log.d("正在执行命令：" +command);
		boolean success = false;
		Process process = null;
		InputStream is = null;
		BufferedReader br = null;
		do {
			try {
				process = mBuilder.command(params).start();
				// process = Runtime.getRuntime().exec(command);
//				success = process.waitFor(2000L, TimeUnit.MILLISECONDS);
				// TODO 还不知道如何检查结果 换commandbuilder试试
				is = process.getInputStream();
				br = new BufferedReader(new InputStreamReader(is,"gbk"));
				String tmp = br.readLine();
				if (tmp == null) {
					success = false;
					break;
				} else {
					do {
						if(!tmp.isEmpty())
							Log.log(new LogInfo(Tag.debug, tmp));
						success = tmp.contains("Everything is Ok") ? true
								: success;
					} while ((tmp = br.readLine()) != null);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.log(new LogInfo(Tag.error, "发生异常：" + e.getMessage()));
				success = false;
			} finally {
				process.destroy();
				closeIO(is);
				closeIO(br);
			}
		} while (false);
		if (!success) {
			Log.e("命令执行失败,请查看以上日志");
		}
		return success;
	}
	/**
	 * 命令行解压zip
	 * @param outDir 输出路径
	 * @param apk	apk路径
	 * @param srcPath	apk中待解压文件的路径
	 * @return
	 */
	private boolean commandUnzip(File outDir, File apk, String srcPath) {
		String command = String.format("%s e -o%s %s %s", PATH_7Z,
				outDir.getAbsolutePath(), apk.getAbsolutePath(), srcPath);
		Log.d("正在执行命令：" + command);
		boolean success = false;
		Process process = null;
		InputStream is = null;
		BufferedReader br = null;
		do {
			try {
				process = mBuilder.command(PATH_7Z,"e","-o"+outDir.getAbsolutePath(), apk.getAbsolutePath(), srcPath).start();
				is = process.getInputStream();
				br = new BufferedReader(new InputStreamReader(is,"gbk"));
				String tmp = br.readLine();
				if (tmp == null) {
					success = false;
					break;
				} else {
					do {
						if(!tmp.isEmpty())
							Log.log(new LogInfo(Tag.debug, tmp));
						success = tmp.contains("Everything is Ok") ? true
								: success;
					} while ((tmp = br.readLine()) != null);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.log(new LogInfo(Tag.error, "发生异常：" + e.getMessage()));
				success = false;
			} finally {
				process.destroy();
				closeIO(is);
				closeIO(br);
			}
		} while (false);
		
		if (!success) {
			Log.e("命令执行失败,请查看以上日志");
		}
		return success;
	}
	/**
	 * 命令行从zip中删除文件
	 * @param apk
	 * @param src
	 * @return
	 */
	private boolean commandDelZip(File apk, List<String> src) {
		if(src.isEmpty())
			return true;
		List<String> params=new ArrayList<String>();
		params.add(PATH_7Z);
		params.add("d");
		params.add(apk.getAbsolutePath());
		String srcPath="";
		for(String p:src){
			srcPath+=(p+" ");
			params.add(p);
		}
		String command = String.format("%s d %s %s",PATH_7Z,apk.getAbsolutePath(), srcPath);
		Log.d("正在执行命令：" +command);
		boolean success = false;
		Process process = null;
		InputStream is = null;
		BufferedReader br = null;
		do {
			try {
				process = mBuilder.command(params).start();
				// process = Runtime.getRuntime().exec(command);
//				success = process.waitFor(2000L, TimeUnit.MILLISECONDS);
				// TODO 还不知道如何检查结果 换commandbuilder试试
				is = process.getInputStream();
				br = new BufferedReader(new InputStreamReader(is,"gbk"));
				String tmp = br.readLine();
				if (tmp == null) {
					success = false;
					break;
				} else {
					do {
						if(!tmp.isEmpty())
							Log.log(new LogInfo(Tag.debug, tmp));
						success = tmp.contains("Everything is Ok") ? true
								: success;
					} while ((tmp = br.readLine()) != null);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.log(new LogInfo(Tag.error, "发生异常：" + e.getMessage()));
				success = false;
			} finally {
				process.destroy();
				closeIO(is);
				closeIO(br);
			}
		} while (false);
		if (!success) {
			Log.e("命令执行失败,请查看以上日志");
		}
		return success;
	}

	public void extractAndDecode(final Callback callback) {
		SwingWorker<Boolean, LogInfo> work = new SwingWorker<Boolean, LogInfo>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				boolean deleted = true;
				if (extractDir.exists()) {
//					boolean delete = callback
//							.onConfirmDeleteAlreadyExistFile(extractDir);
//					if (delete) {
						publish(new LogInfo(Tag.info, "正在删除已存在目录:"));
						publish(new LogInfo(Tag.info,
								extractDir.getAbsolutePath()));
						deleted = IOUtil.delFileDir(extractDir);
//					}
				}
				if (!deleted) {
					publish(new LogInfo(Tag.error,
							"无法删除已存在目录，可能有其他进程正在占用该目录或其下文件。"));
					return null;
				}

				callback.onPrepare();
				publish(new LogInfo(Tag.info, "正在解压并反编apk..."));
				String command = String.format("java -jar \"%s\" d %s -o %s",
						PATH_APKTOOL, apk.getAbsolutePath(),
						extractDir.getAbsolutePath());
				publish(new LogInfo(Tag.debug, "正在执行命令："));
				publish(new LogInfo(Tag.debug, command));
				Process process = Runtime.getRuntime().exec(command);
				/*
				 * Process process = mBuilder.command(PATH_APKTOOL, "d",
				 * apk.getAbsolutePath(), extractDir.getAbsolutePath())
				 * .start();
				 */
				InputStream is = null;
				is = process.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						is, "gbk"));
				String tmp = br.readLine();
				boolean success = false;
				try {
					if (tmp == null) {
						success = false;
					} else {
						if (!tmp.startsWith("I: Using Apktool")) {
							success = false;
							throw new Exception("无法正常解析APK包。输出结果为:\n" + tmp
									+ "...");
						} else {
							do {
								publish(new LogInfo(Tag.debug, tmp));
								success = tmp
										.contains("I: Copying original files...") ? true
										: success;
								success = tmp.contains("error") ? false
										: success;
							} while ((tmp = br.readLine()) != null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					publish(new LogInfo(Tag.error, "发生异常：" + e.getMessage()));
					success = false;
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
				Boolean success = false;
				try {
					if (get() == null) {
						success = null;
						Log.i("已取消解压APK");
					} else if (get()) {
						success = true;
						Log.i("apk解压成功!解压路径：");
						Log.i(extractDir.getAbsolutePath());
					} else {
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

	public void pack(boolean isEncode, List<File> tempChannelDir,
			final Callback callback) {
		pack(isEncode, null, tempChannelDir, callback);
	}

	public void pack(final boolean isEncode, final String apkPath,
			final List<File> tempChannelDir, final Callback callback) {
		SwingWorker<Boolean, LogInfo> work = new SwingWorker<Boolean, LogInfo>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				callback.onPrepare();
				boolean success = false;
				// wokspace/[apk_name]/[apk_name]_unsinged.apk
				File unsignApk = new File(extractDir.getParent() + File.separator
						+ apkName + "_unsinged.apk");
				Process process = null;
				InputStream is = null;
				BufferedReader br = null;
				String command = null;
				if (isEncode) {
					// 需要编码 则用apktool编译打包
					publish(new LogInfo(Tag.info, "正在生成未签名的apk..."));
					// 打包 - 生成未签名的包
					command = String.format("java -jar \"%s\" b %s -o %s",
							PATH_APKTOOL, extractDir,
							unsignApk.getAbsolutePath());
					publish(new LogInfo(Tag.debug, "正在执行命令："));
					publish(new LogInfo(Tag.debug, command));
					process = Runtime.getRuntime().exec(command);
					/*
					 * Process process = mBuilder.command(PATH_APKTOOL, "d",
					 * apk.getAbsolutePath(), extractDir.getAbsolutePath())
					 * .start();
					 */
					is = process.getInputStream();
					br = new BufferedReader(new InputStreamReader(is, "gbk"));
					String tmp = br.readLine();
					try {
						if (tmp == null) {
							success = false;
						} else {
							if (!tmp.startsWith("I: Using Apktool")) {
								success = false;
								throw new Exception("无法生成APK包。输出结果为:\n" + tmp
										+ "...");
							} else {
								do {
									publish(new LogInfo(Tag.debug, tmp));
									success = tmp
											.contains("I: Building apk file...") ? true
											: success;
									success = tmp.contains("error") ? false
											: success;
								} while ((tmp = br.readLine()) != null);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						publish(new LogInfo(Tag.error, "发生异常：" + e.getMessage()));
						return false;
					} finally {
						process.destroy();
						closeIO(is);
						closeIO(br);
					}
					if (!success)
						return success;
					publish(new LogInfo(Tag.info, "成功生成未签名的APK包!"));
				} else {
					try {
						IOUtil.copy(apk, unsignApk);
					} catch (Exception e) {
						e.printStackTrace();
						publish(new LogInfo(Tag.error, "复制原apk失败："
								+ e.getMessage()));
						return false;
					}
				}

				publish(new LogInfo(Tag.info, "正在替换未签名apk中的文件..."));
//				// 替换icon文件
//				if (tempIcon.exists()) {
//					publish(new LogInfo(Tag.debug, "正在替换新的icon..."));
//					// 从路径drawble开始压缩
//					// wokspace/temp_[apk_name]/drawable -->
//					// wokspace/temp_[apk_name]/35000/[apk_name].apk
//					File zipIconSrc = new File(tempDir.getAbsolutePath()
//							+ File.separator
//							+ originApkInfo.getApplicationIcon().substring(
//									0,
//									originApkInfo.getApplicationIcon().indexOf(
//											"/")));
//					if (!commandZip(unsignApk, zipIconSrc)) {
//						publish(new LogInfo(Tag.error, "icon压缩失败！"));
//						return false;
//					}
//				}
				//先删除zip中要删除的文件  再添加或替换文件 顺序不能变
				//删除文件
				if(!commandDelZip(unsignApk, tempDeletedFiles)){
					publish(new LogInfo(Tag.error, "删除apk中的文件失败！"));
					return false;
				}
				//添加或替换文件
				if (!commandZip(unsignApk, tempDir.listFiles())) {
					publish(new LogInfo(Tag.error, "添加或替换压缩文件失败！"));
					return false;
				}
				// 拷贝apk到各个渠道目录
				// wokspace/temp_[apk_name]/35000
				publish(new LogInfo(Tag.debug, "正在拷贝未签名apk至各个渠道目录下..."));
				for (File f : tempChannelDir) {
					try {
						IOUtil.copy(unsignApk, new File(f.getAbsolutePath()
								+ File.separator + unsignApk.getName()));
					} catch (Exception e) {
						e.printStackTrace();
						publish(new LogInfo(Tag.error, "复制未签名apk失败："
								+ e.getMessage()));
						return false;
					}
				}
				publish(new LogInfo(Tag.debug, "正在替换新的ChannelProperties..."));
				// wokspace/temp_[apk_name]/35000/assets -->
				// wokspace/temp_[apk_name]/35000/[apk_name].apk
				// 批量替换ChannelPropeties
				for (File f : tempChannelDir) {
					// 从路径assets开始压缩
					File zipPropSrc = new File(f.getAbsolutePath()
							+ File.separator
							+ ApkInfo.Extension.PROP_FILE_CHANNEL.substring(0,
									ApkInfo.Extension.PROP_FILE_CHANNEL
											.indexOf(File.separator)));
					if (!commandZip(new File(f.getAbsolutePath() + File.separator
							+ unsignApk.getName()), new File[]{zipPropSrc})) {
						publish(new LogInfo(Tag.error,
								ApkInfo.Extension.PROP_FILE_CHANNEL + "压缩失败！"));
						return false;
					}
				}
				// 签名
				for (File f : tempChannelDir) {
					String channelUnsignApk = f.getAbsolutePath() + File.separator
							+ unsignApk.getName();
					File outputChannel = new File(Constants.PATH_OUTPUT_DEFAULT
							+ f.getName());
					if (apkPath != null) {
						outputChannel = new File(new File(apkPath).getParent()
								+ File.separator + Constants.PATH_OUTPUT_NAME
								+ f.getName());
					}
					if (!outputChannel.exists()) {
						outputChannel.mkdirs();
					}
					String signedApk = outputChannel.getAbsolutePath() + File.separator
							+ apkName + "_singed.apk";
					command = String
							.format("jarsigner -storepass %s -digestalg SHA1 -sigalg SHA1withRSA -verbose -keystore \"%s\" -signedjar %s %s %s",
									KEYSTORE_PASSWORD, PATH_KEYSTORE,
									signedApk, channelUnsignApk, KEYSTORE_ALIAS);
					publish(new LogInfo(Tag.info, "开始签名..."));
					publish(new LogInfo(Tag.debug, "正在执行命令：" + command));
					process = Runtime.getRuntime().exec(command);
					is = process.getInputStream();
					br = new BufferedReader(new InputStreamReader(is, "gbk"));
					String tmp = br.readLine();
					success = false;
					try {
						if (tmp == null) {
							success = false;
						} else {
							if (!(tmp.trim().startsWith("正在添加： META-INF")||tmp.trim().startsWith("正在更新： META-INF"))) {
								success = false;
								throw new Exception("签名失败。输出结果为:\n" + tmp
										+ "...");
							} else {
								success = true;
								do {
									publish(new LogInfo(Tag.verbose, tmp));
									// success=tmp.contains("I: Copying unknown files/dir...")?true:success;
									// success=tmp.contains("error")?false:success;
									// //TODO 还不知道如何判断失败
								} while ((tmp = br.readLine()) != null);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						publish(new LogInfo(Tag.error, "发生异常：" + e.getMessage()));
						return false;
					} finally {
						process.destroy();
						closeIO(is);
						closeIO(br);
					}
					if (success) {
//						IOUtil.delFileDir(channelUnsignApk);
						publish(new LogInfo(Tag.info, "签名成功！"));
						publish(new LogInfo(Tag.info, "APK导出成功！路径："));
						publish(new LogInfo(Tag.info, signedApk));
					}
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
				boolean success = false;
				try {
					if (get()) {
						success = true;
						Log.i("APK批量导出成功");
					} else {
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

	public void createModifiedTempIcon(File source) throws IOException {
		if (tempIcon.exists()) {
			IOUtil.delFileDir(tempIcon);
		}
		IOUtil.copy(source, tempIcon);
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

	/*
	 * public static interface ConsoleOutputListener{ public void
	 * onConsoleOutput(Log.Tag tag,String msg); }
	 */
	public static interface Callback {
		public void onPrepare();

		public void onDone(Boolean success);
	}
}
