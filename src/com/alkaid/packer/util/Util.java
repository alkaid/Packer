package com.alkaid.packer.util;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

public class Util {
	public static File getJarFile() {  
		String filePath = System.getProperty("java.class.path");
		File file=new File(filePath);
		return file;
    }  
}
