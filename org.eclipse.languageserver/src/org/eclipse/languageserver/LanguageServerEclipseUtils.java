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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import io.typefox.lsapi.DiagnosticSeverity;
import io.typefox.lsapi.Position;
import io.typefox.lsapi.impl.PositionImpl;
import io.typefox.lsapi.impl.TextDocumentIdentifierImpl;
import io.typefox.lsapi.impl.TextDocumentPositionParamsImpl;

public class LanguageServerEclipseUtils {

	public static PositionImpl toPosition(int offset, IDocument document) throws BadLocationException {
		PositionImpl res = new PositionImpl();
		res.setLine(document.getLineOfOffset(offset));
		res.setCharacter(offset - document.getLineInformationOfOffset(offset).getOffset());
		return res;
	}

	public static int toOffset(Position position, IDocument document) throws BadLocationException {
		return document.getLineInformation(position.getLine()).getOffset() + position.getCharacter();
	}

	public static TextDocumentPositionParamsImpl toTextDocumentPosistionParams(URI fileUri, int offset, IDocument document)
			throws BadLocationException {
		PositionImpl start = toPosition(offset, document);
		TextDocumentPositionParamsImpl param = new TextDocumentPositionParamsImpl();
		param.setPosition(start);
		param.setUri(fileUri.toString());
		TextDocumentIdentifierImpl id = new TextDocumentIdentifierImpl();
		id.setUri(fileUri.toString());
		param.setTextDocument(id);
		return param;
	}

	public static int toEclipseMarkerSeverity(DiagnosticSeverity lspSeverity) {
		switch (lspSeverity) {
		case Error: return IMarker.SEVERITY_ERROR;
		case Warning: return IMarker.SEVERITY_WARNING;
		}
		return IMarker.SEVERITY_INFO;
	}
	
//	public static int toEclipseMarkerSeverity(int lspSeverity) {
//		switch (lspSeverity) {
//		case 1: return IMarker.SEVERITY_ERROR;
//		case 2: return IMarker.SEVERITY_WARNING;
//		}
//		return IMarker.SEVERITY_INFO;
//	}
	
	public static IResource findResourceFor(String uri) {
		uri = uri.replace("file:///", "file:/");
		uri = uri.replace("file://", "file:/");
		IProject project = null;
		for (IProject aProject : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (uri.startsWith(aProject.getLocationURI().toString()) && (project == null || project.getLocation().segmentCount() < aProject.getLocation().segmentCount())) {
				project = aProject;
			}
		}
		if (project == null) {
			return null;
		}
		IResource resource = project.getFile(new Path(uri.substring(project.getLocationURI().toString().length())));
		if (!resource.exists()) {
			//resource.refresh ?
		}
		return resource;
	}

}
