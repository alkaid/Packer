package com.alkaid.packer.biz;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

public class ApkPacker {
	/** Apk icon替换时将要拷贝的新的icon的临时文件的后缀 */
	private static final String APK_ICON_MODIFIED_EX = ".temp_modified.png";
	private static final String UZIP_PATH_PRE = "temp_";

	private static final String PATH_LIB = "lib"+File.separator;
	private static final String PATH_7Z = PATH_LIB + "7z";
	private static final String PATH_APKTOOL = PATH_LIB + "apktool.jar";
	private static final String PATH_KEYSTORE = PATH_LIB + "mykj.keystore";
	private static final String KEYSTORE_ALIAS = "mykj.keystore";
	private static final String KEYSTORE_PASSWORD = "yueguangbaohe";
	private File apk;
	/** 解压路径 */
	private File unzipDir;
	private File extractDir;
	private ApkInfo originApkInfo;
	private String apkName;
	private File originApkIcon;
	private File originChannelProp;

	private File tempChannelProp;
	private File tempAndroidManifest;
	private File tempIcon;
	private List<File> tempAssetsFiles=new ArrayList<File>();
	private ProcessBuilder mBuilder;

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

	public File getTempChannelProp() {
		return tempChannelProp;
	}

	public File getTempAndroidManifest() {
		return tempAndroidManifest;
	}

	public File getTempIcon() {
		return tempIcon;
	}

	public File getUnzipDir() {
		return unzipDir;
	}

	public List<File> getTempAssetsFiles() {
		return tempAssetsFiles;
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
		this.unzipDir = new File(Constants.PATH_WORKSPACE + UZIP_PATH_PRE
				+ apkName);
		this.tempAndroidManifest = new File(unzipDir.getAbsolutePath() + File.separator
				+ ApkInfo.Manifest.fileName);
		this.tempChannelProp = new File(unzipDir.getAbsolutePath() + File.separator
				+ ApkInfo.Extension.PROP_FILE_CHANNEL);
		this.tempIcon = new File(unzipDir.getAbsolutePath() + File.separator
				+ originApkInfo.getApplicationIcon());
		originApkIcon = new File(extractDir.getAbsolutePath() + File.separator
				+ originApkInfo.getApplicationIcon());
		originChannelProp = new File(extractDir.getAbsolutePath() + File.separator
				+ ApkInfo.Extension.PROP_FILE_CHANNEL);
	}

	public boolean loadApk() {
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

	private boolean loadManifest() {
		Log.d("正在加载" + ApkInfo.Manifest.fileName + "...");
		InputStream is = null;
		try {
			is = new FileInputStream(tempAndroidManifest);
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

	private boolean loadChannelProp() {
		Extension extend = originApkInfo.extension;
		InputStream in = null;
		Log.i("正在加载Apk明细项:" + ApkInfo.Extension.PROP_FILE_CHANNEL + "...");
		try {
			Properties pps = new Properties();
			in = new BufferedInputStream(new FileInputStream(tempChannelProp));
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

	private boolean unzip() {
		IOUtil.delFileDir(this.unzipDir);
		Log.i("正在解压apk...");
		if (!commandUnzip(tempChannelProp.getParentFile(), apk,
				ApkInfo.Extension.PROP_FILE_CHANNEL))
			return false;
		// if(!commad7z(tempIcon.getParentFile(),apk,originApkInfo.getApplicationIcon()))
		// return false;
		if (!commandUnzip(tempAndroidManifest.getParentFile(), apk,
				ApkInfo.Manifest.fileName))
			return false;
		Log.i("成功解压apk");
		return true;
	}

	private boolean commandZip(File apk, File src) {
		String command = String.format("%s a %s %s",PATH_7Z,apk.getAbsolutePath(), src.getAbsolutePath());
		Log.d("正在执行命令：" +command);
		boolean success = false;
		Process process = null;
		InputStream is = null;
		BufferedReader br = null;
		do {
			try {
				process = mBuilder.command(PATH_7Z,"a",apk.getAbsolutePath(), src.getAbsolutePath()).start();
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
				// 替换icon
				if (tempIcon.exists()) {
					publish(new LogInfo(Tag.debug, "正在替换新的icon..."));
					// 从路径drawble开始压缩
					// wokspace/temp_[apk_name]/drawable -->
					// wokspace/temp_[apk_name]/35000/[apk_name].apk
					File zipIconSrc = new File(unzipDir.getAbsolutePath()
							+ File.separator
							+ originApkInfo.getApplicationIcon().substring(
									0,
									originApkInfo.getApplicationIcon().indexOf(
											"/")));
					if (!commandZip(unsignApk, zipIconSrc)) {
						publish(new LogInfo(Tag.error, "icon压缩失败！"));
						return false;
					}
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
							+ unsignApk.getName()), zipPropSrc)) {
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
