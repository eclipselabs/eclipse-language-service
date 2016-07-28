/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

import io.typefox.lsapi.Location;

public class LSSearchResult extends FileSearchResult {

	private IDocument document;
	private ISearchQuery query;
	private CompletableFuture<List<? extends Location>> references;

	public LSSearchResult(CompletableFuture<List<? extends Location>> references, IDocument document) {
		super(null);
		this.document = document;
		this.references = references;
	}

	protected Match toMatch(Location loc) {
		try {
			int startOffset = LanguageServerEclipseUtils.toOffset(loc.getRange().getStart(), document);
			int endOffset = LanguageServerEclipseUtils.toOffset(loc.getRange().getEnd(), document);
			IResource resource = LanguageServerEclipseUtils.findResourceFor(loc.getUri());
			if (resource != null) {
				IRegion lineInformation = document.getLineInformationOfOffset(startOffset);
				LineElement lineEntry = new LineElement(resource, document.getLineOfOffset(startOffset), lineInformation.getOffset(), document.get(lineInformation.getOffset(), lineInformation.getLength()));
				return new FileMatch((IFile)resource, startOffset, endOffset - startOffset, lineEntry);
			} else {
				IFileStore store = EFS.getStore(new URI(loc.getUri()));
				return new Match(store, startOffset, endOffset - startOffset);
			}
		} catch (BadLocationException | CoreException | URISyntaxException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public String getLabel() {
		return "References TODO Label";
	}

	@Override
	public String getTooltip() {
		return "References TODO Tooltip";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISearchQuery getQuery() {
		if (this.query == null) {
			this.query = new FileSearchQuery("reference", false, false, null) {
				@Override
				public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
					try {
						for (Location loc : references.get()) {
							Match match = toMatch(loc);
							addMatch(match);
						}
						return Status.OK_STATUS;
					} catch (Exception ex) {
						return new Status(IStatus.ERROR, "TODO", "TODO"); // TODO
					}
				}
				
				@Override
				public ISearchResult getSearchResult() {
					return LSSearchResult.this;
				}
			};
		}
		return this.query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		// TODO Auto-generated method stub
		return null;
	}

}
