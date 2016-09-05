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
package org.eclipse.languageserver.operations.rename;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServerPluginActivator;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.handlers.SaveAllHandler;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.TextEdit;
import io.typefox.lsapi.WorkspaceEdit;
import io.typefox.lsapi.impl.RenameParamsImpl;
import io.typefox.lsapi.impl.TextDocumentIdentifierImpl;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSPRenameHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
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
					languageClient = LanguageServiceAccessor.getLanguageServer(file, document, ServerCapabilities::isRenameProvider);
				} else if (input instanceof IURIEditorInput) {
					fileUri = ((IURIEditorInput)input).getURI();
					document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(new Path(fileUri.getPath()), LocationKind.LOCATION).getDocument();
					// TODO server
				}
		
				if (languageClient != null) {
					ISelection sel = ((AbstractTextEditor) part).getSelectionProvider().getSelection();
					if (sel instanceof TextSelection) {
						RenameParamsImpl params = new RenameParamsImpl();
					    params.setPosition(LSPEclipseUtils.toPosition(((TextSelection) sel).getOffset(), document));
                        TextDocumentIdentifierImpl identifier = new TextDocumentIdentifierImpl();
					    identifier.setUri(fileUri.toString());
					    params.setTextDocument(identifier);
					    params.setNewName(askNewName());
					    CompletableFuture<WorkspaceEdit> rename = languageClient.getTextDocumentService().rename(params);
					    rename.thenAccept(new Consumer<WorkspaceEdit>() {
							@Override
							public void accept(WorkspaceEdit t) {
								apply(t);
							}
						});
					}
				}
			} catch (Exception ex) {
				
			}
		}
		return null;
	}

	private void apply(WorkspaceEdit workspaceEdit) {
		for (Entry<String, ? extends List<? extends TextEdit>> entry : workspaceEdit.getChanges().entrySet()) {
			IResource resource = LSPEclipseUtils.findResourceFor(entry.getKey());
			if (resource.getType() == IResource.FILE) {
				IFile file = (IFile)resource;
				// save all open modified editors?
			}
		}
		WorkspaceJob job = new WorkspaceJob("Rename") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (Entry<String, ? extends List<? extends TextEdit>> entry : workspaceEdit.getChanges().entrySet()) {
					IResource resource = LSPEclipseUtils.findResourceFor(entry.getKey());
					if (resource.getType() == IResource.FILE) {
						IFile file = (IFile)resource;
						IDocument document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE).getDocument();
						try {
							for (TextEdit textEdit : entry.getValue()) {
								document.replace(LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
										LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
										textEdit.getNewText());
							}
							file.setContents(new ByteArrayInputStream(document.get().getBytes(file.getCharset())), false, true, monitor);
						} catch (UnsupportedEncodingException | BadLocationException e) {
							return new Status(IStatus.ERROR, LanguageServerPluginActivator.getDefault().getBundle().getSymbolicName(), e.getMessage(), e);
						}
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	@Override
	public boolean isEnabled() {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (part instanceof AbstractTextEditor) {
			IEditorInput input = ((AbstractTextEditor)part).getEditorInput();
			LanguageClientEndpoint languageClient = null;
			URI fileUri = null;
			try {
				IDocument document = null;
				if (input instanceof IFileEditorInput) { // TODO, also support non resource file
					IFile file = ((IFileEditorInput) input).getFile();
					fileUri = file.getLocation().toFile().toURI();
					document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(file.getFullPath(),	LocationKind.IFILE).getDocument();
					languageClient = LanguageServiceAccessor.getLanguageServer(file, document, ServerCapabilities::isRenameProvider);
				} else if (input instanceof IURIEditorInput) {
					fileUri = ((IURIEditorInput)input).getURI();
					document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(new Path(fileUri.getPath()), LocationKind.LOCATION).getDocument();
					// TODO server
				}
				ISelection selection = ((AbstractTextEditor)part).getSelectionProvider().getSelection();
				return languageClient != null && !selection.isEmpty() && selection instanceof ITextSelection;
			} catch (Exception ex) {
				return false;
			}
		}
		return false;
	}

	private String askNewName() {
		return "blah";
	}

}
