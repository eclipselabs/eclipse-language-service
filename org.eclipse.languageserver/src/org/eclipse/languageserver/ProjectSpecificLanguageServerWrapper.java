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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.progress.ProgressMonitorFocusJobDialog;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;

import com.google.common.base.Objects;

import io.typefox.lsapi.Diagnostic;
import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.Message;
import io.typefox.lsapi.PublishDiagnosticsParams;
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

	protected static final String LS_DIAGNOSTIC_MARKER_TYPE = "org.eclipse.languageserver.diagnostic"; //$NON-NLS-1$

	final private StreamConnectionProvider lspStreamProvider;
	private LanguageClientEndpoint languageClient;
	private IProject project;
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
		this.languageClient.getTextDocumentService().onPublishDiagnostics(new Consumer<PublishDiagnosticsParams>() {
			@Override
			public void accept(PublishDiagnosticsParams diagnostics) {
				try {
					// fix issue with file:/// vs file:/
					String uri = diagnostics.getUri();
					IResource resource = LanguageServerEclipseUtils.findResourceFor(uri);
					if (resource == null || !resource.exists()) {
						resource = project;
					}
					Set<IMarker> remainingMarkers = new HashSet<>(Arrays.asList(resource.findMarkers(LS_DIAGNOSTIC_MARKER_TYPE, false, IResource.DEPTH_ONE)));
					for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
						IMarker associatedMarker = getExistingMarkerFor(resource, diagnostic, remainingMarkers);
						if (associatedMarker == null) {
							createMarkerForDiagnostic(resource, diagnostic);
						} else {
							remainingMarkers.remove(associatedMarker);
						}
					}
					for (IMarker marker : remainingMarkers) {
						marker.delete();
					}
				} catch (CoreException ex) {
					ex.printStackTrace(); // TODO
				}
			}

			private void createMarkerForDiagnostic(IResource resource, Diagnostic diagnostic) {
				try {
					IMarker marker = resource.createMarker(LS_DIAGNOSTIC_MARKER_TYPE);
					marker.setAttribute(IMarker.MESSAGE, diagnostic.getMessage());
					marker.setAttribute(IMarker.SEVERITY, LanguageServerEclipseUtils.toEclipseMarkerSeverity(diagnostic.getSeverity())); // TODO mapping Eclipse <-> LS severity
					if (resource.getType() == IResource.FILE) {
						IFile file = (IFile)resource;
						IDocument document = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE).getDocument();
						marker.setAttribute(IMarker.CHAR_START, LanguageServerEclipseUtils.toOffset(diagnostic.getRange().getStart(), document));
						marker.setAttribute(IMarker.CHAR_END, LanguageServerEclipseUtils.toOffset(diagnostic.getRange().getEnd(), document));
						marker.setAttribute(IMarker.LINE_NUMBER, diagnostic.getRange().getStart().getLine());
					}
				} catch (Exception ex) {
					ex.printStackTrace(); // TODO
				}
			}

			private IMarker getExistingMarkerFor(IResource resource, Diagnostic diagnostic, Set<IMarker> remainingMarkers) {
				ITextFileBuffer textFileBuffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
				if (textFileBuffer == null) {
					return null;
				}
				IDocument document = textFileBuffer.getDocument();
				for (IMarker marker : remainingMarkers) {
					int startOffset = marker.getAttribute(IMarker.CHAR_START, -1);
					int endOffset = marker.getAttribute(IMarker.CHAR_END, -1);
					try {
						if (marker.getResource().getProjectRelativePath().toString().equals(diagnostic.getSource()) 
								&& LanguageServerEclipseUtils.toOffset(diagnostic.getRange().getStart(), document) == startOffset + 1
								&& LanguageServerEclipseUtils.toOffset(diagnostic.getRange().getEnd(), document) == endOffset + 1
								&& Objects.equal(marker.getAttribute(IMarker.MESSAGE), diagnostic.getMessage())) {
							return marker;
						}
					} catch (Exception e) {
						e.printStackTrace(); // TODO
					}
				}
				return null;
			}
		});
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
