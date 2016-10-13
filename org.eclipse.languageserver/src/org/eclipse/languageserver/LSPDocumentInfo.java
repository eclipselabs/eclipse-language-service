package org.eclipse.languageserver;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.text.IDocument;

import io.typefox.lsapi.services.TextDocumentService;
import io.typefox.lsapi.services.WindowService;
import io.typefox.lsapi.services.WorkspaceService;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSPDocumentInfo {
	@NonNull
	private final URI fileUri;
	private final IFile file;
	private final IDocument document;
	@NonNull
	private final LanguageClientEndpoint languageClient;

	LSPDocumentInfo(@NonNull URI fileUri, IFile file, IDocument document, @NonNull LanguageClientEndpoint languageClient) {
		this.fileUri = fileUri;
		this.file = file;
		this.document = document;
		this.languageClient = languageClient;
	}

	@NonNull
	public URI getFileUri() {
		return fileUri;
	}

	public IFile getFile() {
		return file;
	}

	public IDocument getDocument() {
		return document;
	}

	@NonNull
	public LanguageClientEndpoint getLanguageClient() {
		return languageClient;
	}

	public TextDocumentService getTextDocumentService() {
		return languageClient.getTextDocumentService();
	}

	public WindowService getWindowService() {
		return languageClient.getWindowService();
	}

	public WorkspaceService getWorkspaceService() {
		return languageClient.getWorkspaceService();
	}

}