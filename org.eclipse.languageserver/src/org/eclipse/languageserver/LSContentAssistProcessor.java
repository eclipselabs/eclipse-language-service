package org.eclipse.languageserver;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import io.typefox.lsapi.CompletionItem;
import io.typefox.lsapi.CompletionItemImpl;
import io.typefox.lsapi.CompletionList;
import io.typefox.lsapi.DidChangeConfigurationParamsImpl;
import io.typefox.lsapi.DidChangeTextDocumentParamsImpl;
import io.typefox.lsapi.DidOpenTextDocumentParamsImpl;
import io.typefox.lsapi.InitializeParamsImpl;
import io.typefox.lsapi.InitializeResult;
import io.typefox.lsapi.PositionImpl;
import io.typefox.lsapi.RangeImpl;
import io.typefox.lsapi.TextDocumentIdentifierImpl;
import io.typefox.lsapi.TextDocumentItemImpl;
import io.typefox.lsapi.TextDocumentPositionParamsImpl;
import io.typefox.lsapi.TextEditImpl;
import io.typefox.lsapi.VersionedTextDocumentIdentifierImpl;
import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

public class LSContentAssistProcessor implements IContentAssistProcessor {

	public LSContentAssistProcessor() {
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (!(activeEditor instanceof AbstractTextEditor)) {
			return new ICompletionProposal[0];
		}
		IEditorInput input = activeEditor.getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			URI fileUri = file.getLocation().toFile().toURI();
			try {
				JsonBasedLanguageServer server = LanaguageServiceAccessor.getLanaguageServer(file, viewer.getDocument());

				PositionImpl start = new PositionImpl();
				start.setLine(viewer.getDocument().getLineOfOffset(offset));
				start.setCharacter(offset - viewer.getDocument().getLineInformationOfOffset(offset).getOffset());
				TextDocumentPositionParamsImpl param = new TextDocumentPositionParamsImpl();
				param.setPosition(start);
				param.setUri(fileUri.toString());
				TextDocumentIdentifierImpl id = new TextDocumentIdentifierImpl();
				id.setUri(fileUri.toString());
				param.setTextDocument(id);
				CompletableFuture<CompletionList> res = server.getTextDocumentService().completion(param);
				List<ICompletionProposal> proposals = new ArrayList<>();
				for (CompletionItem item : res.get().getItems()) {
					proposals.add(new CompletionProposal(item.getInsertText(), offset, 0, item.getInsertText().length()));
				}
				return proposals.toArray(new ICompletionProposal[proposals.size()]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return null;
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
		return "Error";
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
