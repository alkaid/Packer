/**
 * 
 */
package com.alkaid.packer.view;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * 添加appen机能
 * 
 */
public class JTextPaneAdv extends JTextPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6664735091731450518L;
	private Color color=Color.BLACK;
	public void append(String str) {
		Document doc = getDocument();
		if (doc != null) {
			try {
				doc.insertString(doc.getLength(), str, color==Color.BLACK?null:getColorAttribute(color));
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}
	
	private AttributeSet getColorAttribute(Color color){
		SimpleAttributeSet attr=new SimpleAttributeSet();
		StyleConstants.setForeground(attr, color);
		return attr;
	}
	
	public void setColor(Color color){
		this.color=color;
		/*MutableAttributeSet attr = new SimpleAttributeSet();
		StyleConstants.setForeground(attr, color);
		StyleConstants.setBackground(attr, color);
		setCharacterAttributes(this, attr, false);*/
	}
	
	/*public static final void setCharacterAttributes(JEditorPane editor,
			AttributeSet attr, boolean replace) {
		int p0 = editor.getSelectionStart();
		int p1 = editor.getSelectionEnd();
		if (p0 != p1) {
			StyledDocument doc = getStyledDocument(editor);
			doc.setCharacterAttributes(p0, p1 - p0, attr, replace);
		}
		StyledEditorKit k = getStyledEditorKit(editor);
		MutableAttributeSet inputAttributes = k.getInputAttributes();
		if (replace) {
			inputAttributes.removeAttributes(inputAttributes);
		}
		inputAttributes.addAttributes(attr);
	}

	protected static final StyledDocument getStyledDocument(JEditorPane e) {
		Document d = e.getDocument();
		if (d instanceof StyledDocument) {
			return (StyledDocument) d;
		}
		throw new IllegalArgumentException("document must be StyledDocument");
	}

	protected static final StyledEditorKit getStyledEditorKit(JEditorPane e) {
		EditorKit k = e.getEditorKit();
		if (k instanceof StyledEditorKit) {
			return (StyledEditorKit) k;
		}
		throw new IllegalArgumentException("EditorKit must be StyledEditorKit");

	}*/
}
