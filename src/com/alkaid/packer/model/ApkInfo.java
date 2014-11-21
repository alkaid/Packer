/*
 * @(#)ApkInfo.java		       version: 1.0 
 * Date:2012-1-10
 *
 * Copyright (c) 2011 CFuture09, Institute of Software, 
 * Guangdong Ocean University, Zhanjiang, GuangDong, China.
 * All rights reserved.
 */
package com.alkaid.packer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <B>ApkInfo</B>
 * <p>
 * 该类封装了一个Apk的信息。包括版本号，支持平台，图标，名称，权限，所需设备特性等。
 * </p>
 * 
 * @author CFuture.Geek_Soledad(66704238@51uc.com)
 */
public class ApkInfo implements Cloneable{
	public static final String APPLICATION_ICON_120 = "application-icon-120";
	public static final String APPLICATION_ICON_160 = "application-icon-160";
	public static final String APPLICATION_ICON_240 = "application-icon-240";
	public static final String APPLICATION_ICON_320 = "application-icon-320";
	
	/**
	 * AndroidManifest.xml中标签、属性等的常量集合
	 * @author df
	 *
	 */
	public static class Manifest{
		public static final String fileName="AndroidManifest.xml";
		public static class manifest{
			public static final String manifest="manifest";
			public static final String versionCode = "android:versionCode";
			public static final String versionName = "android:versionName";
			public static final String package_ = "package";
		}
		public static class uses_sdk{
			public static final String uses_sdk="uses-sdk";
			public static final String minSdkVersion="android:minSdkVersion";
			public static final String targetSdkVersion="android:targetSdkVersion";
			public static final String maxSdkVersion="android:maxSdkVersion";
		}
		public static class application{
			public static final String application="application";
			public static final String label="android:label";
			public static final String icon="android:icon";
			public static class activity{
				public static final String activity="activity";
				public static final String name="android:name";
				public static final String icon="android:icon";
			}
			public static class meta_data{
				public static final String meta_data="meta-data";
				public static final String name="android:name";
				public static final String value="android:value";
			}
		}
	}
	
	/**
	 * apk内部版本号
	 */
	private String versionCode = null;
	/**
	 * apk外部版本号
	 */
	private String versionName = null;
	/**
	 * apk的包名
	 */
	private String packageName = null;
	/**
	 * 支持的android平台最低版本号
	 */
	private String minSdkVersion = null;
	/**
	 * apk所需要的权限
	 */
	private List<String> usesPermissions = null;

	/**
	 * 支持的SDK版本。
	 */
	private String sdkVersion;
	/**
	 * 建议的SDK版本
	 */
	private String targetSdkVersion;
	/**
	 * 应用程序名
	 */
	private String applicationLable;
	/**
	 * 各个分辨率下的图标的路径。
	 */
	private Map<String, String> applicationIcons;

	/**
	 * 程序的图标。
	 */
	private String applicationIcon;

	/**
	 * 暗指的特性。
	 */
	private List<ImpliedFeature> impliedFeatures;

	/**
	 * 所需设备特性。
	 */
	private List<String> features;
	/**
	 * 启动界面
	 */
	private String launchableActivity;
	
	/** 附加信息 包括 /assets/channel.properties */
	public Extension extension=new Extension();
	
	public Map<String,String> metaDatas=new HashMap<String, String>();

	public ApkInfo() {
		this.usesPermissions = new ArrayList<String>();
		this.applicationIcons = new HashMap<String, String>();
		this.impliedFeatures = new ArrayList<ImpliedFeature>();
		this.features = new ArrayList<String>();
	}
	
	@Override
	public Object clone()  {
		ApkInfo o=null;
		try {
			o=(ApkInfo) super.clone();
			o.extension=(Extension) this.extension.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return o;
	}

	/**
	 * 返回版本代码。
	 * 
	 * @return 版本代码。
	 */
	public String getVersionCode() {
		return versionCode;
	}

	/**
	 * @param versionCode
	 *            the versionCode to set
	 */
	public void setVersionCode(String versionCode) {
		this.versionCode = versionCode;
	}

	/**
	 * 返回版本名称。
	 * 
	 * @return 版本名称。
	 */
	public String getVersionName() {
		return versionName;
	}

	/**
	 * @param versionName
	 *            the versionName to set
	 */
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	/**
	 * 返回支持的最小sdk平台版本。
	 * 
	 * @return the minSdkVersion
	 */
	public String getMinSdkVersion() {
		return minSdkVersion;
	}

	/**
	 * @param minSdkVersion
	 *            the minSdkVersion to set
	 */
	public void setMinSdkVersion(String minSdkVersion) {
		this.minSdkVersion = minSdkVersion;
	}

	/**
	 * 返回包名。
	 * 
	 * @return 返回的包名。
	 */
	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * 返回sdk平台版本。
	 * 
	 * @return
	 */
	public String getSdkVersion() {
		return sdkVersion;
	}

	public void setSdkVersion(String sdkVersion) {
		this.sdkVersion = sdkVersion;
	}

	/**
	 * 返回所建议的SDK版本。
	 * 
	 * @return
	 */
	public String getTargetSdkVersion() {
		return targetSdkVersion;
	}

	public void setTargetSdkVersion(String targetSdkVersion) {
		this.targetSdkVersion = targetSdkVersion;
	}

	/**
	 * 返回所需的用户权限。
	 * 
	 * @return
	 */
	public List<String> getUsesPermissions() {
		return usesPermissions;
	}

	public void setUsesPermissions(List<String> usesPermission) {
		this.usesPermissions = usesPermission;
	}

	public void addToUsesPermissions(String usesPermission) {
		this.usesPermissions.add(usesPermission);
	}

	/**
	 * 返回程序的名称标签。
	 * 
	 * @return
	 */
	public String getApplicationLable() {
		return applicationLable;
	}

	public void setApplicationLable(String applicationLable) {
		this.applicationLable = applicationLable;
	}

	/**
	 * 返回应用程序的图标。
	 * 
	 * @return
	 */
	public String getApplicationIcon() {
		return applicationIcon;
	}

	public void setApplicationIcon(String applicationIcon) {
		this.applicationIcon = applicationIcon;
	}

	/**
	 * 返回应用程序各个分辨率下的图标。
	 * 
	 * @return
	 */
	public Map<String, String> getApplicationIcons() {
		return applicationIcons;
	}

	public void setApplicationIcons(Map<String, String> applicationIcons) {
		this.applicationIcons = applicationIcons;
	}

	public void addToApplicationIcons(String key, String value) {
		this.applicationIcons.put(key, value);
	}

	public void addToImpliedFeatures(ImpliedFeature impliedFeature) {
		this.impliedFeatures.add(impliedFeature);
	}

	/**
	 * 返回应用程序所需的暗指的特性。
	 * 
	 * @return
	 */
	public List<ImpliedFeature> getImpliedFeatures() {
		return impliedFeatures;
	}

	public void setImpliedFeatures(List<ImpliedFeature> impliedFeatures) {
		this.impliedFeatures = impliedFeatures;
	}

	/**
	 * 返回应用程序所需的特性。
	 * 
	 * @return
	 */
	public List<String> getFeatures() {
		return features;
	}

	public void setFeatures(List<String> features) {
		this.features = features;
	}

	public void addToFeatures(String feature) {
		this.features.add(feature);
	}

	@Override
	public String toString() {
		return "ApkInfo [versionCode=" + versionCode + ",\n versionName="
				+ versionName + ",\n packageName=" + packageName
				+ ",\n minSdkVersion=" + minSdkVersion + ",\n usesPermissions="
				+ usesPermissions + ",\n sdkVersion=" + sdkVersion
				+ ",\n targetSdkVersion=" + targetSdkVersion
				+ ",\n applicationLable=" + applicationLable
				+ ",\n applicationIcons=" + applicationIcons
				+ ",\n applicationIcon=" + applicationIcon
				+ ",\n impliedFeatures=" + impliedFeatures + ",\n features="
				+ features + ",\n launchableActivity=" + launchableActivity + "\n]";
	}

	public String getLaunchableActivity() {
		return launchableActivity;
	}

	public void setLaunchableActivity(String launchableActivity) {
		this.launchableActivity = launchableActivity;
	}

	/*public static ApkInfo copy(ApkInfo origin){
		ApkInfo newInfo=new ApkInfo();
		newInfo.setApplicationIcon(origin.getApplicationIcon());
		newInfo.setApplicationIcons(origin.getApplicationIcons());
		newInfo.setApplicationLable(origin.getApplicationLable());
		newInfo.setFeatures(features)
	}*/
	
	public static class Extension implements Cloneable{
		public static String PROP_FILE_CHANNEL="assets"+File.separator+"channel.properties";
		public static String PROP_KEY_CHANNEL_ID="channelId";
		public static String PROP_KEY_CHILD_CHANNEL_ID="childChannelId";
		public static String PROP_KEY_VERSION_NAME="versionName";
		//properties属性
		public String channelId;
		public String childChannelId;
		public String versionName;
		
		@Override
		public Object clone() {
			Extension o=null;
			try {
				o=(Extension) super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return o;
		}
	}
	/**
	 * 比较两个apkInfo的AndroidManifest是否相同
	 * @param another
	 * @return
	 */
	public boolean isManifestDiff(ApkInfo another){
		if(this.metaDatas.size()!=another.metaDatas.size()){
			return true;
		}
		for(String key:this.metaDatas.keySet()){
			if(!this.metaDatas.get(key).equals(another.metaDatas.get(key))){
				return true;
			}
		}
		return false;
	}
	
}
