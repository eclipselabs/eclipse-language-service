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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.languageserver.LanguageServiceAccessor;

import io.typefox.lsapi.Hover;
import io.typefox.lsapi.MarkedString;
import io.typefox.lsapi.Range;
import io.typefox.lsapi.ServerCapabilities;

public class LSBasedHover implements ITextHover {

	private CompletableFuture<Hover> hover;
	private IRegion lastRegion;
	private ITextViewer textViewer;

	public LSBasedHover() {
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (hoverRegion.equals(this.lastRegion) && textViewer.equals(this.textViewer) && this.hover != null) {
			StringBuilder res = new StringBuilder();
			try {
				for (MarkedString string : this.hover.get(500, TimeUnit.MILLISECONDS).getContents()) {
					res.append(string.getValue());
					res.append('\n');
				}
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
			return res.toString();
		}
		return null;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		IRegion res = new Region(offset, 0);
		final LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor(textViewer, ServerCapabilities::isHoverProvider);
		if (info != null) {
			try {
				hover = info.getTextDocumentService().hover(LSPEclipseUtils.toTextDocumentPosistionParams(info.getFileUri(), offset, textViewer.getDocument()));
				Range range = hover.get(800, TimeUnit.MILLISECONDS).getRange();
				if (range != null) {
					int rangeOffset = LSPEclipseUtils.toOffset(range.getStart(), textViewer.getDocument());
					res = new Region(rangeOffset, LSPEclipseUtils.toOffset(range.getEnd(), textViewer.getDocument()) - rangeOffset);
				}
			} catch (Exception e) {
				e.printStackTrace();
				res = new Region(offset, 1);
			}
		} else {
			res = new Region(offset, 1);
		}

		this.lastRegion = res;
		this.textViewer = textViewer;
		return res;
	}

}
