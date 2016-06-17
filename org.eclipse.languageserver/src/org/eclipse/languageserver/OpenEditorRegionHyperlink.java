package org.eclipse.languageserver;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.languageserver.LanguageClient.EditorRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

public class OpenEditorRegionHyperlink implements IHyperlink {

	private EditorRegion hyperlink;
	private IRegion region;

	public OpenEditorRegionHyperlink(EditorRegion hyperlink, IRegion region) {
		this.hyperlink = hyperlink;
		this.region = region;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return new Region(this.region.getOffset() - 1, 2);
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	@Override
	public String getHyperlinkText() {
		return "Open Declaration";
	}

	@Override
	public void open() {
		try {
			IEditorPart targetEditor = null;
			for (IEditorReference editor : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
				if (editor.getEditorInput().equals(hyperlink.getEditorInput())) {
					targetEditor = editor.getEditor(true);
				}
			}
			if (targetEditor == null) {
				targetEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(hyperlink.getEditorInput(), null);
			}
			targetEditor.getEditorSite().getSelectionProvider().setSelection(new TextSelection(hyperlink.getRegion().getOffset(), hyperlink.getRegion().getLength()));
		} catch (Exception ex) {
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "error", ex.getMessage());
			ex.printStackTrace();
		}
	}

}
