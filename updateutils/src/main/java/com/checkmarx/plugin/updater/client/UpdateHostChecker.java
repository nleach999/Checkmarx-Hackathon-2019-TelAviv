package com.checkmarx.plugin.updater.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.function.Consumer;
import java.util.function.Function;
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

    public static Builder builder() {
        return new UpdateHostChecker.Builder();
    }

    private ArrayList<String> _domainSuffixes = new ArrayList<String>();
    private String _hostName = DEFAULT_HOSTNAME;
    private Boolean _skipLocal = false;
    private String _scheme = DEFAULT_SCHEME;

    private Boolean _inCall = false;
    private Consumer<Object> _callback;

    private int _maxRetries = 3;
    private int _retryDelaySeconds = 300;

    private Pattern _regex;

    public void checkForUpdates(Consumer<Object> callback) {
        _inCall = true;
        _callback = callback;

        doCheck();

        callback.accept(new Object());
        // Execute thread here is async

    }

    private String getCharsetFromContentType(String contentType) {
        String[] elements = contentType.split("\\s|;|charset=");
        return elements[elements.length - 1].trim();
    }

    private LatestVersion doCheckDirectoryListing(CloseableHttpClient httpClient, URI serverUri) {
        Dictionary<String, Matcher> entries = getRemoteDirectoryContents(httpClient, serverUri);

        LatestVersion v = null;


        if (entries != null && entries.size() > 0) {
            String key = pickLatestVersion(entries);
            v = new LatestVersion (key, new URI (serverUri, key).toString(), entries[key]);
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

    private void doCheck() {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        for (String suffix : _domainSuffixes) {

            URI serverUri = null;

            try {
                serverUri = new URIBuilder().setHost(_hostName + "." + suffix).setScheme(_scheme).build();
            } catch (URISyntaxException e) {
                continue;
            }

            try {

                LatestVersion latestVersion = doCheckDirectoryListing(httpClient, serverUri);
                System.out.println(latestVersion);

            } catch (IOException ex) {
                // TODO: increment try count, delay for retry, etc.
            }

        }

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