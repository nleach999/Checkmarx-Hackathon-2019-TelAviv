package com.checkmarx.plugin.eclipse;

import com.checkmarx.plugin.updater.client.LatestVersion;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class LatestVersionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		LatestVersion v = HelloWorld.getDefault().getLatestVersion();

		String msg = "Latest version unknown, version may still be updating.";

		if (v != null)
			msg = v.getFilename();

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "Latest Version", msg);
		return null;
	}

	@Override
	public boolean isEnabled() {
		return HelloWorld.getDefault().isReady();
	}

}
