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
package org.eclipse.languageserver.outline;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.languageserver.LSPImages;
import org.eclipse.languageserver.ui.Messages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.progress.ProgressManager;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import io.typefox.lsapi.SymbolInformation;

public class SymbolsLabelProvider extends LabelProvider implements ICommonLabelProvider, IStyledLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element == LSSymbolsContentProvider.COMPUTING) {
			return JFaceResources.getImage(ProgressManager.WAITING_JOB_KEY);
		}
		if (element instanceof Throwable) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		}
		return LSPImages.imageFromSymbolKind(((SymbolInformation) element).getKind());
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element == LSSymbolsContentProvider.COMPUTING) {
			return new StyledString(Messages.outline_computingSymbols);
		}
		if (element instanceof Throwable) {
			return new StyledString(((Throwable) element).getMessage());
		}
		SymbolInformation symbol = (SymbolInformation) element;
		StyledString res = new StyledString();
		res.append(symbol.getName(), null);
		res.append(" :", null); //$NON-NLS-1$
		res.append(symbol.getKind().toString(), StyledString.DECORATIONS_STYLER);
		return res;
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

	@Override
	public String getDescription(Object anElement) {
		return null;
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}

}
