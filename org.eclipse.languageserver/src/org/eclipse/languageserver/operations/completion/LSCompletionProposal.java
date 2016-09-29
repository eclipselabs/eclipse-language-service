/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver.operations.completion;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension7;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import io.typefox.lsapi.CompletionItem;

public class LSCompletionProposal implements ICompletionProposal, ICompletionProposalExtension,
		ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4,
		ICompletionProposalExtension5, ICompletionProposalExtension6, ICompletionProposalExtension7, IContextInformation {

	private CompletionItem item;
	private int initialOffset;
	private int selectionOffset;

	public LSCompletionProposal(CompletionItem item, int offset) {
		this.item = item;
		this.initialOffset = offset;
	}

	@Override
	public StyledString getStyledDisplayString(IDocument document, int offset, BoldStylerProvider boldStylerProvider) {
		String rawString = getDisplayString();
		StyledString res = new StyledString(rawString);
		if (offset != this.initialOffset) {
			try {
				String subString = document.get(this.initialOffset, offset - this.initialOffset);
				int lastIndex = 0;
				for (Character c : subString.toCharArray()) {
					int index = rawString.indexOf(c, lastIndex);
					if (index < 0) {
						return res;
					} else {
						res.setStyle(index, 1, boldStylerProvider.getBoldStyler());
						lastIndex = index + 1;
					}
				}
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		return res; 
	}

	@Override
	public String getDisplayString() {
		return this.item.getLabel();
	}
	
	@Override
	public StyledString getStyledDisplayString() {
		return new StyledString(getDisplayString()); 
	}

	@Override
	public boolean isAutoInsertable() {
		// TODO consider what's best
		return false;
	}

	@Override
	public IInformationControlCreator getInformationControlCreator() {
		 return new AbstractReusableInformationControlCreator() {
				@Override
				public IInformationControl doCreateInformationControl(Shell shell) {
					return new DefaultInformationControl(shell, true);
				}
			};
	}
	
	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		StringBuilder res = new StringBuilder();
		if (this.item.getDocumentation() != null) {
			res.append(this.item.getDocumentation().replaceAll("\\n", "<br/>"));
		}
		if (res.length() > 0) {
			res.append("<br/>");
		}
		if (this.item.getDetail() != null) {
			res.append(this.item.getDetail().replaceAll("\\n", "<br/>"));
		}
		
		return res.toString();
	}


	@Override
	public boolean isValidFor(IDocument document, int offset) {
		return validate(document, offset, null);
	}
	
	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return item.getInsertText().substring(completionOffset -this.initialOffset);
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return completionOffset;
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	@Override
	public void unselected(ITextViewer viewer) {
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		if (offset != this.initialOffset) {
			try {
				String subString = document.get(this.initialOffset, offset - this.initialOffset);
				String insert = getInsertText();
				int lastIndex = 0;
				for (Character c : subString.toCharArray()) {
					int index = insert.indexOf(c, lastIndex);
					if (index < 0) {
						return false;
					} else {
						lastIndex = index + 1;
					}
				}
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	
	@Override
	public void apply(IDocument document) {
		if (item.getTextEdit() != null) {
			try {
				LSPEclipseUtils.applyEdit(item.getTextEdit(), document);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		} else { //compute a best edit by reusing prefixes and suffixes
			String insertText = getInsertText();
			this.selectionOffset = insertText.length();
			
			// Look for letters that are available before completion offset
			try {
				int backOffset = 0;
				int size = Math.min(this.initialOffset, insertText.length());
				while (backOffset == 0 && size != 0) {
					if (document.get(this.initialOffset - size, size).equals(insertText.substring(0, size))) {
						backOffset = size;
					}
					size--;
				}
				if (backOffset != 0) {
					insertText = insertText.substring(backOffset);
					this.selectionOffset -= backOffset;
				}
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			}
			
			// Looks for letters that were added after completion was triggered
			int aheadOffset = 0;
			try {
				while (aheadOffset < document.getLength() && aheadOffset < insertText.length() && document.getChar(this.initialOffset + aheadOffset) == insertText.charAt(aheadOffset)) {
					aheadOffset++;
				}
				insertText = insertText.substring(aheadOffset);
			} catch (BadLocationException x) {
				x.printStackTrace();
			}
			
			try {
				document.replace(this.initialOffset + aheadOffset, 0, insertText);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String getInsertText() {
		String insertText = this.item.getInsertText();
		if (insertText == null) {
			insertText = this.item.getSortText();
		}
		return insertText;
	}


	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		// TODO Auto-generated method stub
		apply(viewer.getDocument());
	}
	
	@Override
	public void apply(IDocument document, char trigger, int offset) {
		// TODO Auto-generated method stub
		apply(document);
	}

	@Override
	public char[] getTriggerCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getContextInformationPosition() {
		return SWT.RIGHT;
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(this.initialOffset + this.selectionOffset, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return this.item.getDetail();
	}

	@Override
	public Image getImage() {
		switch (this.item.getKind()) {
		case Text: // Text
		case Method: //Method
		case Function: //Function
		case Constructor: //Constructor
		case Field: //Field
		case Variable: //Variable
		case Class: //Class
		case Interface: //Interface
		case Module: //Module
		case Property: //Property
		case Unit: //Unit
		case Value: //Value
		case Enum: //Enum
		case Keyword: //Keyword
		case Snippet: //Snippet
		case Color: //Color
		case File: //File
		case Reference: //Reference
			// TODO
		}
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return this;
	}

	@Override
	public String getContextDisplayString() {
		return getAdditionalProposalInfo();
	}

	@Override
	public String getInformationDisplayString() {
		return getAdditionalProposalInfo();
	}

}
