package com.checkmarx.plugin.updater.netutils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Stack;


public class DomainSuffixResolver {

    public static String suffixFromFQDN(String hostFQDN) {

        if (hostFQDN == null)
            return null;

        Stack<String> components = new Stack<String>();

        for (String component : hostFQDN.split("\\."))
            components.push(component);

        if (components.size() <= 0)
            return null;

        StringBuilder suffix = new StringBuilder();

        while (components.size() > 1) {
            suffix.insert(0, components.pop());
            if (components.size() > 1)
                suffix.insert(0, '.');
        }

        return suffix.length() == 0 ? null : suffix.toString();
    }

    

    public static Iterable<String> resolveLocalDomainSuffixes() throws SocketException {

        LinkedList<String> resolved = new LinkedList<String>();

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface n = nets.nextElement();

            if (!n.isUp())
                continue;

            Enumeration<InetAddress> a = n.getInetAddresses();

            while (a.hasMoreElements()) {
                InetAddress addr = a.nextElement();
                for (String fqdn : FQDNExpander.expandHostnames(addr.getCanonicalHostName())) {
                    String suffix = suffixFromFQDN(fqdn);
                    if (suffix != null)
                        resolved.add(suffix);

                }

            }

        }

        return resolved;
    }
}