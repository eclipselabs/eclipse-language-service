package org.eclipse.languageserver;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;

/**
 * This class wraps several interaction with the language
 * server protocol
 *
 */
public class LanguageClient {

	public static class EditorRegion {
		private IEditorInput editorInput;
		private IRegion region;
		
		public EditorRegion(IEditorInput editorInput, IRegion region) {
			this.editorInput = editorInput;
			this.region = region;
		}

		public IEditorInput getEditorInput() {
			return editorInput;
		}

		public IRegion getRegion() {
			return this.region;
		}
	}
	
	/**
	 * 
	 * @param resource the "context" for this client. Currently a resource but should
	 * be something more generic (file-name?) since it can be used for any file.
	 * @return
	 */
	public static Set<LanguageClient> getAllLanguageClientFor(IResource resource) {
		Set<LanguageClient> res = new HashSet<>();
		res.add(new LanguageClient());
		return res;
	}

	private LanguageClient() {
	}

	public Set<String> getCompletionProposals(ContentAssistInvocationContext context) {
		Set<String> proposals = new HashSet<>();
		// MOCK: implement as a request to language service
		proposals.add("Hello Eclipse!");
		return proposals;
	}
	
	public EditorRegion getHyperlink(IDocument iDocument, IRegion region) {
		// Mock
		return new EditorRegion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput(),
				new Region(0, 2));
	}

	public boolean isActiveFor(ContentAssistInvocationContext context) {
		return true; // TODO, implement real condition
	}

}
