package com.alkaid.packer.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTargetContext;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.alkaid.packer.util.IOUtil;

public class ZipFileTree extends JTree {
	/*public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame();
				ZipFileTree fileTree = new ZipFileTree();
				FileTreeModel model = new FileTreeModel(new File(
						"C:/Users/df/Desktop"));
				fileTree.setModel(model);
				fileTree.setCellRenderer(new FileTreeRenderer());

				frame.getContentPane().add(new JScrollPane(fileTree),
						BorderLayout.CENTER);
				frame.setSize(300, 700);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}*/
	
	private JPopupMenu popMenu;
	private JMenuItem addItem;
	private JMenuItem delItem;
	private JMenuItem editItem;
	private DefaultMutableTreeNode rightKeyClickedNode=null;
	public TreePath mouseInPath;
	protected FileSystemView fileSystemView = FileSystemView
			.getFileSystemView();
	
	public void setFile(File file){
		FileTreeModel model = new FileTreeModel(file);
		this.setModel(model);
		this.setCellRenderer(new FileTreeRenderer());
		this.updateUI();
		new DropListener() {
			@Override
			public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
				if(null==file) return;
				Point p = dtde.getLocation();
			    DropTargetContext dtc = dtde.getDropTargetContext();
			    JTree tree = (JTree) dtc.getComponent();
			    TreePath path = tree.getClosestPathForLocation(p.x, p.y);
				DefaultMutableTreeNode currNode = (DefaultMutableTreeNode) path.getLastPathComponent();
				if(currNode==null) return;
				addOrReplaceFile(currNode, file);
			};
			@Override
			protected void onFileDrop(File file) {
				
			}
		}.startup(this);
	}

	public ZipFileTree() {
		setRootVisible(false);
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		init();
	}
	
	private void init(){
		ActionListener actionListener=new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getSource()==editItem){
					
				}else if(e.getSource()==delItem){
					FileNode fn=(FileNode) rightKeyClickedNode.getUserObject();
					if(null!=mZipEntryClickListener){
						mZipEntryClickListener.onZipEntryDeleted((ZipEntry) fn.file);
					}
					TreeNode parent=rightKeyClickedNode.getParent();
					rightKeyClickedNode.removeFromParent();
					// 通知模型节点发生变化
					DefaultTreeModel treeModel1 = (DefaultTreeModel) getModel();
					treeModel1.nodeStructureChanged(parent);
					updateUI();
				}else if(e.getSource()==addItem){
					
					JFileChooser fileChooser = new JFileChooser();
//					fileChooser.setAcceptAllFileFilterUsed(false);
//					fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
//							"*.apk", "apk"));
					int option = fileChooser.showOpenDialog(null);
					if (option == JFileChooser.APPROVE_OPTION) {
						DefaultMutableTreeNode currNode=rightKeyClickedNode;
						File f =fileChooser.getSelectedFile();
						addOrReplaceFile(currNode, f);
					}
				}
			}
		};
		popMenu = new JPopupMenu();
        addItem = new JMenuItem("添加");
        addItem.addActionListener(actionListener);
        delItem = new JMenuItem("删除");
        delItem.addActionListener(actionListener);
//        editItem = new JMenuItem("重命名");
//        editItem.addActionListener(actionListener);
        popMenu.add(addItem);
        popMenu.add(delItem);
//        popMenu.add(editItem);
		addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillExpand(TreeExpansionEvent event)
					throws ExpandVetoException {
				DefaultMutableTreeNode lastTreeNode = (DefaultMutableTreeNode) event
						.getPath().getLastPathComponent();
				FileNode fileNode = (FileNode) lastTreeNode.getUserObject();
				if (!fileNode.isInit) {
					if (fileNode.file instanceof File) {
						File[] files;
						files = fileSystemView.getFiles((File) fileNode.file,
								false);
						for (int i = 0; i < files.length; i++) {
							FileNode childFileNode = new FileNode(
									fileSystemView
											.getSystemDisplayName(files[i]),
									fileSystemView.getSystemIcon(files[i]),
									isZip(files[i]) ? conver2ZipFile(files[i])
											: files[i]);
							DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(
									childFileNode);
							lastTreeNode.add(childTreeNode);
						}
						// 通知模型节点发生变化
						DefaultTreeModel treeModel1 = (DefaultTreeModel) getModel();
						treeModel1.nodeStructureChanged(lastTreeNode);
					} else if (fileNode.file instanceof ZipFile) {
						Map<String, DefaultMutableTreeNode> nodes = new HashMap<String, DefaultMutableTreeNode>();
						Enumeration<? extends ZipEntry> zes = ((ZipFile) fileNode.file)
								.entries();
						while (zes.hasMoreElements()) {
							ZipEntry ze = zes.nextElement();
							MyZipEntry mze = new MyZipEntry(ze);
							addEntry2Tree(mze, lastTreeNode, nodes);
						}
						// 通知模型节点发生变化
						DefaultTreeModel treeModel1 = (DefaultTreeModel) getModel();
						treeModel1.nodeStructureChanged(lastTreeNode);
					} else if (fileNode.file instanceof ZipEntry) {
					}
					// 更改标识，避免重复加载
					fileNode.isInit = true;
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event)
					throws ExpandVetoException {

			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if(popMenu!=null&&popMenu.isShowing()){
					super.mouseMoved(e);
					return;
				}
				TreePath path = getPathForLocation(e.getX(), e.getY());

				if (path != null) {
					if (mouseInPath != null) {
						Rectangle oldRect = getPathBounds(mouseInPath);
						mouseInPath = path;
						repaint(getPathBounds(path).union(oldRect));
					} else {
						mouseInPath = path;
						Rectangle bounds = getPathBounds(mouseInPath);
						repaint(bounds);
					}
				} else if (mouseInPath != null) {
					Rectangle oldRect = getPathBounds(mouseInPath);
					mouseInPath = null;
					if(null!=oldRect)
						repaint(oldRect);
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!(e.getButton()==MouseEvent.BUTTON3)){
					super.mouseClicked(e);
					return;
				}
				TreePath path = getPathForLocation(e.getX(), e.getY());  
		        if (path == null) {
		        	super.mouseClicked(e);
		            return;
		        }
		        rightKeyClickedNode=(DefaultMutableTreeNode) path.getLastPathComponent();
		        FileNode fileNode=(FileNode) rightKeyClickedNode.getUserObject();
		        if(!(fileNode.file instanceof ZipEntry || fileNode.file instanceof ZipFile)){
		        	super.mouseClicked(e);
					return;
		        }
		        if (e.getButton() == 3) {
		            popMenu.show(ZipFileTree.this, e.getX(), e.getY());
		        }
			}
		});

		addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getLastSelectedPathComponent();// 返回最后选定的节点
				if(null==selectedNode) return;
				FileNode fileNode=(FileNode) selectedNode.getUserObject();
				if(fileNode.file instanceof ZipEntry){
					ZipEntry ze=(ZipEntry) fileNode.file;
					if(null!=mZipEntryClickListener)
						mZipEntryClickListener.onZipEntrySelected(ze);
				}
			}
		});
		this.setModel(null);
	}

	public static class FileNode {
		public FileNode(String name, Icon icon, Object file) {
			this.name = name;
			this.icon = icon;
			this.file = file;
		}

		public boolean isInit;
		public String name;
		public Icon icon;
		public Object file;
	}

	public static class FileTreeRenderer extends DefaultTreeCellRenderer {
		public FileTreeRenderer() {
		}

		@Override
		public Component getTreeCellRendererComponent(javax.swing.JTree tree,
				java.lang.Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			ZipFileTree fileTree = (ZipFileTree) tree;
			JLabel label = (JLabel) super.getTreeCellRendererComponent(tree,
					value, sel, expanded, leaf, row, hasFocus);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			FileNode fileNode = (FileNode) node.getUserObject();
			label.setText(fileNode.name);
			label.setIcon(fileNode.icon);

			label.setOpaque(false);
			if (fileTree.mouseInPath != null
					&& fileTree.mouseInPath.getLastPathComponent()
							.equals(value)) {
				label.setOpaque(true);
				label.setBackground(new Color(255, 0, 0, 90));
			}
			return label;
		}
	}

	public static class FileTreeModel extends DefaultTreeModel {
		public FileTreeModel(File file) {
			super(new DefaultMutableTreeNode(new FileNode("root", null, null)));
			FileSystemView fileSystemView = FileSystemView.getFileSystemView();
			String name=fileSystemView.getSystemDisplayName(file);
			if(file.length()>0){
				name+=" -"+IOUtil.formatHumanSize(file.length()); 
			}
			FileNode childFileNode = new FileNode(
					name,
					fileSystemView.getSystemIcon(file),
					isZip(file) ? conver2ZipFile(file) : file);
			DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(
					childFileNode);
			((DefaultMutableTreeNode) root).add(childTreeNode);
		}

		@Override
		public boolean isLeaf(Object node) {
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
			FileNode fileNode = (FileNode) treeNode.getUserObject();
			if (fileNode.file instanceof ZipFile) {
				return false;
			} else if (fileNode.file instanceof File) {
				return ((File) fileNode.file).isFile();
			} else if (fileNode.file instanceof ZipEntry) {
				return !((ZipEntry) fileNode.file).isDirectory();
			}
			// root has not 'file' member
			return false;
		}
	}

	private static boolean isZip(File file) {
		String name = file.getName();
		return name.endsWith(".zip") || name.endsWith(".apk");
	}

	private static ZipFile conver2ZipFile(File file) {
		try {
			ZipFile zf = new ZipFile(file);
			return zf;
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void addEntry2Tree(MyZipEntry ze, DefaultMutableTreeNode root,
			Map<String, DefaultMutableTreeNode> nodes) {
		File f = new File(ze.getName());
		FileNode childFileNode = new FileNode(
				getNodeDisplayName(ze),
				fileSystemView.getSystemIcon(f), ze);
		DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(
				childFileNode);

		File parent = f.getParentFile();
		// System.out.println(ze.getName()+"  parent:"+parent);
		// parent == null => parentNode is the root
		if (parent == null) {
			if(null==nodes.get(f.getPath())){
				root.add(childTreeNode);
				nodes.put(f.getPath(), childTreeNode);
			}
			return;
		} else {
			DefaultMutableTreeNode parentNode = nodes.get(parent.getPath());
			if (null == parentNode) {
				MyZipEntry z2 = new MyZipEntry(parent.getPath());
				z2.setIsDirectory(true);
				addEntry2Tree(z2, root, nodes);
			} else {
				parentNode.add(childTreeNode);
				nodes.put(f.getPath(), childTreeNode);
				return;
			}
		}

	}
	
	private void addFile2Entry(DefaultMutableTreeNode root,File file,String excludPath){
		String zeName=file.getAbsolutePath().replace(excludPath, "");
		FileNode fn=(FileNode) root.getUserObject();
		if(fn.file instanceof ZipEntry){
			zeName = ((ZipEntry)fn.file).getName()+"/"+ file.getAbsolutePath().replace(excludPath, "");
		}
		zeName=zeName.replace("\\", "/");
		MyZipEntry ze=new MyZipEntry(zeName);
		ze.setIsDirectory(file.isDirectory());
		FileNode childFileNode = new FileNode(
				getNodeDisplayName(file),
				fileSystemView.getSystemIcon(file), ze);
		DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(
				childFileNode);
		root.add(childTreeNode);
		if(file.isDirectory()){
			for (File f : file.listFiles()) {
				addFile2Entry(childTreeNode, f, excludPath);
			}
		}
	}

	private static class MyZipEntry extends ZipEntry {
		private boolean isDir;

		public MyZipEntry(ZipEntry zipEntry) {
			super(zipEntry);
		}

		public MyZipEntry(String name) {
			super(name);
		}

		public void setIsDirectory(boolean isDir) {
			this.isDir = isDir;
		}

		@Override
		public boolean isDirectory() {
			return super.isDirectory() || isDir;
		}
	}
	

	private String getNodeDisplayName(File file){
		String name=fileSystemView.getSystemDisplayName(file);
		if(file.length()>0){
			name+=" -"+IOUtil.formatHumanSize(file.length()); 
		}
		return name;
	}
	private String getNodeDisplayName(ZipEntry ze){
		File f = new File(ze.getName());
		String name=fileSystemView.getSystemDisplayName(f);
		if(ze.getSize()>0){
			name+=" -"+IOUtil.formatHumanSize(ze.getSize()); 
		}
		return name;
	}
	/**
	 * 向树中添加文件
	 * @param currNode
	 * @param file
	 */
	private void addOrReplaceFile(DefaultMutableTreeNode currNode,
			File file) {
		DefaultMutableTreeNode parent=currNode.isLeaf()?(DefaultMutableTreeNode) currNode.getParent():currNode;
		String excludPath=file.getAbsolutePath().replace(file.getName(), "");
		String zeName=file.getAbsolutePath().replace(excludPath, "");
		FileNode fn=(FileNode) parent.getUserObject();
		if(fn.file instanceof ZipEntry){
			zeName = ((ZipEntry)fn.file).getName()+"/"+ file.getAbsolutePath().replace(excludPath, "");
		}
		zeName=zeName.replace("\\", "/");
		MyZipEntry ze=new MyZipEntry(zeName);
		ze.setSize(file.length());
		ze.setIsDirectory(file.isDirectory());
		//检查是否已存在同名文件
		DefaultMutableTreeNode existNode=null;
		Enumeration<TreeNode > childs=parent.children();
		while(childs.hasMoreElements()){
			DefaultMutableTreeNode c= (DefaultMutableTreeNode) childs.nextElement();
			FileNode cfn=(FileNode) c.getUserObject();
			if(((ZipEntry)cfn.file).getName().equals(zeName)){
				existNode=c;
				break;
			}
		}
		if(existNode!=null){
			int optionOverride = JOptionPane.showConfirmDialog(null,
			 "文件"+zeName+"已存在，是否覆盖？",
			 "提示", JOptionPane.YES_NO_OPTION);
			 if(optionOverride==JOptionPane.YES_OPTION){
				 if(mZipEntryClickListener!=null){
					 mZipEntryClickListener.onZipEntryDeleted((ZipEntry) ((FileNode) existNode.getUserObject()).file);
				 }
				 existNode.removeFromParent();
			 }else{
				 return;
			 }
		}
		addFile2Entry(parent, file, excludPath);
		if(null!=mZipEntryClickListener){
			mZipEntryClickListener.onZipEntryAdded(ze, file);
		}
		// 通知模型节点发生变化
		DefaultTreeModel treeModel1 = (DefaultTreeModel) getModel();
		treeModel1.nodeStructureChanged(parent);
		updateUI();
	}
	
	private ZipEntryClickListener mZipEntryClickListener;
	public void setZipEntryClickListener(
			ZipEntryClickListener mZipEntryClickListener) {
		this.mZipEntryClickListener = mZipEntryClickListener;
	}

	public static interface ZipEntryClickListener{
		public void onZipEntrySelected(ZipEntry ze);
//		public void onZipEntryRightKeyDown(ZipEntry ze);
		public void onZipEntryDeleted(ZipEntry ze);
		public void onZipEntryAdded(ZipEntry ze,File file);
	}
}