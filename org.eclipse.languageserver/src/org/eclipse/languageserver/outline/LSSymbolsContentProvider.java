/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver.outline;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

import io.typefox.lsapi.DocumentSymbolParams;
import io.typefox.lsapi.SymbolInformation;
import io.typefox.lsapi.builders.DocumentSymbolParamsBuilder;

public class LSSymbolsContentProvider implements ICommonContentProvider, ITreeContentProvider, IDocumentListener {
	
	public static final Object COMPUTING = new Object();
	
	private TreeViewer viewer;
	private List<? extends SymbolInformation> lastResponse;
	private LSPDocumentInfo info;

	private CompletableFuture<List<? extends SymbolInformation>> symbols;
	
	@Override
	public void restoreState(IMemento aMemento) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveState(IMemento aMemento) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer)viewer;
		this.info = (LSPDocumentInfo)newInput;
		info.getDocument().addDocumentListener(this);
		refreshTreeContentFromLS();
	}
			
	@Override
	public Object[] getElements(Object inputElement) {
		if (lastResponse != null) {
			return this.lastResponse.toArray();
		}
		return new Object[] { COMPUTING };
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		refreshTreeContentFromLS();
	}

	private void refreshTreeContentFromLS() {
		if (symbols != null && !symbols.isDone()) {
			symbols.cancel(true);
		}
		lastResponse = null;
		DocumentSymbolParams params = new DocumentSymbolParamsBuilder().textDocument(info.getFileUri().toString())
		        .build();
		symbols = info.getLanguageClient()
		        .getTextDocumentService().documentSymbol(params);

		symbols.thenAccept((List<? extends SymbolInformation> t) -> {
			lastResponse = t;
			viewer.getControl().getDisplay().asyncExec(() -> {
				viewer.refresh();
			});
		});
	}


}
