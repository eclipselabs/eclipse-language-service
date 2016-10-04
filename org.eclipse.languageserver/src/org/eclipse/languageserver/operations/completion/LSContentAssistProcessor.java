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
package org.eclipse.languageserver.operations.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.languageserver.LSPEclipseUtils;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.LanguageServiceAccessor.LSPDocumentInfo;

import io.typefox.lsapi.CompletionItem;
import io.typefox.lsapi.CompletionList;
import io.typefox.lsapi.impl.TextDocumentPositionParamsImpl;

public class LSContentAssistProcessor implements IContentAssistProcessor {

	public LSContentAssistProcessor() {
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		ICompletionProposal[] res = new ICompletionProposal[0];
		final LSPDocumentInfo info = LanguageServiceAccessor.getLSPDocumentInfoFor(viewer, capabilities -> capabilities.getCodeLensProvider() != null);
		CompletableFuture<CompletionList> request = null;
		try {
			if (info.languageClient != null) {
				IDocument document = viewer.getDocument();
				TextDocumentPositionParamsImpl param = LSPEclipseUtils.toTextDocumentPosistionParams(info.fileUri, offset, document);
				request = info.languageClient.getTextDocumentService().completion(param);
				CompletionList completionList = request.get(5, TimeUnit.SECONDS);
				res = toProposals(offset, completionList);
			}
		} catch (Exception ex) {
			ex.printStackTrace(); //TODO
			if (request != null) {
				res = toProposals(offset, request.getNow(null));
			}
		}
		return res;
	}

	private ICompletionProposal[] toProposals(int offset, CompletionList completionList) {
		if (completionList == null) {
			return new ICompletionProposal[0];
		}
		ICompletionProposal[] res;
		List<ICompletionProposal> proposals = new ArrayList<>();
		for (CompletionItem item : completionList.getItems()) {
			String text = item.getInsertText();
			if (text == null) {
				text = item.getSortText();
			}
			// TODO also consider item.getTextEdit
			// TODO add description and so on
			proposals.add(new LSCompletionProposal(item, offset));
		}
		res = proposals.toArray(new ICompletionProposal[proposals.size()]);
		return res;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		// TODO
		return new IContextInformation[0];
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		return "Error"; //$NON-NLS-1$
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
