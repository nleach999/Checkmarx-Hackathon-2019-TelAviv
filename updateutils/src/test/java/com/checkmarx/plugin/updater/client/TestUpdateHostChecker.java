package com.checkmarx.plugin.updater.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.checkmarx.plugin.updater.client.exceptions.BadBuilderException;
import com.checkmarx.plugin.updater.client.exceptions.MisconfiguredException;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestUpdateHostChecker {
    private static Pattern _testRegex;

    @BeforeClass
    public static void initTests() {
        _testRegex = Pattern.compile(
                "(?<filename>.+)_(?<major>\\d{2})\\.(?<minor>\\d{2})\\.(?<revision>\\d{2})_{0,1}(?<custom>.+)?\\.zip");

    }

    @Test
    public void TestNoSearchSuffixesThrowsExceptionOnBuild() {

        try {

            UpdateHostChecker inst = UpdateHostChecker.builder().withFieldExtractRegex(_testRegex).build();
        } catch (MisconfiguredException ex) {
            assertTrue(true);
            return;
        } catch (Exception ex) {

        }

        fail();
    }

    @Test
    public void TestDoubleBuildInvokeThrowsException() {
        try {

            UpdateHostChecker.Builder.RegexValidatingBuilder inst = UpdateHostChecker.builder()
                    .addSearchDomainSuffix("foo.com").withFieldExtractRegex(_testRegex);

            inst.build();
            inst.build();

        } catch (BadBuilderException ex) {
            assertTrue(true);
            return;
        } catch (Exception ex) {

        }

        fail();
    }

    boolean flag = false;

    @Test
    public void TestSyncCallFlagMakesSyncCall() {
        try {

            flag = false;

            UpdateHostChecker inst = UpdateHostChecker.builder().checkSynchronously().addSearchDomainSuffix("foo.com")
                    .withFieldExtractRegex(_testRegex).build();

            inst.checkForUpdates((foo) -> {
                flag = true;
            });

            assertTrue(flag);
        } catch (Exception ex) {

            fail();
            return;
        }

    }

    private String comparator(Dictionary<String, Matcher> foundFiles) {

        TreeSet<String> sortedFilenames = new TreeSet<String>();

        Enumeration<String> keys = foundFiles.keys();

        while (keys.hasMoreElements())
            sortedFilenames.add(keys.nextElement());

        return sortedFilenames.last();
    }

    @Test
    public void Test() {
        try {
            UpdateHostChecker inst = UpdateHostChecker.builder().checkSynchronously()
                    .withUpdateHostName("ec2-18-218-216-109")
                    .addSearchDomainSuffix(".us-east-2.compute.amazonaws.com:8085").withFieldExtractRegex(_testRegex)
                    .build();

            inst.checkForUpdates(this::comparator, (foo) -> {
                int x = 0;
            });

        } catch (MisconfiguredException | BadBuilderException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestAsyncCheck() {
        fail();
    }

    @Test
    public void TestMaxRetriesAreAttempted() {
        fail();
    }

    @Test
    public void TestRetryDelayIsHonored() {
        fail();
    }

}