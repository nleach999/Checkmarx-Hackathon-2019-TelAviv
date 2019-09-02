package com.checkmarx.plugin.updater.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
            UpdateHostChecker.builder().withFieldExtractRegex(_testRegex).build();
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
}