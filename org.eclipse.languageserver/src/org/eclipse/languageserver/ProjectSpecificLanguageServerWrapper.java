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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.languageserver.operations.diagnostics.LSPDiagnosticsToMarkers;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.progress.ProgressMonitorFocusJobDialog;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;

import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.Message;
import io.typefox.lsapi.impl.ClientCapabilitiesImpl;
import io.typefox.lsapi.impl.DidChangeTextDocumentParamsImpl;
import io.typefox.lsapi.impl.DidOpenTextDocumentParamsImpl;
import io.typefox.lsapi.impl.InitializeParamsImpl;
import io.typefox.lsapi.impl.TextDocumentContentChangeEventImpl;
import io.typefox.lsapi.impl.TextDocumentItemImpl;
import io.typefox.lsapi.impl.VersionedTextDocumentIdentifierImpl;
import io.typefox.lsapi.services.json.MessageJsonHandler;
import io.typefox.lsapi.services.json.StreamMessageReader;
import io.typefox.lsapi.services.json.StreamMessageWriter;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;
import io.typefox.lsapi.services.transport.io.ConcurrentMessageReader;
import io.typefox.lsapi.services.transport.io.MessageReader;
import io.typefox.lsapi.services.transport.io.MessageWriter;

/**
 * Wraps instantiation, initialization of project-specific instance of the
 * language server
 */
public class ProjectSpecificLanguageServerWrapper {

	private final class DocumentChangeListenenr implements IDocumentListener {
		private URI fileURI;
		private int version = 2;
		private DidChangeTextDocumentParamsImpl change;

		public DocumentChangeListenenr(URI fileURI) {
			this.fileURI = fileURI;
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			this.change.getContentChanges().get(0).setText(event.getDocument().get());
			languageClient.getTextDocumentService().didChange(this.change);
			version++;
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			this.change = new DidChangeTextDocumentParamsImpl();
			VersionedTextDocumentIdentifierImpl doc = new VersionedTextDocumentIdentifierImpl();
			doc.setUri(fileURI.toString());
			doc.setVersion(version);
			this.change.setTextDocument(doc);
			TextDocumentContentChangeEventImpl changeEvent = new TextDocumentContentChangeEventImpl();
			changeEvent.setText(event.getDocument().get()); // TODO set to value after change
			this.change.setContentChanges(Arrays.asList(new TextDocumentContentChangeEventImpl[] { changeEvent }));
		}
	}

	final private StreamConnectionProvider lspStreamProvider;
	private LanguageClientEndpoint languageClient;
	IProject project;
	private Map<IPath, DocumentChangeListenenr> connectedFiles;
	private Map<IPath, IDocument> documents;

	private Job initializeJob;
	
	public ProjectSpecificLanguageServerWrapper(IProject project, StreamConnectionProvider connection) {
		this.project = project;
		this.lspStreamProvider = connection;
		this.connectedFiles = new HashMap<>();
		this.documents = new HashMap<>();
	}

	private void start() throws IOException {
		if (this.languageClient != null) {
			if (stillActive()) {
				return;
			} else {
				stop();
			}
		}
		try {
			ExecutorService executorService = Executors.newCachedThreadPool();
			this.languageClient = new LanguageClientEndpoint(executorService);
			this.lspStreamProvider.start();
			MessageJsonHandler jsonHandler = new MessageJsonHandler();
			jsonHandler.setMethodResolver(this.languageClient);
			MessageReader reader = new ConcurrentMessageReader(new StreamMessageReader(this.lspStreamProvider.getInputStream(), jsonHandler), executorService);
			MessageWriter writer = new StreamMessageWriter(this.lspStreamProvider.getOutputStream(), jsonHandler);
			reader.setOnError(new Consumer<Throwable>() {
				@Override
				public void accept(Throwable t) {
					System.err.println("Logged error: ");
					t.printStackTrace(System.err);
					// most likely an issue that requires a restart
					stop();
				}
			});
			reader.setOnRead(new Procedure2<Message, String>() {
				@Override
				public void apply(Message p1, String p2) {
					System.err.println("IN: " + p1.getJsonrpc() + "\n" + p2);
				}
			});
			writer.setOnWrite(new Procedure2<Message, String>() {
				@Override
				public void apply(Message p1, String p2) {
					System.err.println("OUT: " + p1.getJsonrpc() + "\n" + p2);
				}
			});
			writer.setOnError(new Consumer<Throwable>() {
				@Override
				public void accept(Throwable t) {
					System.err.println("Logged error: ");
					t.printStackTrace(System.err);
					// most likely an issue that requires a restart
					stop();
				}
			});
			languageClient.connect(reader, writer);
			this.initializeJob = new Job("Initialize language server") {
				protected IStatus run(IProgressMonitor monitor) {
					InitializeParamsImpl initParams = new InitializeParamsImpl();
					initParams.setRootPath(project.getLocation().toFile().getAbsolutePath());
					String name = "Eclipse IDE";
					if (Platform.getProduct() != null) {
						name = Platform.getProduct().getName();
					}
					initParams.setClientName(name);
					initParams.setCapabilities(new ClientCapabilitiesImpl());
					connectDiagnostics();
					CompletableFuture<InitializeResult> result = languageClient.initialize(initParams);
					try {
						InitializeResult initializeResult = result.get();
					} catch (InterruptedException | ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return Status.OK_STATUS;
				}
			};
			this.initializeJob.setUser(true);
			this.initializeJob.setSystem(false);
			this.initializeJob.schedule();
		} catch (Exception ex) {
			ex.printStackTrace();
			stop();
		}
	}
	
	private boolean stillActive() {
		if (this.languageClient == null || this.languageClient.getReader() == null) {
			return false;
		}
		return ((ConcurrentMessageReader)this.languageClient.getReader()).isRunning();
	}

	private void connectDiagnostics() {
		this.languageClient.getTextDocumentService().onPublishDiagnostics(new LSPDiagnosticsToMarkers(this.project));
	}

	private void stop() {
		if (this.languageClient != null) {
			this.languageClient.shutdown();
			if (this.languageClient.getReader() != null) {
				this.languageClient.getReader().close();
			}
			if (this.languageClient.getWriter() != null) {
				this.languageClient.getWriter().close();
			}
		}
		if (this.lspStreamProvider != null) {
			this.lspStreamProvider.stop();
		}
		while (!this.documents.isEmpty()) {
			disconnect(this.documents.keySet().iterator().next());
		}
		this.languageClient = null;
	}

	public void connect(IFile file, final IDocument document) throws IOException {
		start();
		if (this.connectedFiles.containsKey(file.getLocation())) {
			return;
		}
		// add a document buffer
		DidOpenTextDocumentParamsImpl open = new DidOpenTextDocumentParamsImpl();
		TextDocumentItemImpl textDocument = new TextDocumentItemImpl();
		textDocument.setUri(file.getLocationURI().toString());
		textDocument.setText(document.get());
		textDocument.setLanguageId(file.getFileExtension());
		open.setTextDocument(textDocument);
		this.languageClient.getTextDocumentService().didOpen(open);
		
		DocumentChangeListenenr listener = new DocumentChangeListenenr(file.getLocationURI());
		document.addDocumentListener(listener);
		this.connectedFiles.put(file.getLocation(), listener);
		this.documents.put(file.getLocation(), document);
	}
	
	public void disconnect(IPath path) {
		this.documents.get(path).removeDocumentListener(this.connectedFiles.get(path));
		this.connectedFiles.remove(path);
		this.documents.remove(path);
		if (this.connectedFiles.isEmpty()) {
			stop();
		}
	}

	public LanguageClientEndpoint getServer() {
		if (this.initializeJob.getState() != Job.NONE) {
			if (Display.getCurrent() != null) { // UI Thread
				ProgressMonitorFocusJobDialog dialog = new ProgressMonitorFocusJobDialog(null);
				dialog.setBlockOnOpen(true);
				dialog.show(this.initializeJob, null);
			}
			try {
				this.initializeJob.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return languageClient;
	}
}
