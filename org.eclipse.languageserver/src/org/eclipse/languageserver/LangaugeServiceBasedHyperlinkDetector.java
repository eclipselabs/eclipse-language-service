package org.eclipse.languageserver;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class LangaugeServiceBasedHyperlinkDetector extends AbstractHyperlinkDetector {

	public LangaugeServiceBasedHyperlinkDetector() {
	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		List<IHyperlink> res = new ArrayList<>();
		for (LanguageClient client : LanguageClient.getAllLanguageClientFor(null)) {
			res.add(new OpenEditorRegionHyperlink(client.getHyperlink(textViewer.getDocument(), region), region));
		}
		return res.toArray(new IHyperlink[res.size()]);
	}

	
}
