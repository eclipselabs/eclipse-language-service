/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - initial implementation
 *  Michał Niewrzał (Rogue Wave Software Inc.)
 *******************************************************************************/
package org.eclipse.languageserver.operations.format;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import io.typefox.lsapi.DocumentFormattingParams;
import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.TextEdit;
import io.typefox.lsapi.builders.DocumentFormattingParamsBuilder;
import io.typefox.lsapi.builders.FormattingOptionsBuilder;

public class LSPFormatHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part instanceof ITextEditor) {
			LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor((ITextEditor) part, (capabilities) -> Boolean.TRUE.equals(capabilities.isDocumentFormattingProvider()));
			if (info != null) {
				ISelection sel = ((AbstractTextEditor) part).getSelectionProvider().getSelection();
				if (sel instanceof TextSelection) {
					final Shell shell = HandlerUtil.getActiveShell(event);
					DocumentFormattingParams params = new DocumentFormattingParamsBuilder()
					        .textDocument(info.getFileUri().toString()).options(new FormattingOptionsBuilder().build())
					        .build();
					CompletableFuture<List<? extends TextEdit>> formatter = info.getLanguageClient().getTextDocumentService().formatting(params);
					formatter.thenAccept((List<? extends TextEdit> t) -> {
						shell.getDisplay().asyncExec(() -> {
							LSPEclipseUtils.applyEdits(info.getDocument(), t);
						});
					});
				}
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (part instanceof ITextEditor) {
			LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor((ITextEditor) part,
			        (capabilities) -> Boolean.TRUE.equals(capabilities.isDocumentFormattingProvider()));
			ISelection selection = ((ITextEditor) part).getSelectionProvider().getSelection();
			return info != null && !selection.isEmpty() && selection instanceof ITextSelection;
		}
		return false;
	}

}
