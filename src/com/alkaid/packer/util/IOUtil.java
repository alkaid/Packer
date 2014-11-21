package com.alkaid.packer.util;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
* IO操作工具类
*/
public class IOUtil {
	/** 判断文件是否存在 */
	public static boolean existsFile(String pathName){
		File file = new File(pathName);
		return file.exists();
	}
	/** 递归删除目录或文件 没有判断SD卡是否存在 */
	public static void delFileDir(String pathName) {
		File file = new File(pathName);
		delFileDir(file);
	}
	/** 递归删除目录或文件 没有判断SD卡是否存在 */
	public static boolean delFileDir(File file) {
		boolean success=true;
		if (file.exists()) {
			if (file.isFile()) {
//				success=file.delete()?success:false;
			} else if (file.isDirectory()) {
				File files[] = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					success=delFileDir(files[i])?success:false;
				}
			}
			success=file.delete()?success:false;
		}
		return success;
	}
	
	/**
	 * 读取BufferReader返回字符串
	 * @param r
	 * @return
	 * @throws IOException
	 */
	public static String readBufferReader2Str(BufferedReader r) throws IOException{
		StringBuilder strb = new StringBuilder("");
		int i = 1;
		String line = null;
		try {
			while ((line = r.readLine()) != null) {
				strb.append(line).append("\n");
				i++;
			}
			return strb.toString();
		} catch (EOFException e) {
			System.out.println("line=" + i + " " + line);
			e.printStackTrace();
		} finally{
			r.close();
		}
		return null;
	}
	
	/**
	 * 根据输入流和指定的编码方式读取数据
	 * 
	 * @param is
	 * @param enc
	 *            编码方式 若为null则用默认编码
	 * @return
	 * @throws IOException
	 */
	public static String readInputStrem2Str(InputStream is, String enc)
			throws IOException {
		BufferedReader r = null;
		if (enc != null)
			r = new BufferedReader(new InputStreamReader(is, enc), 1024);
		else
			r = new BufferedReader(new InputStreamReader(is), 1024);
		return readBufferReader2Str(r);
	}

	/**
	 * 根据输入管道读取字节流
	 * @param inStream
	 * @return
	 * @throws IOException
	 */
	public static byte[] readInputStream2Byte(InputStream inStream) throws IOException {
		ByteArrayOutputStream outSrteam = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		try {
			while ((len = inStream.read(buffer)) != -1) {
				outSrteam.write(buffer, 0, len);
			}
			return outSrteam.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			outSrteam.close();
			inStream.close();
		}
		return null;
	}
	
	/**
	 * 读取文件,返回字节
	 * @param filePath 文件路径
	 * @return  内容字节
	 * @throws FileNotFoundException
	 * @throws IOException 
	 */
	public static byte[] readFile2Byte(String filePath) throws FileNotFoundException,IOException{
		FileInputStream fi=null;
		try {
			fi = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			throw e;
		}
		BufferedInputStream bi=new BufferedInputStream(fi);
		return readInputStream2Byte(bi);
	}
	/**
	 * 读取文件，返回字符串
	 * @param filePath  文件路径
	 * @param enc	编码方式 若为null则用默认编码
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String readFile2Str(String filePath,String enc) 
			throws FileNotFoundException,IOException{
		FileInputStream fi=null;
		try {
			fi = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			throw e;
		}
		BufferedInputStream bi=new BufferedInputStream(fi);
		return readInputStrem2Str(bi, enc);
	}
	
	public static BufferedReader getBufferReader(byte[] data){
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
	}
	public static BufferedReader getBufferReader(String data){
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data.getBytes())));
	}
	
	/**
	 * 得到配置列表
	 * @param filePath  文件路径
	 * @param enc	编码方式 若为null则用默认编码
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws AlkaidException 
	 */
	public Map<String, String> getConfig(String fileName,String enc) throws FileNotFoundException, IOException {
		Map<String,String> map=new HashMap<String, String>();
		InputStream is=null;
		String str=readFile2Str(fileName, enc);
		String[] strlist=str.split("\n");
		for (String dat : strlist) {
			String[] keyvalue=dat.split("=");
			map.put(keyvalue[0], keyvalue[1]);
		}
		return map;
	}
	
	/**
	 * 将字符串保存为文件
	 * @param content
	 * @param file
	 * @throws IOException
	 */
	public static void save2File(String content, String file) throws IOException {
		createFile(file);
		FileWriter fwriter = null;
		try {
			fwriter = new FileWriter(file);
			fwriter.write(content);
		} catch (IOException ex) {
			throw ex;
		} finally {
			try {
				fwriter.flush();
				fwriter.close();
			} catch (IOException ex) {
				throw ex;
			}
		}
	}
	
	/**
	 * 拷贝文件
	 * @param file1
	 * @param file2
	 * @throws IOException
	 */
	public static void copy(String file1, String file2) throws IOException {
		File f1=new File(file1);
		File f2=new File(file2);
		copy(f1, f2);
	}
	/**
	 * 拷贝文件
	 * @param f1
	 * @param f2
	 * @throws IOException
	 */
	public static void copy(File f1, File f2) throws IOException  {
		if(!f2.getParentFile().exists()){
			f2.getParentFile().mkdirs();
		}
		int length = 2097152;
		FileInputStream in = null;
		FileOutputStream out = null;
		FileChannel inC = null;
		FileChannel outC = null;
		try {
			in = new FileInputStream(f1);
			out = new FileOutputStream(f2);
			inC = in.getChannel();
			outC = out.getChannel();
			while (true) {
				if (inC.position() == inC.size()) {
					inC.close();
					outC.close();
//				return new Date().getTime() - time;
					return;
				}
				if ((inC.size() - inC.position()) < 20971520)
					length = (int) (inC.size() - inC.position());
				else
					length = 20971520;
				inC.transferTo(inC.position(), length, outC);
				inC.position(inC.position() + length);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally{
			IOUtil.closeIO(in);
			IOUtil.closeIO(inC);
			IOUtil.closeIO(out);
			IOUtil.closeIO(outC);
		}
	}

	/**
	 * 创建任意深度的文件所在文件夹,同时创建文件
	 * 
	 * @param path
	 * @return File对象
	 * @throws IOException 
	 */
	public static File createFile(String path) throws IOException {
		File file = new File(path);
		if(!file.exists()){
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
		}
		return file;
	} 
	
	/**
	 * 解压zip文件
	 * @param archive 被解压文件路径+名称
	 * @param decompressDir
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ZipException
	 */
   public static void readByApacheZipFile(String archive, String decompressDir)   
           throws IOException, FileNotFoundException, ZipException {
   	BufferedInputStream bi;   
       ZipFile zf = new ZipFile(new File(archive));
       File f = new File(decompressDir);
       Enumeration e = zf.entries();   
       while (e.hasMoreElements()) {   
           ZipEntry ze2 = (ZipEntry) e.nextElement();   
           String entryName = ze2.getName();   
           entryName=entryName.replace("\\", "/");
           String path = decompressDir + "/" + entryName;   
           if (ze2.isDirectory()) {   
               File decompressDirFile = new File(path);   //创建目录
               if (!decompressDirFile.exists()) {   
                   decompressDirFile.mkdirs();   
               }   
           } else {   
               String fileDir = path.substring(0, path.lastIndexOf("/"));   
               File fileDirFile = new File(fileDir);   
               if (!fileDirFile.exists()) {   
                   fileDirFile.mkdirs();   
               }   
               BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(   
               		decompressDir + "/" + entryName));   
 
               bi = new BufferedInputStream(zf.getInputStream(ze2));   
               byte[] readContent = new byte[1024];   
               int readCount = bi.read(readContent);   
               while (readCount != -1) {   
                   bos.write(readContent, 0, readCount);   
                   readCount = bi.read(readContent);   
               }   
               bos.close();   
           }   
       }   
       zf.close();   
       IOUtil.delFileDir(archive);
   }
   
   /**
	 * 释放资源。
	 * 
	 * @param c
	 *            将关闭的资源
	 */
	public static void closeIO(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}