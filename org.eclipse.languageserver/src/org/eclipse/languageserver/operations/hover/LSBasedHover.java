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
package org.eclipse.languageserver.operations.hover;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.LanguageServerEclipseUtils;

import io.typefox.lsapi.Hover;
import io.typefox.lsapi.MarkedString;
import io.typefox.lsapi.Range;
import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

public class LSBasedHover implements ITextHover {

	public LSBasedHover() {
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IPath location = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument()).getLocation();
		IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
		JsonBasedLanguageServer server = null;
		URI fileUri = null;
		try {
			if (iFile.exists()) {
				server = LanguageServiceAccessor.getLanguageServer(iFile, textViewer.getDocument());
				fileUri = iFile.getLocationURI();
			} else {
				fileUri = location.toFile().toURI();
			}
			if (server != null) {
				CompletableFuture<Hover> documentHighlight = server.getTextDocumentService().hover(LanguageServerEclipseUtils.toTextDocumentPosistionParams(fileUri, hoverRegion.getOffset(), textViewer.getDocument()));
				StringBuilder res = new StringBuilder();
				for (MarkedString string : documentHighlight.get().getContents()) {
					res.append(string.getValue());
					res.append('\n');
				}
				return res.toString();
			}
		} catch (Exception ex) {
			ex.printStackTrace(); // TODO
		}
		return null;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		// TODO: factorize!
		IPath location = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument()).getLocation();
		IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
		JsonBasedLanguageServer server = null;
		URI fileUri = null;
		try {
			if (iFile.exists()) {
				server = LanguageServiceAccessor.getLanguageServer(iFile, textViewer.getDocument());
				fileUri = iFile.getLocationURI();
			} else {
				fileUri = location.toFile().toURI();
			}
			if (server != null) {
				CompletableFuture<Hover> hover = server.getTextDocumentService().hover(LanguageServerEclipseUtils.toTextDocumentPosistionParams(fileUri, offset, textViewer.getDocument()));
				Range range = hover.get(400, TimeUnit.MILLISECONDS).getRange();
				int rangeOffset = LanguageServerEclipseUtils.toOffset(range.getStart(), textViewer.getDocument());
				Region res = new Region(rangeOffset, LanguageServerEclipseUtils.toOffset(range.getEnd(), textViewer.getDocument()) - rangeOffset);
			}
		} catch (Exception ex) {
			ex.printStackTrace(); // TODO
		}
		return null;
	}

}
