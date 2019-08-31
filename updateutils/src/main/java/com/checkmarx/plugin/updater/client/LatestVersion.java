package com.checkmarx.plugin.updater.client;

public class LatestVersion {

    public LatestVersion(String filename, String fileURI, Matcher matches) {
        _fileName = filename;
        _fileURI = fileURI;
        _m = matches;
    }

    private String _fileName;

    public String getFilename() {
        return _fileName;
    }

    private String _fileURI;

    public String getFileURI() {
        return _fileURI;
    }

    private Matcher _m;

    public Matcher getRegexMatches() {
        return _m;
    }

	@Override
	public String toString() {
		return String.format("LatestVersion: %s (%s)", _fileName, _fileURI);
	}

    
    
}