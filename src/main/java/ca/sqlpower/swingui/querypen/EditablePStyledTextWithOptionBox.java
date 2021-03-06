/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.swingui.querypen;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.log4j.Logger;

import ca.sqlpower.object.SPVariableHelper;
import ca.sqlpower.swingui.object.InsertVariableAction;
import ca.sqlpower.swingui.object.VariableInserter;
import ca.sqlpower.swingui.object.VariableLabel;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;


/**
 * This class is similar to the EditablePStyledText class except this class also handles a Option Box which 
 * will line up right under the JEditorPane. It will handle mouse events on the options and append to the
 * JEditorPane.
 */
public class EditablePStyledTextWithOptionBox  extends EditablePStyledText {
	
private static final Logger logger = Logger.getLogger(EditablePStyledText.class);

	/**
	 * One pixel space for adjusting the whereOptionBox with the whereTextBox.
	 */
	private static final int ONE_PIXEL_SPACE=1;
	
	/**
	 * The options box that will display a list of where clause options
	 */
	private PPath whereOptionBox; 
	
	/**
	 * This listener will set the text of this PStyledText and hide the editor
	 * pane when the editor pane loses focus (ie: clicked away from the editor).
	 */
	private FocusListener editorFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			if(boxClicked) {
				boxClicked = false;
				getEditorPane().requestFocus();
				return;
			} else {
				whereOptionBox.removeFromParent();
				getStyledTextEventHandler().stopEditing();
			}
		}
		public void focusGained(FocusEvent e) {
			whereOptionBox.translate(
					getGlobalFullBounds().getX()-whereOptionBox.getXOffset()-ONE_PIXEL_SPACE,
					(getGlobalFullBounds().getY() + getEditorPane().getHeight())-whereOptionBox.getYOffset());
			if (!queryPen.getTopLayer().isAncestorOf(whereOptionBox))
				queryPen.getTopLayer().addChild(whereOptionBox);
			whereOptionBox.moveToFront();
		}
	};
	
	/**
	 * This is the width of the WHERE option's box
	 */
	private static final int WHERE_OPTION_BOX_WIDTH = 100;
	
	/**
	 * This is the height of the Where option's box
	 */
	private static final int WHERE_OPTION_BOX_HIEGHT = 90;
	
	
	private boolean boxClicked;
	
	private final QueryPen queryPen;

	private final SPVariableHelper variablesHelper;
	
	/**
	 *  This is an Array of Where Options for the whereOptionsBox
	 */
	private static final String[] whereOptions = new String[]{"<", ">", 
		"=", "<>", ">=", "<=", "BETWEEN", "LIKE", "IN", "NOT" };

	public EditablePStyledTextWithOptionBox(String startingText, QueryPen queryPenRef, PCanvas canvas, int minCharCountSize) {
		this(startingText, queryPenRef, canvas, minCharCountSize, null);
	}
	
	public EditablePStyledTextWithOptionBox(String startingText, QueryPen queryPenRef, final PCanvas canvas, int minCharCountSize, SPVariableHelper variables) {
		super(startingText, queryPenRef, canvas, minCharCountSize);
		this.queryPen = queryPenRef;
		this.variablesHelper = variables;
		
		getEditorPane().removeFocusListener(getEditorFocusListener());
		getEditorPane().addFocusListener(editorFocusListener);
		
		if (this.variablesHelper != null) {
			
			// Substitutes the variables for the nice variables labels
			VariableLabel.insertLabels(this.variablesHelper, (StyledDocument)getDocument(), getEditorPane());
			
			// Maps CTRL+SPACE to insert variable
			getEditorPane().getInputMap().put(
					KeyStroke.getKeyStroke(
							KeyEvent.VK_SPACE,
							InputEvent.CTRL_MASK),
							"insertVariable");
			getEditorPane().getActionMap().put(
					"insertVariable", 
					new InsertVariableAction(
							"Insert variable",
							this.variablesHelper, 
							null, 
							new VariableInserter() {
								public void insert(String variable) {
									try {
										getEditorPane().setText(getEditorPane().getText().trim());
										getEditorPane().getDocument().insertString(
												getEditorPane().getCaretPosition(), 
												variable, 
												null);
									} catch (BadLocationException e) {
										throw new IllegalStateException(e);
									}
									syncWithDocument();
									getStyledTextEventHandler().stopEditing();
								}
							}, 
							this.getEditorPane()));
		}
		
		whereOptionBox = PPath.createRectangle(0, 0
				, (float)WHERE_OPTION_BOX_WIDTH, (float)WHERE_OPTION_BOX_HIEGHT);
		
		whereOptionBox.addInputEventListener(new PBasicInputEventHandler() {
			@Override
			public void mousePressed(PInputEvent event) {
				boxClicked = true;
			}
		});
		
		// Add the whereOptions to the whereOptionsBox with Mouse Listeners.
		int yLoc = 0;
		int xLoc = 0;
		for(String whereOption : whereOptions) {
			final PText newOption = new PText(whereOption);
			final PNode background = new PNode();
			background.setTransparency((float)0.3);
			newOption.addAttribute(StyleConstants.FontFamily, UIManager.getFont("List.font").getFamily());
			newOption.translate((WHERE_OPTION_BOX_WIDTH/3)*xLoc+ ONE_PIXEL_SPACE*3, (getHeight()+ 1) * yLoc+ ONE_PIXEL_SPACE*5);
			background.setBounds(newOption.getBounds().getX(), newOption.getBounds().getY()
					,newOption.getBounds().getWidth()+ 3, newOption.getBounds().getHeight()+3);
			newOption.addChild(background);
			background.setPaint(Color.gray);
			background.setVisible(false);
			newOption.addInputEventListener(new PBasicInputEventHandler() {
				
				@Override
				public void mouseEntered(PInputEvent event) {
					background.setVisible(true);
					newOption.repaint();
				}
				
				@Override
				public void mouseExited(PInputEvent event) {
					background.setVisible(false);
					newOption.repaint();
				}
				
				@Override
				public void mousePressed(PInputEvent event) {
					JEditorPane whereEditorPane = getEditorPane();
					try {
						whereEditorPane.getDocument().insertString(whereEditorPane.getCaretPosition()
								, newOption.getText(), null);
					} catch (BadLocationException e) {
						logger.debug("Bad Location when trying to insert whereOption on whereText");
						throw new IllegalStateException(e);
					}
				}
				
			});
			yLoc++;
			if(yLoc > 4) {
				yLoc = 0;
				xLoc = 1;
			}
			whereOptionBox.addChild(newOption);
		}
		
	}
	
	
//	public void syncWithDocument() {
//		if (this.variablesHelper == null) {
//			super.syncWithDocument();
//		} else {
//			getEditorPane().setText(getEditorPane().getText().trim());
//			VariableLabel.insertLabelsForPicollo((StyledDocument)getDocument());
//			if (getEditorPane().getText() == null || getEditorPane().getText().trim().equals("") ) {
//				getEditorPane().setText(startingText);
//			} else if (	getEditorPane().getText().length() < minCharCountSize) {
//				StringBuffer sb = new StringBuffer();
//				for (int i = 0; i < minCharCountSize - getEditorPane().getText().length(); i++) {
//					sb.append(" ");
//				}
//				getEditorPane().setText(getEditorPane().getText() + sb.toString());
//			}
//			super.syncWithDocument();
//			VariableLabel.removeLabelsForPicollo((StyledDocument)getDocument());
//		}
//	}
	
	public PPath getOptionBox() {
		return whereOptionBox;
	}
	
}
