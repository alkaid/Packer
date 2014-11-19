/**
 * 
 */
package com.alkaid.packer.biz;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.alkaid.packer.model.ApkInfo;
import com.alkaid.packer.model.ApkInfo.Extension;
import com.alkaid.packer.model.ApkInfo.Manifest;
import com.alkaid.packer.util.IOUtil;
import com.alkaid.packer.util.Log;
import com.alkaid.packer.util.Log.LogInfo;
import com.alkaid.packer.util.Log.OnLogListener;
import com.alkaid.packer.util.Log.Tag;
import com.alkaid.packer.util.Properties;
import com.alkaid.packer.util.SafeProperties;

/**
 * @author alkaid
 *
 */
public class Refactor {
	private ApkInfo origin;
	private ApkInfo modified;
	/** 解压路径 */
	private File extractDir;
	private OnLogListener onLogListener;

	public Refactor(File extractDir, ApkInfo origin, ApkInfo modified,
			OnLogListener onLogListener) {
		this.extractDir = extractDir;
		this.origin = origin;
		this.modified = modified;
		this.onLogListener = onLogListener;
	}
	
	public static void loadMetadatas(File extractDir, ApkInfo apkInfo) throws ParserConfigurationException, SAXException, IOException{
		File f1 = new File(extractDir.getAbsolutePath() + "/"
				+ "AndroidManifest.xml");
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse(f1);
		document.normalize();
		Element root = document.getDocumentElement();
//		Element applicationNode=(Element) root.getElementsByTagName(Manifest.application.application).item(0);
		NodeList metadataNodes = root.getElementsByTagName(Manifest.application.meta_data.meta_data);
		for (int i = 0; i < metadataNodes.getLength(); i++) {
			Element e = (Element) metadataNodes.item(i);
			if(e.getParentNode().getNodeName().equals(Manifest.application.application)){
				String name=e.getAttribute(Manifest.application.meta_data.name);
				String value=e.getAttribute(Manifest.application.meta_data.value);
				apkInfo.metaDatas.put(name,value);
			}
		}
	}

	public boolean reManifest() {
		boolean success = false;
		onLogListener
				.onLog(new LogInfo(Tag.info, "正在载入AndroidManifext.xml..."));
		File f1 = new File(extractDir.getAbsolutePath() + "/"
				+ "AndroidManifest.xml");
		Document document = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(f1);
			document.normalize();
			Element root = document.getDocumentElement();
			onLogListener.onLog(new LogInfo(Tag.debug, "正在修改versionCode"));
			if (!modified.getVersionCode().equals(origin.getVersionCode())) {
				root.setAttribute(Manifest.manifest.versionCode,
						modified.getVersionCode());
			}
			onLogListener.onLog(new LogInfo(Tag.debug, "正在修改versionName"));
			if (!modified.getVersionName().equals(origin.getVersionName())) {
				root.setAttribute(Manifest.manifest.versionName,
						modified.getVersionName());
			}
			onLogListener.onLog(new LogInfo(Tag.debug, "正在修改packageName"));
			if (!modified.getPackageName().equals(origin.getPackageName())) {
				// 修改包名
				root.setAttribute(Manifest.manifest.package_,
						modified.getPackageName());
				// 遍历所有Activity修改android:name为完整名称
				NodeList list = root
						.getElementsByTagName(Manifest.application.activity.activity);
				for (int i = 0; i < list.getLength(); i++) {
					Element e = (Element) list.item(i);
					String actName = e
							.getAttribute(Manifest.application.activity.name);
					if (actName.startsWith(".")) {
						e.setAttribute(Manifest.application.activity.name,
								origin.getPackageName() + actName);
					}
				}
			}
			onLogListener.onLog(new LogInfo(Tag.debug, "正在修改meta-data"));
			for (String key : modified.metaDatas.keySet()) {
				String value=modified.metaDatas.get(key);
				if(!value.equals(origin.metaDatas.get(key))){
					NodeList metadataNodes = root.getElementsByTagName(Manifest.application.meta_data.meta_data);
					for (int i = 0; i < metadataNodes.getLength(); i++) {
						Element e = (Element) metadataNodes.item(i);
						if(e.getParentNode().getNodeName().equals(Manifest.application.application)){
							String name=e.getAttribute(Manifest.application.meta_data.name);
							if(name.equals(key)){
								e.setAttribute(Manifest.application.meta_data.value, value);
							}
						}
					}
				}
			}
			onLogListener.onLog(new LogInfo(Tag.debug, "正在保存修改....."));
			/** 将document中的内容写入文件中 */
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			/** 编码 */
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(f1);
			transformer.transform(source, result);
			success=true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
		} catch (IOException e) {
			e.printStackTrace();
			onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
		} catch (SAXException e) {
			e.printStackTrace();
			onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
		} catch (TransformerException e) {
			e.printStackTrace();
			onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
		} finally {
		}
		if (success)
			onLogListener.onLog(new LogInfo(Tag.info, "Manifest修改成功!"));
		return success;
	}

	public boolean renamePackage() {
		boolean success = true;
		onLogListener.onLog(new LogInfo(Tag.info, "正在重构包名..."));
		String originPackName = origin.getPackageName().replace(".", "/");
		String newPackName = modified.getPackageName().replace(".", "/");
		File packDir = new File(extractDir.getAbsolutePath() + "/smali/"
				+ originPackName);
		File newPackDir = new File(extractDir.getAbsolutePath() + "/smali/"
				+ newPackName);
		if (!newPackDir.exists()) {
			newPackDir.mkdirs();
		}
		File[] files=packDir.listFiles(new GenFilter());
		if(null!=packDir&&null!=files){
		for (File f : packDir.listFiles(new GenFilter())) {
			onLogListener.onLog(new LogInfo(Tag.debug, "正在处理文件"
					+ originPackName + "/" + f.getName()));
			File f2 = new File(newPackDir.getAbsolutePath() + "/" + f.getName());
			FileInputStream is = null;
			BufferedReader br = null;
			FileOutputStream os = null;
			BufferedWriter bw = null;
			try {
				f2.createNewFile();
				is = new FileInputStream(f);
				br = new BufferedReader(new InputStreamReader(is, "utf-8"),
						1024);
				os = new FileOutputStream(f2);
				bw = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
				String tmp = null;
				String tmp2 = null;
				while ((tmp = br.readLine()) != null) {
					tmp2 = tmp.replace(originPackName, newPackName);
					bw.write(tmp2);
					bw.newLine();
					bw.flush();
				}
				IOUtil.closeIO(is);
				IOUtil.closeIO(br);
				IOUtil.closeIO(os);
				IOUtil.closeIO(bw);
				f.delete();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				success = false;
				onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
				break;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				success = false;
				onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
				break;
			} catch (IOException e) {
				e.printStackTrace();
				success = false;
				onLogListener.onLog(new LogInfo(Tag.error, e.getMessage()));
				break;
			} finally {
				IOUtil.closeIO(is);
				IOUtil.closeIO(br);
				IOUtil.closeIO(os);
				IOUtil.closeIO(bw);
			}
		}
		}
		if (success){
			success=reManifest();
		}
		if(success){
			success=this.writeApkExtension();
		}
		if(success){
			onLogListener.onLog(new LogInfo(Tag.info, "包名重构成功!"));
		}else{
			onLogListener.onLog(new LogInfo(Tag.error, "包名重构失败!详细请查看上述日志..."));
		}
		return success;
	}

	private static class GenFilter implements FileFilter {
		public boolean accept(File f) {
			if (!f.isFile())
				return false;
			String name = f.getName();
			String regEx = "^BuildConfig\\.smali$";
			Matcher mat = Pattern.compile(regEx).matcher(name);
			if (mat.find())
				return true;

			regEx = "^Manifest\\.smali$";
			mat = Pattern.compile(regEx).matcher(name);
			if (mat.find())
				return true;
			regEx = "^Manifest\\$\\w+\\.smali$";
			mat = Pattern.compile(regEx).matcher(name);
			if (mat.find())
				return true;

			regEx = "^R\\.smali$";
			mat = Pattern.compile(regEx).matcher(name);
			if (mat.find())
				return true;
			regEx = "^R\\$\\w+\\.smali$";
			mat = Pattern.compile(regEx).matcher(name);
			if (mat.find())
				return true;
			return false;
		}
	}
	
	public static Extension readApkExtension(File extraDir) {
		Extension extend = null;
		InputStream in=null;
		Log.i("正在加载Apk明细项:"+ApkInfo.Extension.PROP_FILE_CHANNEL+"...");
		try {
			extend = new Extension();
			Properties pps = new Properties();
			in = new BufferedInputStream(new FileInputStream(extraDir.getAbsolutePath()+"/"+Extension.PROP_FILE_CHANNEL));
			pps.load(in);
			extend.channelId = pps.getProperty(Extension.PROP_KEY_CHANNEL_ID);
			extend.childChannelId=pps.getProperty(Extension.PROP_KEY_CHILD_CHANNEL_ID);
			extend.versionName=pps.getProperty(Extension.PROP_KEY_VERSION_NAME);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		}finally{
			IOUtil.closeIO(in);
		}
		return extend;
	}
	
	public boolean writeApkExtension(){
		boolean success = false;
		InputStream in=null;
		OutputStream out=null;
		Log.i("正在写入"+ApkInfo.Extension.PROP_FILE_CHANNEL+"...");
		try {
			success = false;
			Properties pps = new Properties();
			in = new BufferedInputStream(new FileInputStream(extractDir.getAbsolutePath()+"/"+Extension.PROP_FILE_CHANNEL));
			pps.load(in);
			pps.setProperty(ApkInfo.Extension.PROP_KEY_CHANNEL_ID, modified.extension.channelId);
			pps.setProperty(ApkInfo.Extension.PROP_KEY_CHILD_CHANNEL_ID, modified.extension.childChannelId);
			pps.setProperty(ApkInfo.Extension.PROP_KEY_VERSION_NAME, modified.extension.versionName);
			out = new FileOutputStream(extractDir.getAbsolutePath()+"/"+Extension.PROP_FILE_CHANNEL);
			pps.store(out, null);
			success=true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		}finally{
			IOUtil.closeIO(in);
			IOUtil.closeIO(out);
		}
		if(success){
			Log.i("写入"+Extension.PROP_FILE_CHANNEL+"成功!");
		}else{
			Log.e("写入"+Extension.PROP_FILE_CHANNEL+"失败!");
		}
		return success;
	}
}
