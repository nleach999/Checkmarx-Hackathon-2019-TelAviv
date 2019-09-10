package com.checkmarx.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.regex.Pattern;

import com.checkmarx.plugin.downloader.PluginDownloader;
import com.checkmarx.plugin.updater.client.LatestVersion;
import com.checkmarx.plugin.updater.client.UpdateHostChecker;
import com.checkmarx.plugin.updater.client.exceptions.BadBuilderException;
import com.checkmarx.plugin.updater.client.exceptions.MisconfiguredException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class HelloWorld extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "SimpleMenu";

	// The shared instance
	private static HelloWorld plugin;

	private Pattern _matchRegex = Pattern.compile(
			"(?<filename>TestPlugin)-(?<major>\\d{2})\\.(?<minor>\\d{2})\\.(?<revision>\\d{2})_{0,1}(?<custom>.+)?\\.jar");

	private UpdateHostChecker _hc = null;
	private Iterable<String> _localSuffixes;

	private String _archiveName = null;

	public HelloWorld() {
	}

	private String _pluginVersion = null;

	public String getCurrentVersion() {
		return _pluginVersion;
	}

	private Object _versionLock = new Object();
	private LatestVersion _version = null;

	public LatestVersion getLatestVersion() {
		synchronized (_versionLock) {
			return _version;
		}
	}

	private void onDownloadComplete(Boolean done) {

		String msg = "DOWNLOAD FAILED";
		if (done) {
			msg = "Download complete, please reboot.";
			System.out.println("Download completed successfully");
		} else
			System.out.println("Download FAILED");

		installNewPlugin();

		displayVersionDialog(msg, _version);

		PlatformUI.getWorkbench().restart();

	}

	private void installNewPlugin() {
		String cwd = System.getProperty("user.dir");

		File downloaded = new File(FileSystems.getDefault().getPath(cwd, _version.getFilename()).toString());
		File installed = new File(FileSystems.getDefault().getPath(cwd, "dropins", _version.getFilename()).toString());
		File old = new File(FileSystems.getDefault().getPath(cwd, "dropins", _archiveName).toString());

		try {
			FileUtils.copyFile(downloaded, installed, true);
			old.delete();
			downloaded.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void downloadLatestVersion() throws URISyntaxException {
		PluginDownloader pdInst = PluginDownloader.builder().withPluginURI(new URI(_version.getFileURI()))
				.withDownloadFilename(_version.getFilename()).withMaxPluginSizeInMegabytes(20).build();

		pdInst.doDownload((foo) -> {
			System.out.println("Progress Callback: " + foo);
		}, this::onDownloadComplete);
	}

	public static Boolean isVersionGreaterThanCurrent(LatestVersion v) {

		if (v == null)
			return false;

		String versionForCompare = getVersionString(v);

		return versionForCompare.compareTo(getDefault().getCurrentVersion()) > 0;
	}

	private static String getVersionString(LatestVersion v) {
		String retVal = String.format("%s.%s.%s", v.getRegexMatches().group("major"),
				v.getRegexMatches().group("minor"), v.getRegexMatches().group("revision"));

		if (v.getRegexMatches().group("custom") != null)
			retVal = String.format("%s.%s", retVal, v.getRegexMatches().group("custom"));

		return retVal;

	}

	public void displayVersionDialog(String title, LatestVersion v) {
		Display display = new Display();

		Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		shell.setText(title);
		shell.setLayout(new FillLayout());

		Composite parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new GridLayout(2, true));

		new Label(parent, SWT.LEFT).setText("Version:");
		new Label(parent, SWT.RIGHT).setText(getVersionString(v));

		new Label(parent, SWT.LEFT).setText("Filename:");
		new Label(parent, SWT.RIGHT).setText(v.getFilename());

		new Label(parent, SWT.LEFT).setText("Location: " + v.getFileURI());

		shell.setSize(400, 200);
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

	}

	private void updateCheckCallback(LatestVersion v) {
		if (v == null)
			return;

		_version = v;

		if (isVersionGreaterThanCurrent(v))
			displayVersionDialog("New Version Available", _version);
	}

	public void initialize(Iterable<String> domainSuffixes) {
		_localSuffixes = domainSuffixes;

		try {
			_hc = UpdateHostChecker.builder().withDomainSuffixes(_localSuffixes).withFieldExtractRegex(_matchRegex)
					.build();
		} catch (MisconfiguredException | BadBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		_hc.checkForUpdates(this::updateCheckCallback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		_pluginVersion = context.getBundle().getHeaders().get("Bundle-Version");

		_archiveName = context.getBundle().getHeaders().get("Bundle-Name");

		plugin = this;
	}

	public Boolean isReady() {
		return _version != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		_pluginVersion = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static HelloWorld getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative
	 * path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
