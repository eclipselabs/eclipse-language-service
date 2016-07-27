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
package org.eclipse.languageserver;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.Location;
import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

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
			IEditorInput input = null; 
			IResource targetResource = LanguageServerEclipseUtils.findResourceFor(this.location.getUri());
			if (targetResource != null && targetResource.getType() == IResource.FILE) {
				input = new FileEditorInput((IFile)targetResource);
			} else {
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileUri);
				if (fileStore != null) {
					input = new FileStoreEditorInput(fileStore);
				}
			}
			if (input == null) {
				return;
			}
			try {
				IEditorPart part = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), input, null);
				if (part instanceof AbstractTextEditor) {
					AbstractTextEditor editor = (AbstractTextEditor) part;
					IDocument targetDocument = editor.getAdapter(IDocument.class);
					int offset = LanguageServerEclipseUtils.toOffset(location.getRange().getStart(), targetDocument);
					int endOffset = LanguageServerEclipseUtils.toOffset(location.getRange().getEnd(), targetDocument);
					editor.getSelectionProvider().setSelection(new TextSelection(offset, endOffset - offset));
				}
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		IPath location = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument()).getLocation();
		IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
		JsonBasedLanguageServer server = null;
		URI fileUri = null;
		try {
			if (iFile.exists()) {
				server = LanaguageServiceAccessor.getLanaguageServer(iFile, textViewer.getDocument());
				fileUri = iFile.getLocationURI();
			} else {
				fileUri = location.toFile().toURI();
			}
			if (server != null) {
				CompletableFuture<List<? extends Location>> documentHighlight = server.getTextDocumentService().definition(LanguageServerEclipseUtils.toTextDocumentPosistionParams(fileUri, region.getOffset(), textViewer.getDocument()));
				List<? extends Location> response = documentHighlight.get();
				List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>(response.size());
				if (hyperlinks.isEmpty()) {
					return null;
				}
				for (Location responseLocation : response) {
					hyperlinks.add(new LSBasedHyperlink(responseLocation, fileUri, region));
				}
				return hyperlinks.toArray(new IHyperlink[hyperlinks.size()]);
			}
		} catch (Exception ex) {
			ex.printStackTrace(); // TODO
		}
		return null;
	}

}
