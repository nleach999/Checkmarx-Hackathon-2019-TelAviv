package com.checkmarx.plugin.eclipse;

import java.net.URISyntaxException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;

public class InstallHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String pwd = System.getProperty("user.dir");
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        MessageDialog.openInformation(window.getShell(), "Install Notification",
                "Installing from: " + HelloWorld.getDefault().getLatestVersion().getFileURI() + ", please wait");

        try {
            HelloWorld.getDefault().downloadLatestVersion();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isEnabled() {
        return HelloWorld.isVersionGreaterThanCurrent (HelloWorld.getDefault ().getLatestVersion() );
    }
}
