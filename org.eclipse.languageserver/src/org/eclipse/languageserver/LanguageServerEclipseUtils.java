package org.eclipse.languageserver;

import java.net.URI;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import io.typefox.lsapi.Position;
import io.typefox.lsapi.PositionImpl;
import io.typefox.lsapi.TextDocumentIdentifierImpl;
import io.typefox.lsapi.TextDocumentPositionParamsImpl;

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

	public static int toEclipseMarkerSeverity(Integer lspSeverity) {
		switch (lspSeverity) {
		case 1: return IMarker.SEVERITY_ERROR;
		case 2: return IMarker.SEVERITY_WARNING;
		}
		return IMarker.SEVERITY_INFO;
	}

}
