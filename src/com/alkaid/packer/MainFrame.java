package com.alkaid.packer;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultStyledDocument;

import com.alkaid.packer.biz.ApkPacker;
import com.alkaid.packer.biz.ApkPacker.Callback;
import com.alkaid.packer.biz.ApkParser;
import com.alkaid.packer.biz.Refactor;
import com.alkaid.packer.model.ApkInfo;
import com.alkaid.packer.model.ApkInfo.Extension;
import com.alkaid.packer.util.Log;
import com.alkaid.packer.util.Util;
import com.alkaid.packer.view.DropListener;
import com.alkaid.packer.view.JTextPaneAdv;

public class MainFrame {

	private JFrame frame;
//	private JTextField tfVerCode;
//	private JTextField tfVerName;
//	private JTextField tfPackName;
	private JTextArea taApkInfo;
	private JTextPaneAdv taLog;
	private ApkInfo originApkInfo = null;
	private ApkPacker apkPacker;
	private boolean isApkLoaded = false;
	private boolean isLoading=false;
//	private JTextField tfChanelId;
//	private JTextField tfChildChannelId;
	private JButton btnAdvance;
	private JLabel lblIcon;
	private JMenuItem miExport;
	private JMenuItem miExportAs;
	private JMenuItem miOpenFile;
	private File originApkFile;
	private JTable table;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame window = new MainFrame();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 856, 651);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(132, 10, 402, 83);
		frame.getContentPane().add(scrollPane_1);

		taApkInfo = new JTextArea();
		scrollPane_1.setViewportView(taApkInfo);
		taApkInfo.setEditable(false);

		lblIcon = new JLabel("");
		lblIcon.setForeground(Color.WHITE);
		lblIcon.setBackground(Color.WHITE);
		lblIcon.setBounds(10, 10, 84, 84);
		frame.getContentPane().add(lblIcon);

		/*JLabel lblVersionCode = new JLabel("Version Code:");
		lblVersionCode.setBounds(29, 114, 89, 23);
		frame.getContentPane().add(lblVersionCode);

		JLabel lblVersionName = new JLabel("Version Name:");
		lblVersionName.setBounds(29, 147, 89, 23);
		frame.getContentPane().add(lblVersionName);

		JLabel lblPackage = new JLabel("Package Name:");
		lblPackage.setBounds(29, 180, 89, 23);
		frame.getContentPane().add(lblPackage);

		tfVerCode = new JTextField();
		tfVerCode.setBounds(132, 115, 189, 21);
		frame.getContentPane().add(tfVerCode);
		tfVerCode.setColumns(10);

		tfVerName = new JTextField();
		tfVerName.setColumns(10);
		tfVerName.setBounds(132, 149, 189, 21);
		frame.getContentPane().add(tfVerName);

		tfPackName = new JTextField();
		tfPackName.setColumns(10);
		tfPackName.setBounds(132, 181, 189, 21);
		frame.getContentPane().add(tfPackName);*/

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(29, 350, 723, 228);
		frame.getContentPane().add(scrollPane);

		taLog = new JTextPaneAdv();
		scrollPane.setViewportView(taLog);
		taLog.setEditable(false);
		taLog.setDocument(new DefaultStyledDocument());
		Log.init(taLog);

		JLabel lblLog = new JLabel("Log:");
		lblLog.setBounds(29, 325, 54, 15);
		frame.getContentPane().add(lblLog);

		btnAdvance = new JButton("解压APK以获得更多信息");
		btnAdvance.setBounds(548, 70, 159, 23);
		frame.getContentPane().add(btnAdvance);
		btnAdvance.setVisible(false);
		
		JButton btnClear = new JButton("clear");
		btnClear.setBounds(65, 321, 93, 23);
		frame.getContentPane().add(btnClear);
		
		/*JLabel lblChannelId = new JLabel("Channel ID:");
		lblChannelId.setBounds(369, 118, 66, 15);
		frame.getContentPane().add(lblChannelId);
		
		tfChanelId = new JTextField();
		tfChanelId.setColumns(10);
		tfChanelId.setBounds(439, 115, 189, 21);
		frame.getContentPane().add(tfChanelId);
		
		JLabel lblChildChannelId = new JLabel("ChildChannelID:");
		lblChildChannelId.setBounds(344, 151, 93, 15);
		frame.getContentPane().add(lblChildChannelId);
		
		tfChildChannelId = new JTextField();
		tfChildChannelId.setColumns(10);
		tfChildChannelId.setBounds(439, 148, 189, 21);
		frame.getContentPane().add(tfChildChannelId);*/

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		miOpenFile = new JMenuItem("打开");
		menuBar.add(miOpenFile);

		miExport = new JMenuItem("导出");
		menuBar.add(miExport);

		miExportAs = new JMenuItem("导出为");
		menuBar.add(miExportAs);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(32, 104, 720, 216);
		frame.getContentPane().add(tabbedPane);
		
		Object[][] data={};
		table = new JTable();
		table.setRowSelectionAllowed(false);
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		updateTable(data);
		
		JScrollPane p1=new JScrollPane(table);
		table.setFillsViewportHeight(true);
		tabbedPane.add("PackageInfo", p1);
		
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				taLog.setText("");
			}
		});
		
		new DropListener() {
			@Override
			protected void onFileDrop(File file) {
				originApkFile=file;
				loadApk(file);
			}
		}.startup(frame);;
		
		miOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				// 去掉所有文件选项
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.apk", "apk"));
				int option = fileChooser.showOpenDialog(null);
				if (option == JFileChooser.APPROVE_OPTION) {
					originApkFile=fileChooser.getSelectedFile();
					loadApk(originApkFile);
				}
			}});

		btnAdvance.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				extractApk();
			}
		});
		
		miExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				packApk(null);
			}
		});
		
		lblIcon.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent arg0) {
			}
			public void mousePressed(MouseEvent arg0) {
			}
			public void mouseExited(MouseEvent arg0) {
			}
			public void mouseEntered(MouseEvent arg0) {
			}
			public void mouseClicked(MouseEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				// 去掉所有文件选项
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.png", "png"));
				int option = fileChooser.showOpenDialog(null);
				if (option == JFileChooser.APPROVE_OPTION) {
					changeIcon(fileChooser.getSelectedFile());
				}
			}
		});
		new DropListener() {
			@Override
			protected void onFileDrop(File file) {
				changeIcon(file);
			}
		}.startup(lblIcon);
		
		miExportAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				// 去掉所有文件选项
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.apk", "apk"));
//				fileChooser.setCurrentDirectory(originApkFile);//设置默认打开路径
				fileChooser.setSelectedFile(originApkFile);//设置默认打开路径
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);//设置保存对话框
				int option = fileChooser.showSaveDialog(null);
				if (option == JFileChooser.APPROVE_OPTION) {
					File apkFile=fileChooser.getSelectedFile();
					packApk(apkFile.getAbsolutePath());
				}
			}
		});
		
		Log.d("Launched: " + Util.getJarFile().getAbsolutePath());

	}
	
	private void extractApk(){
		apkPacker.extract(new Callback() {
			public void onPrepare() {
				btnAdvance.setEnabled(false);
				miExport.setEnabled(false);
				miExportAs.setEnabled(false);
			}
			public boolean onConfirmDeleteAlreadyExistFile(File extractDir) {
				int option = JOptionPane.showConfirmDialog(null, "解压目录"+extractDir.getAbsolutePath()+"已存在，是否删除？\n\n注意：：选择“否”则将使用已存在目录解析APK明细项，可能与APK原信息不符！！！", "提示", JOptionPane.YES_NO_OPTION);
				return option==JOptionPane.YES_OPTION;
			}
			public void onDone(Boolean success) {
				//当取消删除APK解压目录时，则用已存在目录加载APK明细
				if(null==success || success){
					Extension ex1=Refactor.readApkExtension(apkPacker.getExtractDir());
					originApkInfo.extension=ex1;
					updateForm();
				}
				btnAdvance.setEnabled(true);
				miExport.setEnabled(true);
				miExportAs.setEnabled(true);
			}
		});
	}
	
	private void packApk(String apkPath){
		miExport.setEnabled(false);
		miExportAs.setEnabled(false);
		boolean refactRet = new Refactor(apkPacker.getExtractDir(),originApkInfo, createModifiedApkInfo(),new Log.DefaultOnLogListener()).renamePackage();
		if(!refactRet){
			miExport.setEnabled(true);
			miExportAs.setEnabled(true);
			return;
		}
		apkPacker.pack(apkPath,new Callback() {
			public void onPrepare() {
				miExport.setEnabled(false);
				miExportAs.setEnabled(false);
			}
			public boolean onConfirmDeleteAlreadyExistFile(
					File extractDir) {
				return false;
			}
			public void onDone(Boolean success) {
				miExport.setEnabled(true);
				miExportAs.setEnabled(true);
				
			}
		});
	}
	
	private void changeIcon(File newIcon){
		Log.d("正在拷贝临时icon...");
		try {
			apkPacker.createModifiedTempIcon(newIcon);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(e.getMessage());
			Log.e("临时icon拷贝失败...");
			return;
		}
		ImageIcon img = null;
		try {
			img = new ImageIcon(ImageIO.read(apkPacker.getModifiedTempApkIcon()).getScaledInstance(
					84, 84, Image.SCALE_DEFAULT));
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Icon打开失败.请重启程序重试");
			return;
		}
		lblIcon.setIcon(img);
	}

	private void updateForm() {
		taApkInfo.setText(originApkInfo.toString());
		taApkInfo.setSelectionStart(0);
		taApkInfo.setSelectionEnd(0);
		updateTable(getTableData());
		Log.d("数据表格已刷新");
	}
	
	private ApkInfo createModifiedApkInfo(){
//		if(!tfPackName.getText().trim().equals(""))
//			modifiedApkInfo.setPackageName(tfPackName.getText().trim());
//		if(!tfVerCode.getText().trim().equals(""))
//			modifiedApkInfo.setVersionCode(tfVerCode.getText().trim());
//		if(!tfVerName.getText().trim().equals("")){
//			modifiedApkInfo.setVersionName(tfVerName.getText().trim());
//			modifiedApkInfo.extension.versionName=modifiedApkInfo.getVersionName();
//		}
//		if(!tfChanelId.getText().trim().equals(""))
//			modifiedApkInfo.extension.channelId=tfChanelId.getText().trim();
//		if(!tfChildChannelId.getText().trim().equals(""))
//			modifiedApkInfo.extension.childChannelId=tfChildChannelId.getText().trim();
		if(table.isEditing()){
			table.getCellEditor().stopCellEditing();
		}
		ApkInfo modifiedApkInfo=(ApkInfo) originApkInfo.clone();
		modifiedApkInfo.metaDatas=new HashMap<String, String>();
		for(int i=0;i<table.getModel().getRowCount();i++){
			String key=(String) table.getModel().getValueAt(i, 0);
			String originValue= (String) table.getModel().getValueAt(i, 1);
			String newValue= (String) table.getModel().getValueAt(i, 2);
			if(key.equals(ApkParser.VERSION_CODE)){
				modifiedApkInfo.setVersionCode(newValue);
			}else if(key.equals(ApkParser.VERSION_NAME)){
				modifiedApkInfo.setVersionName(newValue);
				modifiedApkInfo.extension.versionName=modifiedApkInfo.getVersionName();
			}else if(key.equals(ApkParser.PACKAGE)){
				modifiedApkInfo.setPackageName(newValue);
			}else if(key.equals(Extension.PROP_KEY_CHANNEL_ID)){
				modifiedApkInfo.extension.channelId=newValue;
			}else if(key.equals(Extension.PROP_KEY_CHILD_CHANNEL_ID)){
				modifiedApkInfo.extension.childChannelId=newValue;
			}else if(key.startsWith(TABLE_KEY_METADATA_PRE)){
				key=key.replace(TABLE_KEY_METADATA_PRE, "");
				modifiedApkInfo.metaDatas.put(key,newValue);
			}
		}
		return modifiedApkInfo;
	}
	
	private void loadApk(File apkFile) {
		miOpenFile.setEnabled(false);
		isLoading=true;
		// 获取基本信息
		String path = apkFile.getAbsolutePath();
		ApkParser apkParser = new ApkParser();
		originApkInfo = null;
		try {
			originApkInfo = apkParser.getApkInfo(path);
			Log.i("APK打开成功: " + path);
		} catch (Exception e) {
			e.printStackTrace();
			taApkInfo.setText(e.getMessage());
			Log.e(e.getMessage());
			Log.e("APK打开失败: " + path + "\n请重启程序重试");
			isLoading=false;
			miOpenFile.setEnabled(true);
			return;
		}
		updateForm();
		apkPacker = new ApkPacker(apkFile,originApkInfo);
		btnAdvance.setEnabled(true);

		InputStream is = ApkParser.extractFileFromApk(apkFile,originApkInfo.getApplicationIcon());
		ImageIcon img = null;
		try {
			img = new ImageIcon(ImageIO.read(is).getScaledInstance(
					84, 84, Image.SCALE_DEFAULT));
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Icon打开失败.请重启程序重试");
			isLoading=false;
			miOpenFile.setEnabled(true);
			return;
		}
		lblIcon.setIcon(img);
		
		//加载明细
		extractApk();
		isApkLoaded=true;
		isLoading=false;
		miOpenFile.setEnabled(true);
	}
	
	private void updateTable(Object[][] data){
		String[] columnNames={"属性","原值","修改值","恢复默认"};
		DefaultTableModel m=new DefaultTableModel(data,columnNames){
			@Override
			public boolean isCellEditable(int row, int col) {
				return col==2||col==3;
			}
		};
		table.setModel(m);
		table.getColumnModel().getColumn(3).setCellEditor(new RestoreCellEditor());
		table.getColumnModel().getColumn(3).setCellRenderer(new TableCellRenderer() {
			public Component getTableCellRendererComponent(JTable arg0, Object arg1,
					boolean arg2, boolean arg3, int arg4, int arg5) {
				JButton btn=new JButton();
				btn.setText("restore");
				return new JButton();
			}
		});
		table.getColumnModel().getColumn(3).setPreferredWidth(1);
		table.repaint();
		table.updateUI();
	}
	
	private static final String TABLE_KEY_METADATA_PRE="("+ApkInfo.Manifest.application.meta_data.meta_data+")";
	private Object[][] getTableData(){
		//versioncode versionname packagename 3
		int row=3;
		//properties 2
		row+=2;
		//meta-data
		if(apkPacker!=null){
			try {
				Log.d("正在解析meta-data...");
				Refactor.loadMetadatas(apkPacker.getExtractDir(), originApkInfo);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e("meta-data加载失败！原因："+e.getMessage());
			}
			}
		row+=originApkInfo.metaDatas.size();
		Object[][] data=new Object[row][3];
		int i=0,j=0;
		//versioncode versionname packagename 3
		data[i][j++]=ApkParser.VERSION_NAME;
		data[i][j++]=originApkInfo.getVersionName();
		data[i][j++]=originApkInfo.getVersionName();
		i++;j=0;
		data[i][j++]=ApkParser.VERSION_CODE;
		data[i][j++]=originApkInfo.getVersionCode();
		data[i][j++]=originApkInfo.getVersionCode();
		i++;j=0;
		data[i][j++]=ApkParser.PACKAGE;
		data[i][j++]=originApkInfo.getPackageName();
		data[i][j++]=originApkInfo.getPackageName();
		//properties 
		i++;j=0;
		data[i][j++]=ApkInfo.Extension.PROP_KEY_CHANNEL_ID;
		data[i][j++]=originApkInfo.extension.channelId;
		data[i][j++]=originApkInfo.extension.channelId;
		i++;j=0;
		data[i][j++]=ApkInfo.Extension.PROP_KEY_CHILD_CHANNEL_ID;
		data[i][j++]=originApkInfo.extension.childChannelId;
		data[i][j++]=originApkInfo.extension.childChannelId;
		//meta-data
		for (String k : originApkInfo.metaDatas.keySet()) {
			i++;j=0;
			data[i][j++]=TABLE_KEY_METADATA_PRE+k;
			data[i][j++]=originApkInfo.metaDatas.get(k);
			data[i][j++]=originApkInfo.metaDatas.get(k);
		}
		return data;
	}
	
	private static class RestoreCellEditor extends DefaultCellEditor{
		public RestoreCellEditor() {
			super(new JCheckBox());
		}
		@Override
		public Component getTableCellEditorComponent(final JTable table, Object value,  
	            boolean isSelected,final int row, int column) {
//			JPanel pan=new JPanel();
//			pan.setLayout(null);
			JButton btnRestore=new JButton();
			btnRestore.setText("restore");
			btnRestore.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					table.getModel().setValueAt(table.getModel().getValueAt(row, 1), row, 2);
				}
			});
//			pan.add(btnRestore);
//			if (isSelected) {  
//				btnRestore.setForeground(table.getSelectionForeground());  
//				btnRestore.setBackground(table.getSelectionBackground());  
//			} else {  
//				btnRestore.setForeground(table.getForeground());  
//				btnRestore.setBackground(table.getBackground());  
//			}  
	        return btnRestore;
	    }  
		@Override
		public Object getCellEditorValue() {
			return "restore";
		}
	}
}
