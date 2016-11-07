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
package org.eclipse.languageserver;

import java.net.URI;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.DiagnosticSeverity;
import io.typefox.lsapi.Location;
import io.typefox.lsapi.Position;
import io.typefox.lsapi.TextEdit;
import io.typefox.lsapi.impl.PositionImpl;
import io.typefox.lsapi.impl.TextDocumentIdentifierImpl;
import io.typefox.lsapi.impl.TextDocumentPositionParamsImpl;

/**
 * Some utility methods to convert between Eclipse and LS-API types
 */
public class LSPEclipseUtils {

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
		default:
			return IMarker.SEVERITY_INFO;
		}

	}
	
//	public static int toEclipseMarkerSeverity(int lspSeverity) {
//		switch (lspSeverity) {
//		case 1: return IMarker.SEVERITY_ERROR;
//		case 2: return IMarker.SEVERITY_WARNING;
//		}
//		return IMarker.SEVERITY_INFO;
//	}
	
	public static IResource findResourceFor(String uri) {
		uri = uri.replace("file:///", "file:/");  //$NON-NLS-1$//$NON-NLS-2$
		uri = uri.replace("file://", "file:/");  //$NON-NLS-1$//$NON-NLS-2$
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
	
	public static void applyEdit(TextEdit textEdit, IDocument document) throws BadLocationException {
		document.replace(
				LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
				LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
				textEdit.getNewText());
	}

	/**
	 * Method will apply all edits to document as single modification. Needs to
	 * be executed in UI thread.
	 * 
	 * @param document
	 *            document to modify
	 * @param edits
	 *            list of LSP TextEdits
	 */
	public static void applyEdits(IDocument document, List<? extends TextEdit> edits) {
		if (document == null || edits.isEmpty()) {
			return;
		}

		IDocumentUndoManager manager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
		if (manager != null) {
			manager.beginCompoundChange();
		}

		MultiTextEdit edit = new MultiTextEdit();
		for (TextEdit textEdit : edits) {
			try {
				int offset = LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document);
				int length = LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - offset;
				edit.addChild(new ReplaceEdit(offset, length, textEdit.getNewText()));
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			edit.apply(document);
		} catch (MalformedTreeException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (manager != null) {
			manager.endCompoundChange();
		}
	}

	public static IDocument getDocument(IResource resource) {
		if (resource == null) {
			return null;
		}

		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		IDocument document = null;
		ITextFileBuffer buffer = bufferManager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
		if (buffer != null) {
			document = buffer.getDocument();
		} else if (resource.getType() == IResource.FILE) {
			try {
				bufferManager.connect(resource.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return document;
			}
			buffer = bufferManager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
			if (buffer != null) {
				document = buffer.getDocument();
			}
		}
		return document;
	}

	public static void openInEditor(Location location, IWorkbenchPage page) {
		IEditorPart part = null;
		IDocument targetDocument = null;
		IResource targetResource = LSPEclipseUtils.findResourceFor(location.getUri());
		try {
			if (targetResource != null && targetResource.getType() == IResource.FILE) {
				part = IDE.openEditor(page, (IFile) targetResource);
				targetDocument = FileBuffers.getTextFileBufferManager()
				        .getTextFileBuffer(targetResource.getFullPath(), LocationKind.IFILE).getDocument();
			} else {
				URI fileUri = URI.create(location.getUri());
				part = IDE.openEditor(page, fileUri, null, true);
				targetDocument = FileBuffers.getTextFileBufferManager()
				        .getTextFileBuffer(new Path(fileUri.getPath()), LocationKind.LOCATION).getDocument();
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
				editor.getSelectionProvider()
				        .setSelection(new TextSelection(offset, endOffset > offset ? endOffset - offset : 0));
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
