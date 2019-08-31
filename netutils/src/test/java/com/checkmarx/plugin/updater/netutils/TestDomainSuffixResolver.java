package com.checkmarx.plugin.updater.netutils;

import org.junit.Test;
import static org.junit.Assert.*;

import java.net.SocketException;

public class TestDomainSuffixResolver {
    public TestDomainSuffixResolver() {

    }

    @Test
    public void someDomainsResolve() {
        try {
            assertTrue(DomainSuffixResolver.resolveLocalDomainSuffixes().iterator().hasNext());
        } catch (SocketException ex) {
            fail(ex.getMessage());
        }

    }

    @Test
    public void testSuffixFromHostnameOnlyIsNull() {
        assertNull(DomainSuffixResolver.suffixFromFQDN("ahostname"));
    }

    @Test
    public void testEmptySuffixResultIsNull() {
        assertNull(DomainSuffixResolver.suffixFromFQDN("ahostname."));
    }

    @Test
    public void testEmptyStringResultIsNull() {
        assertNull(DomainSuffixResolver.suffixFromFQDN(""));
    }

    @Test
    public void testSingleSeparatorInStringResultIsNull() {
        assertNull(DomainSuffixResolver.suffixFromFQDN("."));
    }

    @Test
    public void testMultiSeparatorInStringResultIsNull() {
        assertNull(DomainSuffixResolver.suffixFromFQDN("....."));
    }

    @Test
    public void testCorrectResult1() {
        assertTrue(DomainSuffixResolver.suffixFromFQDN("foo.bar.com").compareTo("bar.com") == 0);
    }

    @Test
    public void testCorrectResult2() {
        assertTrue(DomainSuffixResolver.suffixFromFQDN("bar.com").compareTo("com") == 0);
    }

    @Test
    public void testCorrectResult3() {
        assertTrue(DomainSuffixResolver.suffixFromFQDN(".bar.com").compareTo("bar.com") == 0);
    }

    @Test
    public void testCorrectResult4() {
        assertTrue(DomainSuffixResolver.suffixFromFQDN(".com").compareTo("com") == 0);
    }

    @Test
    public void testNullHostResultsInNull() {
        assertNull(DomainSuffixResolver.suffixFromFQDN(null));

    }
}