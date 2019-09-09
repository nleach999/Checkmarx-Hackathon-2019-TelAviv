package com.checkmarx.plugin.eclipse;

import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.checkmarx.plugin.updater.netutils.DomainSuffixResolver;

import org.eclipse.ui.IStartup;

public class StartupHandler implements IStartup, Runnable {

    private ExecutorService _executor = Executors.newSingleThreadExecutor();

    @Override
    public void earlyStartup() {
        _executor.submit(this);
    }

    @Override
    public void run() {
        try {
            Iterable<String> suffixes = DomainSuffixResolver.resolveLocalDomainSuffixes();
            HelloWorld.getDefault().initialize(suffixes);
            _executor.shutdown();
        } catch (SocketException e) {
        }

    }

}