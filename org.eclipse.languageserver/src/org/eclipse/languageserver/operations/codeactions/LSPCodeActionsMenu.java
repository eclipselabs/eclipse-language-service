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

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
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
import io.typefox.lsapi.builders.CodeActionContextBuilder;
import io.typefox.lsapi.builders.CodeActionParamsBuilder;
import io.typefox.lsapi.builders.RangeBuilder;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSPCodeActionsMenu extends ContributionItem implements IWorkbenchContribution {

	private LanguageClientEndpoint languageClient;
	private URI fileUri;
	private Range range;

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor) {
			IEditorInput input = editor.getEditorInput();
			languageClient = null;
			fileUri = null;
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
				ITextSelection selection = (ITextSelection) ((ITextEditor) editor).getSelectionProvider().getSelection();
				this.range = new RangeBuilder()
						.start(LSPEclipseUtils.toPosition(selection.getOffset(), document))
						.end(LSPEclipseUtils.toPosition(selection.getOffset() + selection.getLength(), document))
						.build();
			} catch (Exception e) {
				// TODO: handle exception
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
				.textDocument(fileUri.toString())
				.range(this.range)
				.context(context)
				.build();
		final CompletableFuture<List<? extends Command>> codeActions = this.languageClient.getTextDocumentService().codeAction(param);
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
