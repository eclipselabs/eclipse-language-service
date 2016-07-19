package org.eclipse.languageserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.osgi.storage.bundlefile.FileBundleEntry;

import io.typefox.lsapi.DidChangeTextDocumentParamsImpl;
import io.typefox.lsapi.DidOpenTextDocumentParamsImpl;
import io.typefox.lsapi.InitializeParamsImpl;
import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.PositionImpl;
import io.typefox.lsapi.RangeImpl;
import io.typefox.lsapi.TextDocumentContentChangeEvent;
import io.typefox.lsapi.TextDocumentContentChangeEventImpl;
import io.typefox.lsapi.TextDocumentItemImpl;
import io.typefox.lsapi.VersionedTextDocumentIdentifierImpl;
import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

public class LanguageServerWrapper {

	private final class DocumentChangeListenenr implements IDocumentListener {
		private IFile file;
		private int version = 2;

		public DocumentChangeListenenr(IFile file) {
			this.file = file;
		}

		@Override
		public void documentChanged(DocumentEvent event) {
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			try {
				DidChangeTextDocumentParamsImpl change = new DidChangeTextDocumentParamsImpl();
				VersionedTextDocumentIdentifierImpl doc = new VersionedTextDocumentIdentifierImpl();
				doc.setUri(file.getLocationURI().toString());
				doc.setVersion(version);
				change.setTextDocument(doc);
				TextDocumentContentChangeEventImpl changeEvent = new TextDocumentContentChangeEventImpl();
				RangeImpl range = new RangeImpl();
				PositionImpl start = new PositionImpl();
				start.setLine(event.getDocument().getLineOfOffset(event.getOffset()));
				start.setCharacter(event.getOffset() - event.getDocument().getLineInformationOfOffset(event.getOffset()).getOffset());
				range.setStart(start);
				PositionImpl end = new PositionImpl();
				end.setLine(event.getDocument().getLineOfOffset(event.getOffset() + event.getLength()));
				end.setCharacter(event.getOffset() + event.getLength() - event.getDocument().getLineInformationOfOffset(event.getOffset() + event.getLength()).getOffset());
				range.setEnd(end);
				changeEvent.setRange(range);
				changeEvent.setRangeLength(event.getLength());
				changeEvent.setText(event.getText());
				change.setContentChanges(Arrays.asList(new TextDocumentContentChangeEventImpl[] { changeEvent }));
				server.getTextDocumentService().didChange(change);
				version++;
			} catch (BadLocationException ex) {
				ex.printStackTrace(); // TODO
			}
		}
	}

	private Process process;
	private JsonBasedLanguageServer server;
	private IProject project;
	private Map<IPath, DocumentChangeListenenr> connectedFiles;
	
	public LanguageServerWrapper(IProject project) {
		this.project = project;
	}
	
	private void start() throws IOException {
		if (this.process != null) {
			return;
		}
		// Steps to get a usable Language server (for language supported by VSCode LS implementations)
		// 1. Make VSCode LS [for CSS here] use STDIN/STDOUT. In file extensions/css/server/cssServerMain.ts change connection line to
		//       let connection: IConnection = createConnection(new StreamMessageReader(process.in), new StreamMessageWriter(process.out));
		//    and add imports  StreamMessageReader, StreamMessageWriter (as same level as IPCMessageReader/Writer)
		// 2. Build VSCode as explained in https://github.com/Microsoft/vscode/wiki/How-to-Contribute#build-and-run-from-source
		//     (don't forget the `npm run watch` after the install script)
		// 3. Then set language server location and adapt process builder.
		
		ProcessBuilder builder = new ProcessBuilder("/usr/bin/node", "/home/mistria/git/vscode/extensions/css/server/out/cssServerMain.js")
			.directory(new File("/home/mistria/git/vscode/extensions/css/server/out/"));
		this.process = builder.start();
		this.server = new JsonBasedLanguageServer();
		this.server.connect(this.process.getInputStream(), this.process.getOutputStream());
		// initialize
		InitializeParamsImpl initParams = new InitializeParamsImpl();
		initParams.setRootPath(project.getLocation().toFile().getAbsolutePath());
		CompletableFuture<InitializeResult> result = server.initialize(initParams);
		this.connectedFiles = new HashMap<>();
	}
	
	private void stop() {
		this.process.destroy();
		this.process = null;
		this.server.shutdown();
		this.server = null;
	}

	public void connect(IFile file, IDocument document) throws IOException {
		start();
		if (this.connectedFiles.containsKey(file.getLocation())) {
			return;
		}
		// add a document buffer
		DidOpenTextDocumentParamsImpl open = new DidOpenTextDocumentParamsImpl();
		TextDocumentItemImpl textDocument = new TextDocumentItemImpl();
		textDocument.setUri(file.getLocationURI().toString());
		textDocument.setText(document.get());
		open.setTextDocument(textDocument);
		this.server.getTextDocumentService().didOpen(open);
		
		DocumentChangeListenenr listener = new DocumentChangeListenenr(file);
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

	public JsonBasedLanguageServer getServer() {
		return server;
	}
}
