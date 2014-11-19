package com.alkaid.packer.view;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class DropListener implements DropTargetListener {
	protected DropTarget dropTarget;
	protected File file;
	public DropListener() {
	}
	public void startup(Component component){
		dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY_OR_MOVE,
				this, true, null);
	}

	// Implementation of the DropTargetListener interface
	public void dragEnter(DropTargetDragEvent dtde) {
		// do nothing
		DataFlavor[] dataFlavors = dtde.getCurrentDataFlavors();
		if (dataFlavors[0].match(DataFlavor.javaFileListFlavor)) {
			try {
				Transferable tr = dtde.getTransferable();
				Object obj = tr.getTransferData(DataFlavor.javaFileListFlavor);
				List<File> files = (List<File>) obj;
				file=files.get(0);
			} catch (UnsupportedFlavorException ex) {
				ex.printStackTrace();
				file=null;
			} catch (IOException ex) {
				ex.printStackTrace();
				file=null;
			}
		}
	}

	public void dragExit(DropTargetEvent arg0) {
		// do nothing
	}

	public void dragOver(DropTargetDragEvent arg0) {
		// do nothing
	}

	public void drop(DropTargetDropEvent dtde) {
		if(null!=file){
			onFileDrop(file);
		}
	}

	public void dropActionChanged(DropTargetDragEvent arg0) {
		// do nothing
	}

	protected abstract void onFileDrop(File file);

}
