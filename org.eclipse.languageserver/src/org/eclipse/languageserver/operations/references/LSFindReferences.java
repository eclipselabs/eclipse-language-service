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

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.languageserver.LanguageServerEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.search2.internal.ui.SearchView;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.Location;
import io.typefox.lsapi.impl.ReferenceContextImpl;
import io.typefox.lsapi.impl.ReferenceParamsImpl;
import io.typefox.lsapi.impl.TextDocumentIdentifierImpl;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSFindReferences extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		SearchView searchView = null;
		try {
			searchView = (SearchView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().showView("org.eclipse.search.ui.views.SearchView");
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (part instanceof AbstractTextEditor) {
			IEditorInput input = part.getEditorInput();
			LanguageClientEndpoint languageClient = null;
			URI fileUri = null;
			try {
				IDocument document = null;
				if (input instanceof IFileEditorInput) { // TODO, also support non resource file
					IFile file = ((IFileEditorInput) input).getFile();
					fileUri = file.getLocation().toFile().toURI();
					document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(file.getFullPath(),	LocationKind.IFILE).getDocument();
					languageClient = LanguageServiceAccessor.getLanguageServer(file, document);
				} else if (input instanceof IURIEditorInput) {
					fileUri = ((IURIEditorInput)input).getURI();
					document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(new Path(fileUri.getPath()), LocationKind.LOCATION).getDocument();
					// TODO server
				}
		
				if (languageClient != null) {
					ISelection sel = ((AbstractTextEditor) part).getSelectionProvider().getSelection();
					if (sel instanceof TextSelection) {
					    ReferenceParamsImpl params = new ReferenceParamsImpl();
					    params.setPosition(LanguageServerEclipseUtils.toPosition(((TextSelection) sel).getOffset(), document));
                        TextDocumentIdentifierImpl identifier = new TextDocumentIdentifierImpl();
					    identifier.setUri(fileUri.toString());
					    params.setTextDocument(identifier);
					    ReferenceContextImpl context = new ReferenceContextImpl();
					    context.setIncludeDeclaration(true);
					    params.setContext(context);
					    CompletableFuture<List<? extends Location>> references = languageClient.getTextDocumentService().references(params);
					    LSSearchResult search = new LSSearchResult(references, document);
						search.getQuery().run(new NullProgressMonitor());
						searchView.showSearchResult(search);

					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public boolean isHandled() {
		return true;
	}

}
