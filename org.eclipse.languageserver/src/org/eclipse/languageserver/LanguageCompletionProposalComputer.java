package org.eclipse.languageserver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;

public class LanguageCompletionProposalComputer implements IJavaCompletionProposalComputer {

	public LanguageCompletionProposalComputer() {
	}
	@Override
	public void sessionStarted() {
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		List<ICompletionProposal> languageServiceProposals = new ArrayList<>();
		for (LanguageClient languageClient : LanguageClient.getAllLanguageClientFor(null)) {
			if (languageClient.isActiveFor(context)) {
				Set<String> proposals = languageClient.getCompletionProposals(context);
				for (String proposal : proposals) {
					languageServiceProposals.add(new CompletionProposal(proposal, context.getInvocationOffset(), 0, 0));;
				}
			}
		}
		return languageServiceProposals;
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
		
	}

}
 