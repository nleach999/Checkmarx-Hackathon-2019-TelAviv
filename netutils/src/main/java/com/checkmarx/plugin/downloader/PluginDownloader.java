package com.checkmarx.plugin.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class PluginDownloader {

    private static long MAX_PLUGIN_MB = 500;

    private URI _targetURI;
    private Function<InputStream, Boolean> _sigFunc = null;
    private Consumer<String> _progressCallback = null;
    private long _maxMB = MAX_PLUGIN_MB;

    private PluginDownloader() {
    }

    public Future<?> doDownload(Consumer<String> progressCallback) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {

            HttpHead headCheck = new HttpHead(_targetURI);
            CloseableHttpResponse response = httpClient.execute(headCheck);
            Header h[] = response.getAllHeaders();
            response.close ();

        } catch (IOException ex) {

        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                // Intentionally ignoring exception
            }
        }

        return null;
    }

    public static Builder builder() {
        return new PluginDownloader.Builder();
    }

    public static class Builder {
        private PluginDownloader _inst;

        private Builder() {
            _inst = new PluginDownloader();
        }

        public PluginDownloader build() {
            return _inst;
        }

        public PluginDownloader.Builder withPluginURI(URI pluginLocation) {
            _inst._targetURI = pluginLocation;
            return this;
        }

        public PluginDownloader.Builder withSignatureVerificationFunction(Function<InputStream, Boolean> sigVerifier) {
            _inst._sigFunc = sigVerifier;
            return this;
        }

        public PluginDownloader.Builder withMaxPluginSizeInMegabytes(long megaBytes) {
            _inst._maxMB = megaBytes;
            return this;
        }

    }

}