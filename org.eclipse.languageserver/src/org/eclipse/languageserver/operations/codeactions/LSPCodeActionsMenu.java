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
package org.eclipse.languageserver.operations.codeactions;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.languageserver.LSPDocumentInfo;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditor;

import io.typefox.lsapi.CodeActionContext;
import io.typefox.lsapi.CodeActionParams;
import io.typefox.lsapi.Command;
import io.typefox.lsapi.Diagnostic;
import io.typefox.lsapi.Range;
import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.builders.CodeActionContextBuilder;
import io.typefox.lsapi.builders.CodeActionParamsBuilder;
import io.typefox.lsapi.builders.RangeBuilder;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSPCodeActionsMenu extends ContributionItem implements IWorkbenchContribution {

	private LSPDocumentInfo info;
	private Range range;

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor) {
			info = LanguageServiceAccessor.getLSPDocumentInfoFor((ITextEditor) editor, ServerCapabilities::isCodeActionProvider);
			ITextSelection selection = (ITextSelection) ((ITextEditor) editor).getSelectionProvider().getSelection();
			try {
				this.range = new RangeBuilder()
						.start(LSPEclipseUtils.toPosition(selection.getOffset(), info.getDocument()))
						.end(LSPEclipseUtils.toPosition(selection.getOffset() + selection.getLength(), info.getDocument()))
						.build();
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void fill(final Menu menu, int index) {
		final MenuItem item = new MenuItem(menu, SWT.NONE, index);
		item.setText("Computing...");
		item.setEnabled(false);
		CodeActionContext context = new CodeActionContextBuilder()
				.diagnostic((Diagnostic)null)
				.build();
		CodeActionParams param = new CodeActionParamsBuilder()
				.textDocument(info.getFileUri().toString())
				.range(this.range)
				.context(context)
				.build();
		final CompletableFuture<List<? extends Command>> codeActions = info.getTextDocumentService().codeAction(param);
		codeActions.whenComplete(new BiConsumer<List<? extends Command>, Throwable>() {

			@Override
			public void accept(List<? extends Command> t, Throwable u) {
				UIJob job = new UIJob(menu.getDisplay(), "Update codelens menu") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (u != null) {
							// log?
							item.setText(u.getMessage());
						} else {
							for (Command command : t) {
								final MenuItem item = new MenuItem(menu, SWT.NONE, index);
								item.setText(command.getTitle());
								item.setEnabled(false);
							}
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}

		});
		super.fill(menu, index);
	}

}
