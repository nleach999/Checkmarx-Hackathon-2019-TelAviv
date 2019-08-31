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
                "(?<filename>TestPlugin)_(?<major>\\d{2})\\.(?<minor>\\d{2})\\.(?<revision>\\d{2})_{0,1}(?<custom>.+)?\\.zip");
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
    public void Test() {
        try {
            UpdateHostChecker inst = UpdateHostChecker.builder()
                    .withUpdateHostName("ec2-3-14-71-124")
                    .addSearchDomainSuffix(".us-east-2.compute.amazonaws.com").withFieldExtractRegex(_testRegex)
                    .build();

            inst.checkForUpdates((foo) -> {
                System.out.println (foo);
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