/**
 * 
 */
package com.alkaid.packer.biz;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.ArrayList;
import java.util.List;
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

import com.alkaid.packer.common.Constants;
import com.alkaid.packer.model.ApkInfo;
import com.alkaid.packer.model.ApkInfo.Extension;
import com.alkaid.packer.model.ApkInfo.Manifest;
import com.alkaid.packer.util.IOUtil;
import com.alkaid.packer.util.Log;
import com.alkaid.packer.util.Log.LogInfo;
import com.alkaid.packer.util.Log.OnLogListener;
import com.alkaid.packer.util.Log.Tag;
import com.alkaid.packer.util.Properties;

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
	private ApkPacker packer;
	private String[] channelIds;
	private List<File> tempChannelDir;
	
	public List<File> getTempChannelDir() {
		return tempChannelDir;
	}

	public Refactor(ApkPacker packer, ApkInfo modified,
			OnLogListener onLogListener) {
		this.extractDir = packer.getExtractDir();
		this.origin = packer.getOriginApkInfo();
		this.modified = modified;
		this.onLogListener = onLogListener;
		this.packer=packer;
		this.channelIds=modified.extension.channelId.split(Constants.CHANNLE_SEPERATOR);
		this.tempChannelDir=new ArrayList<File>();
		for(String cid:channelIds){
			File f=new File(packer.getUnzipDir().getAbsolutePath()+File.separator+cid);
			tempChannelDir.add(f);
		}
	}
	
	public boolean refactor(){
		onLogListener.onLog(new LogInfo(Tag.info, "正在重构APK..."));
		boolean success=true;
		do {
			if(origin.isManifestDiff(modified)){
				if(!renamePackage()){
					success=false;
					break;
				}
				if(!reManifest()){
					success=false;
					break;
				}
				if(!rewriteChannelProperties()){
					success=false;
					break;
				}
			}else{
				if(!rewriteChannelProperties()){
					success=false;
					break;
				}
			}
		} while (false);
		if(success){
			onLogListener.onLog(new LogInfo(Tag.info, "APK重构成功！"));
		}else{
			onLogListener.onLog(new LogInfo(Tag.error, "APK重构失败！"));
		}
		return success;
	}

	private boolean reManifest() {
		boolean success = false;
		onLogListener
				.onLog(new LogInfo(Tag.info, "正在载入AndroidManifext.xml..."));
		File f1 = new File(extractDir.getAbsolutePath() + File.separator
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

	private boolean renamePackage() {
		if(origin.getPackageName().equals(modified.getPackageName()))
			return true;
		boolean success = true;
		onLogListener.onLog(new LogInfo(Tag.info, "正在重构包名..."));
		String originPackName = origin.getPackageName().replace(".", File.separator);
		String newPackName = modified.getPackageName().replace(".", File.separator);
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
					+ originPackName + File.separator + f.getName()));
			File f2 = new File(newPackDir.getAbsolutePath() + File.separator + f.getName());
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
	
	private boolean rewriteChannelProperties(){
		String[] childIds=modified.extension.childChannelId.split(Constants.CHANNLE_SEPERATOR);
		for(int i=0;i<tempChannelDir.size();i++){
			IOUtil.delFileDir(tempChannelDir.get(i));
			File dest=new File(tempChannelDir.get(i).getAbsolutePath()+File.separator+ApkInfo.Extension.PROP_FILE_CHANNEL);
			try {
				IOUtil.copy(packer.getTempChannelProp(),dest );
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(e.getMessage());
				Log.e("写入"+Extension.PROP_FILE_CHANNEL+"失败!");
				return false;
			}
			ApkInfo.Extension ex=new Extension();
			ex.channelId=channelIds[i];
			ex.childChannelId=childIds[i];
			ex.versionName=modified.extension.versionName;
			this.rewriteChannelProperties(dest,ex);
		}
		return true;
	}
	
	private boolean rewriteChannelProperties(File channelProp,Extension ex){
		boolean success = false;
		InputStream in=null;
		OutputStream out=null;
		Log.i("正在写入"+ApkInfo.Extension.PROP_FILE_CHANNEL+"...");
		try {
			success = false;
			Properties pps = new Properties();
			in = new BufferedInputStream(new FileInputStream(channelProp));
			pps.load(in);
			pps.setProperty(ApkInfo.Extension.PROP_KEY_CHANNEL_ID, ex.channelId);
			pps.setProperty(ApkInfo.Extension.PROP_KEY_CHILD_CHANNEL_ID, ex.childChannelId);
			pps.setProperty(ApkInfo.Extension.PROP_KEY_VERSION_NAME, ex.versionName);
			out = new BufferedOutputStream(new FileOutputStream(channelProp));
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
