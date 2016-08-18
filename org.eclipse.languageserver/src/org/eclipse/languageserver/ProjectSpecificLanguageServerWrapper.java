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
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
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
//			try {
				this.change = new DidChangeTextDocumentParamsImpl();
				VersionedTextDocumentIdentifierImpl doc = new VersionedTextDocumentIdentifierImpl();
				doc.setUri(fileURI.toString());
				doc.setVersion(version);
				this.change.setTextDocument(doc);
				TextDocumentContentChangeEventImpl changeEvent = new TextDocumentContentChangeEventImpl();
//				RangeImpl range = new RangeImpl();
//				PositionImpl start = LanguageServerEclipseUtils.toPosition(event.getOffset(), event.getDocument());
//				range.setStart(start);
//				PositionImpl end = LanguageServerEclipseUtils.toPosition(event.getOffset() + event.getLength(), event.getDocument());
//				range.setEnd(end);
//				changeEvent.setRange(range);
//				changeEvent.setRangeLength(event.getLength());
				changeEvent.setText(event.getDocument().get()); // TODO set to value after change
				this.change.setContentChanges(Arrays.asList(new TextDocumentContentChangeEventImpl[] { changeEvent }));
//			} catch (BadLocationException ex) {
//				ex.printStackTrace(); // TODO
//			}
		}
	}

	protected static final String LS_DIAGNOSTIC_MARKER_TYPE = "org.eclipse.languageserver.diagnostic"; //$NON-NLS-1$

	final private StreamConnectionProvider lspStreamProvider;
	private LanguageClientEndpoint languageClient;
	private IProject project;
	private IContentType contentType;
	private Map<IPath, DocumentChangeListenenr> connectedFiles;

	private Job languageClientListenerJob;
	
	public ProjectSpecificLanguageServerWrapper(IProject project, IContentType contentType, StreamConnectionProvider connection) {
		this.project = project;
		this.contentType = contentType;
		this.lspStreamProvider = connection;
	}

	private void start() throws IOException {
		if (this.languageClient != null) {
			return;
		}
		this.languageClient = new LanguageClientEndpoint();
		this.lspStreamProvider.start();
		MessageJsonHandler jsonHandler = new MessageJsonHandler();
		jsonHandler.setMethodResolver(this.languageClient);
		MessageReader reader = new StreamMessageReader(this.lspStreamProvider.getInputStream(), jsonHandler);
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
		this.languageClientListenerJob = new Job("Language Client Endpoint - " + project.getName() + " - " + contentType.getId()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				languageClient.connect(reader, writer);
				return Status.OK_STATUS;
			}
		};
		this.languageClientListenerJob.schedule();
		// initialize
		InitializeParamsImpl initParams = new InitializeParamsImpl();
		initParams.setRootPath(project.getLocation().toFile().getAbsolutePath());
		String name = "Eclipse IDE";
		if (Platform.getProduct() != null) {
			name = Platform.getProduct().getName();
		}
		initParams.setClientName(name);
		Integer.valueOf(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		initParams.setCapabilities(new ClientCapabilitiesImpl());
		connectDiagnostics();
		CompletableFuture<InitializeResult> result = languageClient.initialize(initParams);
		try {
			InitializeResult initializeResult = result.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.connectedFiles = new HashMap<>();
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
		this.languageClientListenerJob.cancel();
		this.languageClient.shutdown();
		this.languageClient.getWriter().close();
		this.languageClient.getReader().close();
		this.lspStreamProvider.stop();
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
	}
	
	public void disconnect(IFile file, IDocument document) {
		document.removeDocumentListener(this.connectedFiles.get(file.getLocation()));
		this.connectedFiles.remove(file.getLocation());
		if (this.connectedFiles.isEmpty()) {
			stop();
		}
	}

	public LanguageClientEndpoint getServer() {
		return languageClient;
	}
}
