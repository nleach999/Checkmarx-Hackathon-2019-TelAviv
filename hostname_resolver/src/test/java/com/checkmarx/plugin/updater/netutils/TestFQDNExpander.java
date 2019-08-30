package com.checkmarx.plugin.updater.netutils;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedList;

public class TestFQDNExpander {

    @Test
    public void testExpectedNumElementsWithFQDN() {
        String fqdn = "cxupdate.my.domain.com";
        assertTrue(FQDNExpander.expandHostnames(fqdn).size() == 4);
    }

    @Test
    public void testExpectedNumElementsWithHostnameAndSuffix() {
        String suffix = "my.domain.com";
        assertTrue(FQDNExpander.expandHostnames("cxupdate", suffix).size() == 4);
    }

    @Test
    public void testSameResultsForBothFormsOfExpansion() {
        String fqdn = "cxupdate.my.domain.com";
        String hostName = "cxupdate";
        String domainSuffix = "my.domain.com";
        LinkedList<String> list1 = FQDNExpander.expandHostnames(fqdn);
        LinkedList<String> list2 = FQDNExpander.expandHostnames(hostName, domainSuffix);

        try {
            for (int i = 0; i < list1.size(); i++) {
                if (list1.get(i).compareTo(list2.get(i)) != 0)
                    throw new Exception("Hostname mismatch");
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
            return;
        }

        assertTrue(list1.size() == list2.size());

    }

}