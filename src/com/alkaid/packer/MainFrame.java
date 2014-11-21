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
import com.alkaid.packer.common.Constants;
import com.alkaid.packer.model.ApkInfo;
import com.alkaid.packer.model.ApkInfo.Extension;
import com.alkaid.packer.util.IOUtil;
import com.alkaid.packer.util.Log;
import com.alkaid.packer.util.Util;
import com.alkaid.packer.view.DropListener;
import com.alkaid.packer.view.JTextPaneAdv;
import javax.swing.SwingConstants;

public class MainFrame {

	private JFrame frame;
	// private JTextField tfVerCode;
	// private JTextField tfVerName;
	// private JTextField tfPackName;
	private JTextArea taApkInfo;
	private JTextPaneAdv taLog;
	private ApkInfo originApkInfo = null;
	private ApkPacker apkPacker;
//	private boolean isApkLoaded = false;
//	private boolean isLoading = false;
	// private JTextField tfChanelId;
	// private JTextField tfChildChannelId;
	private JLabel lblIcon;
	private JMenuItem miExport;
	private JMenuItem miExportAs;
	private JMenuItem miOpenFile;
	private File originApkFile;
	private JTable tbPackageInfo;
	private JTable tbAssets;
	private JLabel lblTitle;
	private long startTime;

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
		scrollPane_1.setBounds(336, 10, 474, 94);
		frame.getContentPane().add(scrollPane_1);

		taApkInfo = new JTextArea();
		scrollPane_1.setViewportView(taApkInfo);
		taApkInfo.setEditable(false);
		
		lblTitle = new JLabel("");
		lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
		lblTitle.setVerticalAlignment(SwingConstants.TOP);
		lblTitle.setBounds(104, 10, 277, 94);
		frame.getContentPane().add(lblTitle);
		lblTitle.setText("<html>名称：<br/>版本名:    版本号:</html>");

		lblIcon = new JLabel("");
		lblIcon.setOpaque(true);
		lblIcon.setBackground(Color.GRAY);
		lblIcon.setBounds(10, 10, 84, 84);
		frame.getContentPane().add(lblIcon);

		/*
		 * JLabel lblVersionCode = new JLabel("Version Code:");
		 * lblVersionCode.setBounds(29, 114, 89, 23);
		 * frame.getContentPane().add(lblVersionCode);
		 * 
		 * JLabel lblVersionName = new JLabel("Version Name:");
		 * lblVersionName.setBounds(29, 147, 89, 23);
		 * frame.getContentPane().add(lblVersionName);
		 * 
		 * JLabel lblPackage = new JLabel("Package Name:");
		 * lblPackage.setBounds(29, 180, 89, 23);
		 * frame.getContentPane().add(lblPackage);
		 * 
		 * tfVerCode = new JTextField(); tfVerCode.setBounds(132, 115, 189, 21);
		 * frame.getContentPane().add(tfVerCode); tfVerCode.setColumns(10);
		 * 
		 * tfVerName = new JTextField(); tfVerName.setColumns(10);
		 * tfVerName.setBounds(132, 149, 189, 21);
		 * frame.getContentPane().add(tfVerName);
		 * 
		 * tfPackName = new JTextField(); tfPackName.setColumns(10);
		 * tfPackName.setBounds(132, 181, 189, 21);
		 * frame.getContentPane().add(tfPackName);
		 */

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(29, 350, 781, 228);
		frame.getContentPane().add(scrollPane);

		taLog = new JTextPaneAdv();
		scrollPane.setViewportView(taLog);
		taLog.setEditable(false);
		taLog.setDocument(new DefaultStyledDocument());
		Log.init(taLog);

		JLabel lblLog = new JLabel("Log:");
		lblLog.setBounds(29, 325, 54, 15);
		frame.getContentPane().add(lblLog);

		JButton btnClear = new JButton("clear");
		btnClear.setBounds(65, 321, 93, 23);
		frame.getContentPane().add(btnClear);

		/*
		 * JLabel lblChannelId = new JLabel("Channel ID:");
		 * lblChannelId.setBounds(369, 118, 66, 15);
		 * frame.getContentPane().add(lblChannelId);
		 * 
		 * tfChanelId = new JTextField(); tfChanelId.setColumns(10);
		 * tfChanelId.setBounds(439, 115, 189, 21);
		 * frame.getContentPane().add(tfChanelId);
		 * 
		 * JLabel lblChildChannelId = new JLabel("ChildChannelID:");
		 * lblChildChannelId.setBounds(344, 151, 93, 15);
		 * frame.getContentPane().add(lblChildChannelId);
		 * 
		 * tfChildChannelId = new JTextField(); tfChildChannelId.setColumns(10);
		 * tfChildChannelId.setBounds(439, 148, 189, 21);
		 * frame.getContentPane().add(tfChildChannelId);
		 */

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		miOpenFile = new JMenuItem("打开");
		menuBar.add(miOpenFile);

		miExport = new JMenuItem("导出");
		menuBar.add(miExport);

		miExportAs = new JMenuItem("导出为");
		menuBar.add(miExportAs);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(32, 104, 778, 216);
		frame.getContentPane().add(tabbedPane);

		Object[][] data = {};
		tbPackageInfo = new JTable();
		tbPackageInfo.setRowSelectionAllowed(false);
		tbPackageInfo.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		updateTablePackageInfo(data);

		JScrollPane p1 = new JScrollPane(tbPackageInfo);
		tbPackageInfo.setFillsViewportHeight(true);
		tabbedPane.add("PackageInfo", p1);
		
		Object[][] data2 = {};
		tbAssets = new JTable();
		tbAssets.setRowSelectionAllowed(false);
		tbAssets.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		updateTablePackageInfo(data2);

		JScrollPane p2 = new JScrollPane(tbAssets);
		tbAssets.setFillsViewportHeight(true);
		tabbedPane.add("AssetsFiles", p2);
		

		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				taLog.setText("");
			}
		});

		new DropListener() {
			@Override
			protected void onFileDrop(File file) {
				originApkFile = file;
				loadApk(file);
			}
		}.startup(frame);
		;

		miOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				// 去掉所有文件选项
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
						"*.apk", "apk"));
				int option = fileChooser.showOpenDialog(null);
				if (option == JFileChooser.APPROVE_OPTION) {
					originApkFile = fileChooser.getSelectedFile();
					loadApk(originApkFile);
				}
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
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
						"*.png", "png"));
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
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
						"*.apk", "apk"));
				// fileChooser.setCurrentDirectory(originApkFile);//设置默认打开路径
				fileChooser.setSelectedFile(originApkFile);// 设置默认打开路径
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);// 设置保存对话框
				int option = fileChooser.showSaveDialog(null);
				if (option == JFileChooser.APPROVE_OPTION) {
					File apkFile = fileChooser.getSelectedFile();
					packApk(apkFile.getAbsolutePath());
				}
			}
		});

		Log.d("Launched: " + Util.getJarFile().getAbsolutePath());
		File workspace = new File(Constants.PATH_WORKSPACE);
		IOUtil.delFileDir(workspace);

	}

	private void decodeApk() {
		apkPacker.extractAndDecode(new Callback() {
			public void onPrepare() {
				miExport.setEnabled(false);
				miExportAs.setEnabled(false);
			}

			public boolean onConfirmDeleteAlreadyExistFile(File extractDir) {
				// int option = JOptionPane.showConfirmDialog(null,
				// "解压目录"+extractDir.getAbsolutePath()+"已存在，是否删除？\n\n注意：：选择“否”则将使用已存在目录解析APK明细项，可能与APK原信息不符！！！",
				// "提示", JOptionPane.YES_NO_OPTION);
				// return option==JOptionPane.YES_OPTION;
				return true;
			}

			public void onDone(Boolean success) {
				// 当取消删除APK解压目录时，则用已存在目录加载APK明细
				if (null == success || success) {
				}
				miExport.setEnabled(true);
				miExportAs.setEnabled(true);
			}
		});
	}

	private void packApk(String apkPath) {
		Log.i("开始打包...");
		startTime=System.currentTimeMillis();
		miExport.setEnabled(false);
		miExportAs.setEnabled(false);
		ApkInfo modified = createModifiedApkInfo();
		Refactor refactor = new Refactor(apkPacker, modified,
				new Log.DefaultOnLogListener());
		boolean refactRet = refactor.refactor();
		if (!refactRet) {
			miExport.setEnabled(true);
			miExportAs.setEnabled(true);
			return;
		}
		apkPacker.pack(originApkInfo.isManifestDiff(modified), apkPath,
				refactor.getTempChannelDir(), new Callback() {
					public void onPrepare() {
						miExport.setEnabled(false);
						miExportAs.setEnabled(false);
					}
					public void onDone(Boolean success) {
						miExport.setEnabled(true);
						miExportAs.setEnabled(true);
						if(success){
							int consume=(int)((System.currentTimeMillis()-startTime)/1000);
							Log.i("打包结束，花费"+consume+"秒");
						}
					}
				});
	}

	private void changeIcon(File newIcon) {
		if(!newIcon.getName().endsWith(".png")){
			Log.w("目前仅支持png格式！");
			return;
		}else if(null==apkPacker){
			Log.w("还未加载apk,请先加载apk！");
			return;
		}
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
			img = new ImageIcon(ImageIO.read(apkPacker.getTempIcon())
					.getScaledInstance(84, 84, Image.SCALE_DEFAULT));
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
		updateTablePackageInfo(getPackageInfoTableData());
		Log.d("数据表格已刷新");
	}

	private ApkInfo createModifiedApkInfo() {
		if (tbPackageInfo.isEditing()) {
			tbPackageInfo.getCellEditor().stopCellEditing();
		}
		ApkInfo modifiedApkInfo = (ApkInfo) originApkInfo.clone();
		modifiedApkInfo.metaDatas = new HashMap<String, String>();
		for (int i = 0; i < tbPackageInfo.getModel().getRowCount(); i++) {
			String key = (String) tbPackageInfo.getModel().getValueAt(i, 0);
			String originValue = (String) tbPackageInfo.getModel().getValueAt(i, 1);
			String newValue = (String) tbPackageInfo.getModel().getValueAt(i, 2);
			if (key.equals(ApkParser.VERSION_CODE)) {
				modifiedApkInfo.setVersionCode(newValue);
			} else if (key.equals(ApkParser.VERSION_NAME)) {
				modifiedApkInfo.setVersionName(newValue);
				modifiedApkInfo.extension.versionName = modifiedApkInfo
						.getVersionName();
			} else if (key.equals(ApkParser.PACKAGE)) {
				modifiedApkInfo.setPackageName(newValue);
			} else if (key.equals(Extension.PROP_KEY_CHANNEL_ID)) {
				newValue=newValue.replaceAll("[，；;/\\\\'+-]", ",");
				tbPackageInfo.getModel().setValueAt(newValue,i, 2);
				modifiedApkInfo.extension.channelId = newValue;
			} else if (key.equals(Extension.PROP_KEY_CHILD_CHANNEL_ID)) {
				newValue=newValue.replaceAll("[，；;/\\\\'+-]", ",");
				tbPackageInfo.getModel().setValueAt(newValue,i, 2);
				modifiedApkInfo.extension.childChannelId = newValue;
			} else if (key.startsWith(TABLE_KEY_METADATA_PRE)) {
				key = key.replace(TABLE_KEY_METADATA_PRE, "");
				modifiedApkInfo.metaDatas.put(key, newValue);
			}
		}
		return modifiedApkInfo;
	}

	private void loadApk(File apkFile) {
		if(!apkFile.getName().endsWith(".apk")){
			Log.w("不支持非apk格式!");
			return;
		}
		miOpenFile.setEnabled(false);
		miExport.setEnabled(false);
		miExportAs.setEnabled(false);
//		isLoading = true;
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
//			isLoading = false;
			miOpenFile.setEnabled(true);
			return;
		}
		updateForm();
		apkPacker = new ApkPacker(apkFile, originApkInfo);
		
		StringBuilder title=new StringBuilder();
		title.append("<html><font size='7'>")
		.append(originApkInfo.getApplicationLable())
		.append("</font><br/>")
		.append("    包名：").append(originApkInfo.getPackageName())
		.append("<br/>")
		.append("版本名：").append(originApkInfo.getVersionName())
		.append("     版本号：").append(originApkInfo.getVersionCode())
		.append("</html>");
		lblTitle.setText(title.toString());

		InputStream is = ApkParser.extractFileFromApk(apkFile,
				originApkInfo.getApplicationIcon());
		ImageIcon img = null;
		try {
			img = new ImageIcon(ImageIO.read(is).getScaledInstance(84, 84,
					Image.SCALE_DEFAULT));
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Icon打开失败.请重启程序重试");
//			isLoading = false;
			miOpenFile.setEnabled(true);
			return;
		}
		lblIcon.setOpaque(false);
		lblIcon.setIcon(img);

		// 加载明细
		apkPacker.loadApk();
//		isApkLoaded = true;
//		isLoading = false;
		miOpenFile.setEnabled(true);
		updateForm();
		decodeApk();
	}

	private void updateTablePackageInfo(Object[][] data) {
		String[] columnNames = { "属性", "原值", "修改值", "恢复默认" };
		DefaultTableModel m = new DefaultTableModel(data, columnNames) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return col == 2 || col == 3;
			}
		};
		tbPackageInfo.setModel(m);
		tbPackageInfo.getColumnModel().getColumn(3)
				.setCellEditor(new ButtonCellEditor("恢复"));
		tbPackageInfo.getColumnModel().getColumn(3)
				.setCellRenderer(new ButtonCellRender("恢复"));
		tbPackageInfo.getColumnModel().getColumn(3).setPreferredWidth(1);
		tbPackageInfo.repaint();
		tbPackageInfo.updateUI();
	}
	
	private void updateTableAssets(Object[][] data) {
		String[] columnNames = { "文件", "原值", "修改值", "编辑","替换","恢复" };
		DefaultTableModel m = new DefaultTableModel(data, columnNames) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return col>=3;
			}
		};
		tbPackageInfo.setModel(m);
		tbPackageInfo.getColumnModel().getColumn(3)
			.setCellEditor(new ButtonCellEditor("编辑"));
		tbPackageInfo.getColumnModel().getColumn(4)
			.setCellEditor(new ButtonCellEditor("替换"));
		tbPackageInfo.getColumnModel().getColumn(5)
			.setCellEditor(new ButtonCellEditor("恢复"));
		tbPackageInfo.getColumnModel().getColumn(3)
			.setCellRenderer(new ButtonCellRender("编辑"));
		tbPackageInfo.getColumnModel().getColumn(4)
			.setCellRenderer(new ButtonCellRender("替换"));
		tbPackageInfo.getColumnModel().getColumn(5)
			.setCellRenderer(new ButtonCellRender("恢复"));
		tbPackageInfo.getColumnModel().getColumn(3).setPreferredWidth(1);
		tbPackageInfo.getColumnModel().getColumn(4).setPreferredWidth(1);
		tbPackageInfo.getColumnModel().getColumn(5).setPreferredWidth(1);
		tbPackageInfo.repaint();
		tbPackageInfo.updateUI();
	}

	private static final String TABLE_KEY_METADATA_PRE = "("
			+ ApkInfo.Manifest.application.meta_data.meta_data + ")";

	private Object[][] getPackageInfoTableData() {
		// versioncode versionname packagename 3
		int row = 3;
		// properties 2
		row += 2;
		// meta-data
		row += originApkInfo.metaDatas.size();
		Object[][] data = new Object[row][3];
		int i = 0, j = 0;
		// versioncode versionname packagename 3
		data[i][j++] = ApkParser.VERSION_NAME;
		data[i][j++] = originApkInfo.getVersionName();
		data[i][j++] = originApkInfo.getVersionName();
		i++;
		j = 0;
		data[i][j++] = ApkParser.VERSION_CODE;
		data[i][j++] = originApkInfo.getVersionCode();
		data[i][j++] = originApkInfo.getVersionCode();
		i++;
		j = 0;
		data[i][j++] = ApkParser.PACKAGE;
		data[i][j++] = originApkInfo.getPackageName();
		data[i][j++] = originApkInfo.getPackageName();
		// properties
		i++;
		j = 0;
		data[i][j++] = ApkInfo.Extension.PROP_KEY_CHANNEL_ID;
		data[i][j++] = originApkInfo.extension.channelId;
		data[i][j++] = originApkInfo.extension.channelId;
		i++;
		j = 0;
		data[i][j++] = ApkInfo.Extension.PROP_KEY_CHILD_CHANNEL_ID;
		data[i][j++] = originApkInfo.extension.childChannelId;
		data[i][j++] = originApkInfo.extension.childChannelId;
		// meta-data
		for (String k : originApkInfo.metaDatas.keySet()) {
			i++;
			j = 0;
			data[i][j++] = TABLE_KEY_METADATA_PRE + k;
			data[i][j++] = originApkInfo.metaDatas.get(k);
			data[i][j++] = originApkInfo.metaDatas.get(k);
		}
		return data;
	}
	private Object[][] getAssetsTableData() {
		//加载apk assets根目录下的文件
		Object[][] data = new Object[6][6];
		return data;
	}
	private static class ButtonCellRender implements TableCellRenderer{
		private String text="Button";
		public ButtonCellRender(String text){
			this.text=text;
		}
		public Component getTableCellRendererComponent(JTable arg0,
				Object arg1, boolean arg2, boolean arg3, int arg4,
				int arg5) {
			JButton btn = new JButton();
			btn.setText(text);
			return btn;
		} 
	}

	private static class ButtonCellEditor extends DefaultCellEditor {
		private String text="Button";
		public ButtonCellEditor(String text) {
			super(new JCheckBox());
			this.text=text;
		}

		@Override
		public Component getTableCellEditorComponent(final JTable table,
				Object value, boolean isSelected, final int row, int column) {
			// JPanel pan=new JPanel();
			// pan.setLayout(null);
			JButton btnRestore = new JButton();
			btnRestore.setText(text);
			btnRestore.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					table.getModel().setValueAt(
							table.getModel().getValueAt(row, 1), row, 2);
				}
			});
			// pan.add(btnRestore);
			// if (isSelected) {
			// btnRestore.setForeground(table.getSelectionForeground());
			// btnRestore.setBackground(table.getSelectionBackground());
			// } else {
			// btnRestore.setForeground(table.getForeground());
			// btnRestore.setBackground(table.getBackground());
			// }
			return btnRestore;
		}

		@Override
		public Object getCellEditorValue() {
			return text;
		}
	}
}
