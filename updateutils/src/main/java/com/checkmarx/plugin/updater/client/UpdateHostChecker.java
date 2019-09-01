package com.checkmarx.plugin.updater.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.checkmarx.plugin.updater.client.exceptions.BadBuilderException;
import com.checkmarx.plugin.updater.client.exceptions.MisconfiguredException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class UpdateHostChecker {
    private static String DEFAULT_HOSTNAME = "cxupdate";
    private static String DEFAULT_SCHEME = "http";

    public static String getDefaultHostname() {
        return DEFAULT_HOSTNAME;
    }

    public static Builder builder() {
        return new UpdateHostChecker.Builder();
    }

    private ArrayList<String> _domainSuffixes = new ArrayList<String>();
    private String _hostName = DEFAULT_HOSTNAME;
    private Boolean _skipLocal = false;
    private String _scheme = DEFAULT_SCHEME;

    private Boolean _inCall = false;
    private Consumer<LatestVersion> _resolvedCallback;

    private int _maxRetries = 3;
    private int _retryDelaySeconds = 300;

    private Pattern _regex;

    public void checkForUpdates(Consumer<LatestVersion> callback) {
        _inCall = true;
        _resolvedCallback = callback;

        LatestVersion v = doLocalResolution();
        if (v == null)
            v = doRemoteResolution();

        if (v != null)
            _resolvedCallback.accept(v);
    }

    private String getCharsetFromContentType(String contentType) {
        String[] elements = contentType.split("\\s|;|charset=");
        return elements[elements.length - 1].trim();
    }

    private LatestVersion doResolveRemoteDirectoryContents(CloseableHttpClient httpClient, URI serverUri)
            throws IOException {
        Dictionary<String, Matcher> entries = getRemoteDirectoryContents(httpClient, serverUri);

        LatestVersion v = null;

        if (entries != null && entries.size() > 0) {
            String key = pickLatestVersion(entries);

            try {
                v = new LatestVersion(key, new URIBuilder(serverUri).setPath(key).build().toString(), entries.get(key));
            } catch (URISyntaxException ex) {
                // TODO: Handle exception
            }
        }

        return v;
    }

    private String pickLatestVersion(Dictionary<String, Matcher> foundFiles) {
        TreeSet<String> sortedFilenames = new TreeSet<String>();

        Enumeration<String> keys = foundFiles.keys();

        while (keys.hasMoreElements())
            sortedFilenames.add(keys.nextElement());

        return sortedFilenames.last();
    }

    private Dictionary<String, Matcher> getRemoteDirectoryContents(CloseableHttpClient httpClient, URI serverUri)
            throws IOException {

        Dictionary<String, Matcher> foundEntries = new Hashtable<String, Matcher>();

        HttpGet httpGet = new HttpGet(serverUri);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            if (response.getStatusLine().getStatusCode() == 200
                    && response.getEntity().getContentType().getValue().toLowerCase().startsWith("text/html")) {

                Document htmlContent = Jsoup.parse(response.getEntity().getContent(),
                        getCharsetFromContentType(response.getEntity().getContentType().getValue()),
                        serverUri.toString());

                Elements anchors = htmlContent.select("a[href]");

                for (Element anchor : anchors) {
                    Matcher m = _regex.matcher(anchor.text());
                    if (m.matches()) {
                        foundEntries.put(anchor.text(), m);
                    }
                }
            }

        } finally {
            response.close();
        }

        return foundEntries;
    }

    private LatestVersion doLocalResolution() {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        LatestVersion latestVersion = null;

        try {
            URI noDomain = null, mDNS = null;

            try {
                noDomain = formServerURI();
                mDNS = formServerURI("local");
            } catch (URISyntaxException e) {
            }

            try {
                latestVersion = doResolveRemoteDirectoryContents(httpClient, noDomain);

                if (latestVersion == null)
                    latestVersion = doResolveRemoteDirectoryContents(httpClient, mDNS);

            } catch (IOException e) {
                // TODO: Increment retry, retry delay, etc.
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                // Intentionally ignoring the exception here.
            }
        }

        return latestVersion;
    }

    private LatestVersion doRemoteResolution() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        LatestVersion latestVersion = null;

        try {

            for (String suffix : _domainSuffixes) {
                if (latestVersion != null)
                    break;

                URI serverUri = null;

                try {
                    serverUri = formServerURI(suffix);
                } catch (URISyntaxException e) {
                    continue;
                }

                try {
                    latestVersion = doResolveRemoteDirectoryContents(httpClient, serverUri);
                } catch (IOException ex) {
                    // TODO: increment try count, delay for retry, etc.
                }

            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                // Intentionally ignoring the exception here.
            }
        }

        return latestVersion;
    }

    private URI formServerURI(String suffix) throws URISyntaxException {
        return new URIBuilder().setHost(_hostName + "." + suffix).setScheme(_scheme).build();
    }

    private URI formServerURI() throws URISyntaxException {
        return new URIBuilder().setHost(_hostName).setScheme(_scheme).build();
    }

    public static class Builder {

        private Builder() {
            _inst = new UpdateHostChecker();
        }

        private UpdateHostChecker _inst;

        public class RegexValidatingBuilder {

            public UpdateHostChecker build() throws MisconfiguredException, BadBuilderException {

                if (_inst == null)
                    throw new BadBuilderException();

                if (_inst._domainSuffixes.size() <= 0)
                    throw new MisconfiguredException("No domain search suffixes have been supplied");

                UpdateHostChecker retVal = _inst;
                _inst = null;
                return retVal;
            }
        }

        public RegexValidatingBuilder withFieldExtractRegex(Pattern regex) {
            _inst._regex = regex;
            return new RegexValidatingBuilder();
        }

        public Builder maxRetries(int retryCount) {
            _inst._maxRetries = retryCount;
            return this;
        }

        public Builder retryDelaySeconds(int retryDelay) {
            _inst._retryDelaySeconds = retryDelay;
            return this;
        }

        public Builder withUpdateHostName(String hostName) {

            _inst._hostName = hostName;

            return this;
        }

        public Builder skipLocalNetworkResolution() {

            _inst._skipLocal = true;

            return this;
        }

        public Builder addSearchDomainSuffix(String domainSuffix) {

            _inst._domainSuffixes
                    .add((domainSuffix.startsWith(".")) ? (domainSuffix.substring(1, domainSuffix.length()))
                            : (domainSuffix));
            return this;
        }

        public Builder withDomainSuffixes(String[] domainSuffixes) {

            for (String domainSuffix : domainSuffixes)
                _inst._domainSuffixes
                        .add((domainSuffix.startsWith(".")) ? (domainSuffix.substring(1, domainSuffix.length()))
                                : (domainSuffix));

            return this;
        }

    }

    private UpdateHostChecker() {
    }

}