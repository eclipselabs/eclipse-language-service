package org.eclipse.languageserver;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;

/**
 * This class wraps several interaction with the language
 * server protocol
 *
 */
public class LanguageClient {

	private URI serviceUri;

	public LanguageClient(URI serviceUri) {
		this.serviceUri = serviceUri;
	}

	public Set<String> getCompletionProposals(ContentAssistInvocationContext context) {
		Set<String> proposals = new HashSet<>();
		// MOCK: implement as a request to language service
		proposals.add("Hello Eclipse!");
		return proposals;
	}

	public boolean isActiveFor(ContentAssistInvocationContext context) {
		return true; // TODO, implement real condition
	}

}
