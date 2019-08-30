package com.checkmarx.plugin.updater.netutils;

import java.util.LinkedList;

class FQDNExpander {

    private static String DOMAIN_ELEMENT_SEPARATOR = ".";

    public static LinkedList<String> expandHostnames(String fqdnOfHost) {

        String fqdnElements[] = fqdnOfHost.split("\\.");
        String hostName = fqdnElements[0];
        String domainSuffix = fqdnOfHost.substring(fqdnOfHost.indexOf(hostName) + hostName.length(),
                fqdnOfHost.length());

        LinkedList<String> returnList = makeHostList(domainSuffix, hostName);

        return returnList;
    }

    private static LinkedList<String> makeHostList(String domainSuffix, String hostName) {

        LinkedList<String> returnList = new LinkedList<String>();
        String suffixElements[] = domainSuffix.split("\\.");

        for (String element : suffixElements) {

            int startOfElement = domainSuffix.indexOf(element);
            if (startOfElement >= 0) {
                int pos = startOfElement + element.length();
                returnList.add(hostName.concat(domainSuffix.substring(pos, domainSuffix.length())));
            }
        }
        return returnList;
    }

    public static LinkedList<String> expandHostnames(String hostName, String domainSuffix) {
        return makeHostList (DOMAIN_ELEMENT_SEPARATOR.concat(domainSuffix), hostName);
    }

}