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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.languageserver.ui.Messages;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditor;

public class LSPCodeActionsMenu extends ContributionItem implements IWorkbenchContribution {

	private LSPDocumentInfo info;
	private Range range;

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor) {
			info = LanguageServiceAccessor.getLSPDocumentInfoFor((ITextEditor) editor, (capabilities) -> Boolean.TRUE.equals(capabilities.getCodeActionProvider()));
			ITextSelection selection = (ITextSelection) ((ITextEditor) editor).getSelectionProvider().getSelection();
			try {
				this.range = new Range(
						LSPEclipseUtils.toPosition(selection.getOffset(), info.getDocument()),
						LSPEclipseUtils.toPosition(selection.getOffset() + selection.getLength(), info.getDocument()));
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void fill(final Menu menu, int index) {
		final MenuItem item = new MenuItem(menu, SWT.NONE, index);
		item.setText(Messages.computing);
		item.setEnabled(false);
		CodeActionContext context = new CodeActionContext(Collections.emptyList());
		CodeActionParams params = new CodeActionParams();
		params.setTextDocument(new TextDocumentIdentifier(info.getFileUri().toString()));
		params.setRange(this.range);
		params.setContext(context);
		final CompletableFuture<List<? extends Command>> codeActions = info.getLanguageClient().getTextDocumentService().codeAction(params);
		codeActions.whenComplete(new BiConsumer<List<? extends Command>, Throwable>() {

			@Override
			public void accept(List<? extends Command> t, Throwable u) {
				UIJob job = new UIJob(menu.getDisplay(), Messages.updateCodeActions_menu) {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (u != null) {
							// log?
							item.setText(u.getMessage());
						} else {
							for (Command command : t) {
								if (command != null) {
									final MenuItem item = new MenuItem(menu, SWT.NONE, index);
									item.setText(command.getTitle());
									item.setEnabled(false);
								}
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
