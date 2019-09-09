package com.checkmarx.plugin.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class PluginDownloader implements Runnable {

    private static final long MAX_PLUGIN_MB = 500;
    private static final String CONTENT_LEN_HEADER = "Content-Length";
    private static final long BYTES_TO_MB_DIVISOR = 1000000;

    private URI _targetURI;
    private Function<InputStream, Boolean> _sigFunc = null;
    private Consumer<String> _progressCallback = null;
    private Consumer<Boolean> _dlCompleteCallback = null;
    private long _maxMB = MAX_PLUGIN_MB;
    private String _destinationDirectory = System.getProperty("user.dir");
    private String _destinationFilename = "download.zip";

    private static final int READ_BUF_SIZE = 64738;
    private byte[] _readBuf = new byte[READ_BUF_SIZE];

    private ExecutorService _executor = Executors.newFixedThreadPool(2);
    private Future<?> _downloadFuture = null;

    private CloseableHttpClient _httpClient = null;

    private PluginDownloader() {
    }

    public void forceShutdown() {
        if (_downloadFuture != null) {
            _downloadFuture.cancel(true);
            _downloadFuture = null;
        }

        _executor.shutdown();
    }

    public Future<?> doDownload(Consumer<String> progressCallback) {

        if (_httpClient != null)
            return null;

        _progressCallback = progressCallback;

        _httpClient = HttpClients.createDefault();

        return _executor.submit(this);
    }

    public Future<?> doDownload(Consumer<String> progressCallback, Consumer<Boolean> dlCompleteCallback) {
        _dlCompleteCallback = dlCompleteCallback;
        return doDownload(progressCallback);
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

        public PluginDownloader.Builder downloadToDirectory(String dir) {
            _inst._destinationDirectory = dir;
            return this;
        }

        public PluginDownloader.Builder withDownloadFilename(String name) {
            _inst._destinationFilename = name;
            return this;
        }

    }

    private void reportProgress(String progressString) {
        if (_progressCallback != null)
            _progressCallback.accept(progressString);
    }

    private void reportCompletion(Boolean done) {
        if (_dlCompleteCallback != null)
            _dlCompleteCallback.accept(done);
    }

    private long _totalBytesRead = 0;
    private long _contentByteLen = 0;

    public boolean isDownloadComplete() {
        return _totalBytesRead == _contentByteLen;
    }

    @Override
    public void run() {
        CloseableHttpResponse response = null;

        try {

            reportProgress("Checking content length of " + _targetURI.getPath());

            HttpHead headCheck = new HttpHead(_targetURI);
            response = _httpClient.execute(headCheck);
            _contentByteLen = Long.parseLong(response.getLastHeader(CONTENT_LEN_HEADER).getValue());
            response.close();

            long mb = _contentByteLen / BYTES_TO_MB_DIVISOR;

            if (mb > _maxMB) {
                reportProgress(String.format("Content length of %dMB exceeds max length of %dMB", mb, _maxMB));
            } else {
                reportProgress(String.format("Downloading %dMB", mb));

                _downloadFuture = _executor.submit(() -> {

                    HttpGet downloadGet = new HttpGet(_targetURI);
                    CloseableHttpResponse downloadResponse = null;
                    try {
                        downloadResponse = _httpClient.execute(downloadGet);
                        InputStream payloadStream = downloadResponse.getEntity().getContent();

                        FileOutputStream fileOut = new FileOutputStream(
                                Paths.get(_destinationDirectory, _destinationFilename).toFile());

                        int lastRead = 0;
                        int lastReportedProgress = 0;

                        do {
                            lastRead = payloadStream.read(_readBuf);

                            if (lastRead > 0) {
                                fileOut.write(_readBuf, 0, lastRead);
                                _totalBytesRead += lastRead;
                            }

                            int progress = (int) ((_totalBytesRead / (float) _contentByteLen) * 100.0);

                            if (progress != lastReportedProgress && progress % 10 == 0) {
                                reportProgress(String.format("Downloaded %d%%", progress));
                                lastReportedProgress = progress;
                            }

                        } while (lastRead > 0);

                        payloadStream.close();

                        fileOut.flush();
                        fileOut.close();
                        
                        reportCompletion (isDownloadComplete() );
                        
                    } catch (IOException e) {
                        reportProgress(e.getMessage());
                    } finally {
                        if (downloadResponse != null)
                            try {
                                downloadResponse.close();
                            } catch (IOException e) {
                                // Intentionally ignoring exception
                            }

                    }
                });

                // Wait for download to finish or cancel
                try {
                    _downloadFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    reportProgress(e.getMessage());
                }
                _executor.shutdown();
            }

        } catch (IOException ex) {

            // Intentionally ignoring exception.

        } finally {
            try {
                if (response != null)
                    response.close();

                _httpClient.close();
            } catch (IOException e) {
                // Intentionally ignoring exception
            }
        }

    }

}