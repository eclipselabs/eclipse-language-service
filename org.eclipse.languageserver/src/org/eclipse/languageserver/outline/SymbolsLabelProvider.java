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
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.languageserver.ui.Messages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.progress.ProgressManager;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import io.typefox.lsapi.SymbolInformation;

public class SymbolsLabelProvider implements ICommonLabelProvider, IStyledLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element == LSSymbolsContentProvider.COMPUTING) {
			return JFaceResources.getImage(ProgressManager.WAITING_JOB_KEY);
		}
		if (element instanceof Throwable) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		}
		switch (((SymbolInformation)element).getKind()) {
		// TODO
		case Array: break;
		case Boolean: break;
		case Class: break;
		case Constant: break;
		case Constructor: break;
		case Enum: break;
		case Field: break;
		case File: return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		case Function: break;
		case Interface: break;
		case Method: break;
		case Module: break;
		case Namespace: break;
		case Number: break;
		case Package: break;
		case Property: break;
		case String: break;
		case Variable: break;
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void dispose() {

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {

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

	@Override
	public StyledString getStyledText(Object element) {
		if (element == LSSymbolsContentProvider.COMPUTING) {
			return new StyledString(Messages.outline_computingSymbols);
		}
		if (element instanceof Throwable) {
			return new StyledString(((Throwable) element).getMessage());
		}
		SymbolInformation symbol = (SymbolInformation)element;
		StyledString res = new StyledString();
		res.append(symbol.getName(), null);
		res.append(" :", null); //$NON-NLS-1$
		res.append(symbol.getKind().toString(), StyledString.DECORATIONS_STYLER);
		return res;
	}

}
