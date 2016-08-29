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
package org.eclipse.languageserver.operations.format;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.DocumentFormattingParams;
import io.typefox.lsapi.TextEdit;
import io.typefox.lsapi.builders.DocumentFormattingParamsBuilder;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSPFormatHandler extends AbstractHandler implements IHandler {

	private IDocument document;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part instanceof AbstractTextEditor) {
			IEditorInput input = part.getEditorInput();
			LanguageClientEndpoint languageClient = null;
			URI fileUri = null;
			try {
				document = null;
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
						DocumentFormattingParams params = new DocumentFormattingParamsBuilder()
								.textDocument(fileUri.toString())
								.build();
					    CompletableFuture<List<? extends TextEdit>> rename = languageClient.getTextDocumentService().formatting(params);
					    rename.thenAccept(new Consumer<List<? extends TextEdit>>() {
							@Override
							public void accept(List<? extends TextEdit> t) {
								for (TextEdit textEdit : t) {
									try {
										document.replace(
												LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
												LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
												textEdit.getNewText());
									} catch (BadLocationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						});
					}
				}
			} catch (Exception ex) {
				
			}
		}
		return null;
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
					languageClient = LanguageServiceAccessor.getLanguageServer(file, document);
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
