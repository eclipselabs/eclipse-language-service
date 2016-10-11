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
package org.eclipse.languageserver.operations.declaration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.languageserver.ui.Messages;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.Location;
import io.typefox.lsapi.ServerCapabilities;

public class OpenDeclarationHyperlinkDetector extends AbstractHyperlinkDetector {

	public class LSBasedHyperlink implements IHyperlink {

		private Location location;
		private URI fileUri;
		private IRegion region;

		public LSBasedHyperlink(Location response, URI fileUri, IRegion region) {
			this.location = response;
			this.fileUri = fileUri;
			this.region = region;
		}

		@Override
		public IRegion getHyperlinkRegion() {
			return this.region;
		}

		@Override
		public String getTypeLabel() {
			return Messages.hyperlinkLabel;
		}

		@Override
		public String getHyperlinkText() {
			return Messages.hyperlinkLabel;
		}

		@Override
		public void open() {
			IEditorPart part = null;
			IDocument targetDocument = null;
			IResource targetResource = LSPEclipseUtils.findResourceFor(this.location.getUri());
			try {
				if (targetResource != null && targetResource.getType() == IResource.FILE) {
					part = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile)targetResource);
					targetDocument = FileBuffers.getTextFileBufferManager().getTextFileBuffer(targetResource.getFullPath(), LocationKind.IFILE).getDocument();
				} else {
					part = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), fileUri, null, true);
					targetDocument = FileBuffers.getTextFileBufferManager().getTextFileBuffer(new Path(fileUri.getPath()), LocationKind.LOCATION).getDocument();
				}
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (part instanceof AbstractTextEditor) {
					AbstractTextEditor editor = (AbstractTextEditor) part;
					int offset = LSPEclipseUtils.toOffset(location.getRange().getStart(), targetDocument);
					int endOffset = LSPEclipseUtils.toOffset(location.getRange().getEnd(), targetDocument);
					editor.getSelectionProvider().setSelection(new TextSelection(offset, endOffset > offset ? endOffset - offset : 0));
				}
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		final LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor(textViewer, ServerCapabilities::isDefinitionProvider);
		if (info != null) {
			try {
				CompletableFuture<List<? extends Location>> documentHighlight = info.getLanguageClient().getTextDocumentService()
						.definition(LSPEclipseUtils.toTextDocumentPosistionParams(info.getFileUri(), region.getOffset(), info.getDocument()));
				List<? extends Location> response = documentHighlight.get(2, TimeUnit.SECONDS);
				if (response.isEmpty()) {
					return null;
				}
				List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>(response.size());
				for (Location responseLocation : response) {
					hyperlinks.add(new LSBasedHyperlink(responseLocation, info.getFileUri(), region));
				}
				return hyperlinks.toArray(new IHyperlink[hyperlinks.size()]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

}
