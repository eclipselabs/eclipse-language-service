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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.languageserver.operations.diagnostics.LSPDiagnosticsToMarkers;
import org.eclipse.languageserver.ui.Messages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.progress.ProgressMonitorFocusJobDialog;

import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.Message;
import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.TextDocumentSyncKind;
import io.typefox.lsapi.impl.ClientCapabilitiesImpl;
import io.typefox.lsapi.impl.DidChangeTextDocumentParamsImpl;
import io.typefox.lsapi.impl.DidOpenTextDocumentParamsImpl;
import io.typefox.lsapi.impl.InitializeParamsImpl;
import io.typefox.lsapi.impl.RangeImpl;
import io.typefox.lsapi.impl.TextDocumentContentChangeEventImpl;
import io.typefox.lsapi.impl.TextDocumentItemImpl;
import io.typefox.lsapi.impl.VersionedTextDocumentIdentifierImpl;
import io.typefox.lsapi.services.json.InvalidMessageException;
import io.typefox.lsapi.services.json.MessageJsonHandler;
import io.typefox.lsapi.services.json.StreamMessageReader;
import io.typefox.lsapi.services.json.StreamMessageWriter;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;
import io.typefox.lsapi.services.transport.io.ConcurrentMessageReader;
import io.typefox.lsapi.services.transport.io.MessageWriter;
import io.typefox.lsapi.services.transport.trace.MessageTracer;

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
			if (this.change == null) {
				return;
			}
			this.change.getContentChanges().get(0).setText(event.getDocument().get());
			languageClient.getTextDocumentService().didChange(this.change);
			version++;
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			// create change event according synch
			TextDocumentContentChangeEventImpl changeEvent = toChangeEvent(event);
			if (changeEvent == null) {
				return;
			}
			this.change = new DidChangeTextDocumentParamsImpl();
			VersionedTextDocumentIdentifierImpl doc = new VersionedTextDocumentIdentifierImpl();
			doc.setUri(fileURI.toString());
			doc.setVersion(version);
			this.change.setTextDocument(doc);
			this.change.setContentChanges(Arrays.asList(new TextDocumentContentChangeEventImpl[] { changeEvent }));
		}

		/**
		 * Convert Eclipse {@link DocumentEvent} to LS according
		 * {@link TextDocumentSyncKind}.
		 * {@link TextDocumentContentChangeEventImpl}.
		 * 
		 * @param event
		 *            Eclipse {@link DocumentEvent}
		 * @return the converted LS {@link TextDocumentContentChangeEventImpl}.
		 */
		private TextDocumentContentChangeEventImpl toChangeEvent(DocumentEvent event) {
			IDocument document = event.getDocument();
			TextDocumentContentChangeEventImpl changeEvent = null;
			TextDocumentSyncKind syncKind = getTextDocumentSyncKind();
			switch (syncKind) {
			case None:
				changeEvent = null;
				break;
			case Full:
				changeEvent = new TextDocumentContentChangeEventImpl();
				changeEvent.setText(document.get());
				break;
			case Incremental:
				changeEvent = new TextDocumentContentChangeEventImpl();
				String newText = event.getText();
				int offset = event.getOffset();
				int length = event.getLength();
				try {
					// try to convert the Eclipse start/end offset to LS range.
					RangeImpl range = new RangeImpl(LSPEclipseUtils.toPosition(offset, document),
					        LSPEclipseUtils.toPosition(offset + length, document));
					changeEvent.setRange(range);
					changeEvent.setText(newText);
					changeEvent.setRangeLength(length);
				} catch (BadLocationException e) {
					// error while conversion (should never occur)
					// set the full document text as changes.
					changeEvent.setText(document.get());
				}
				break;
			}
			return changeEvent;
		}

		/**
		 * Returns the text document sync kind capabilities of the server and
		 * {@link TextDocumentSyncKind#Full} otherwise.
		 * 
		 * @return the text document sync kind capabilities of the server and
		 *         {@link TextDocumentSyncKind#Full} otherwise.
		 */
		private TextDocumentSyncKind getTextDocumentSyncKind() {
			TextDocumentSyncKind syncKind = initializeResult != null
			        ? initializeResult.getCapabilities().getTextDocumentSync() : null;
			return syncKind != null ? syncKind : TextDocumentSyncKind.Full;
		}
	}

	final private StreamConnectionProvider lspStreamProvider;
	private LanguageClientEndpoint languageClient;
	private IProject project;
	private Map<IPath, DocumentChangeListenenr> connectedFiles;
	private Map<IPath, IDocument> documents;

	private Job initializeJob;
	private InitializeResult initializeResult;
	
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
			if (Boolean.getBoolean("ls.log")) { //$NON-NLS-1$
				this.languageClient.setMessageTracer(new MessageTracer() {
					@Override
					public void onWrite(Message message, String json) {
						if (json.contains("telemetry/event")) {  //$NON-NLS-1$
							return;
						}
						System.out.println("WRITE: "); //$NON-NLS-1$
						System.out.println(json);
						System.out.println(message);
						System.out.println("");  //$NON-NLS-1$
					}
					
					@Override
					public void onRead(Message message, String json) {
						if (json.contains("telemetry/event")) { //$NON-NLS-1$
							return;
						}
						System.out.println("READ: "); //$NON-NLS-1$
						System.out.println(json);
						System.out.println(message);
						System.out.println(""); //$NON-NLS-1$
					}
					
					@Override
					public void onError(String message, Throwable throwable) {
						System.err.println("ERR:"); //$NON-NLS-1$
						System.err.println("message: " + message); //$NON-NLS-1$
						System.err.println("ex: " ); //$NON-NLS-1$
						if (throwable != null) {
							throwable.printStackTrace(System.err);
						}
						if (throwable instanceof InvalidMessageException) {
							//System.err.println("json unavailable, see https://github.com/TypeFox/ls-api/issues/51");
							System.err.println("json: " + ((InvalidMessageException)throwable).getJson()); //$NON-NLS-1$
						}
					}
				});
			}
			this.lspStreamProvider.start();
			MessageJsonHandler jsonHandler = new MessageJsonHandler();
			jsonHandler.setMethodResolver(this.languageClient);
			StreamMessageReader baseMessageReader = new StreamMessageReader(this.lspStreamProvider.getInputStream(), jsonHandler);
			ConcurrentMessageReader multiThreadReader = new ConcurrentMessageReader(baseMessageReader, executorService);
			MessageWriter writer = new StreamMessageWriter(this.lspStreamProvider.getOutputStream(), jsonHandler);
			languageClient.connect(multiThreadReader, writer);
			this.initializeJob = new Job(Messages.initializeLanguageServer_job) {
				protected IStatus run(IProgressMonitor monitor) {
					InitializeParamsImpl initParams = new InitializeParamsImpl();
					initParams.setRootPath(project.getLocation().toFile().getAbsolutePath());
					String name = "Eclipse IDE"; //$NON-NLS-1$
					if (Platform.getProduct() != null) {
						name = Platform.getProduct().getName();
					}
					initParams.setClientName(name);
					initParams.setCapabilities(new ClientCapabilitiesImpl());
					connectDiagnostics();
					CompletableFuture<InitializeResult> result = languageClient.initialize(initParams);
					try {
						initializeResult = result.get();
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
		if (this.initializeJob != null) {
			this.initializeJob.cancel();
		}
		this.initializeJob = null;
		this.initializeResult = null;
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
		try {
			this.initializeJob.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

	public ServerCapabilities getServerCapabilities() {
		try {
			start();
			this.initializeJob.join(1000, new NullProgressMonitor());
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (this.initializeResult != null) {
			return this.initializeResult.getCapabilities();
		} else {
			return null;
		}
	}
}
