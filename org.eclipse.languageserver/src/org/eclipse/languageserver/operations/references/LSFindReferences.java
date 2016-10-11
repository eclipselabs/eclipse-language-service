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
package org.eclipse.languageserver.operations.references;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.search2.internal.ui.SearchView;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import io.typefox.lsapi.Location;
import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.impl.ReferenceContextImpl;
import io.typefox.lsapi.impl.ReferenceParamsImpl;
import io.typefox.lsapi.impl.TextDocumentIdentifierImpl;

public class LSFindReferences extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		SearchView searchView = null;
		try {
			searchView = (SearchView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().showView("org.eclipse.search.ui.views.SearchView"); //$NON-NLS-1$
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (part instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) part;
			LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor(editor,
					ServerCapabilities::isReferencesProvider);

			if (info != null) {
				ISelection sel = ((AbstractTextEditor) part).getSelectionProvider().getSelection();
				if (sel instanceof TextSelection) {
					try {
						ReferenceParamsImpl params = new ReferenceParamsImpl();
						params.setPosition(LSPEclipseUtils.toPosition(((TextSelection) sel).getOffset(), info.getDocument()));
						TextDocumentIdentifierImpl identifier = new TextDocumentIdentifierImpl();
						identifier.setUri(info.getFileUri().toString());
						params.setTextDocument(identifier);
						ReferenceContextImpl context = new ReferenceContextImpl();
						context.setIncludeDeclaration(true);
						params.setContext(context);
						CompletableFuture<List<? extends Location>> references = info.getLanguageClient().getTextDocumentService().references(params);
						LSSearchResult search = new LSSearchResult(references, info.getDocument());
						search.getQuery().run(new NullProgressMonitor());
						if (searchView != null) {
							searchView.showSearchResult(search);
						}
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (part instanceof ITextEditor) {
			LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor((ITextEditor) part, ServerCapabilities::isReferencesProvider);
			ISelection selection = ((ITextEditor) part).getSelectionProvider().getSelection();
			return info != null && !selection.isEmpty() && selection instanceof ITextSelection;
		}
		return false;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

}
