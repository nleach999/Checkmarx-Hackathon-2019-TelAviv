package com.checkmarx;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import com.checkmarx.plugin.updater.client.LatestVersion;
import com.checkmarx.plugin.updater.client.UpdateHostChecker;
import com.checkmarx.plugin.updater.client.exceptions.BadBuilderException;
import com.checkmarx.plugin.updater.client.exceptions.MisconfiguredException;
import com.checkmarx.plugin.updater.netutils.DomainSuffixResolver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CmdTest {

    private static final String OPTION_HELP_SHRT = "?";
    private static final String OPTION_HELP_LONG = "help";
    private static final String OPTION_HELP = OPTION_HELP_SHRT;

    private static final String OPTION_KEY_FILE_LONG = "key-file";
    private static final String OPTION_KEY_FILE = OPTION_KEY_FILE_LONG;

    private static final String OPTION_SKIP_VERIFY_LONG = "skip-verify";
    private static final String OPTION_SKIP_VERIFY = OPTION_SKIP_VERIFY_LONG;

    private static final String OPTION_MAX_DL_LONG = "max-dl-mbytes";
    private static final String OPTION_MAX_DL = OPTION_MAX_DL_LONG;

    private static final String OPTION_NO_DOWNLOAD_LONG = "no-download";
    private static final String OPTION_NO_DOWNLOAD = OPTION_NO_DOWNLOAD_LONG;

    private static final String OPTION_REGEX_GROUP_SHRT = "g";
    private static final String OPTION_REGEX_GROUP_LONG = "regex-group";
    private static final String OPTION_REGEX_GROUP = OPTION_REGEX_GROUP_SHRT;

    private static final String OPTION_DOMAIN_SHRT = "d";
    private static final String OPTION_DOMAIN_LONG = "domain";
    private static final String OPTION_DOMAIN = OPTION_DOMAIN_SHRT;

    private static final String OPTION_TIMEOUT_SHRT = "t";
    private static final String OPTION_TIMEOUT_LONG = "timeout";
    private static final String OPTION_TIMEOUT = OPTION_TIMEOUT_SHRT;

    private static final String OPTION_REGEX_PROPS_LONG = "regex-props";
    private static final String OPTION_REGEX_PROPS = OPTION_REGEX_PROPS_LONG;

    private static final String OPTION_REGEX_NAME_LONG = "regex-name";
    private static final String OPTION_REGEX_NAME = OPTION_REGEX_NAME_LONG;

    private static final String OPTION_REGEX_LONG = "regex";
    private static final String OPTION_REGEX = OPTION_REGEX_LONG;

    private static final String OPTION_SKIP_LOCAL_SHRT = "s";
    private static final String OPTION_SKIP_LOCAL_LONG = "skip-local";
    private static final String OPTION_SKIP_LOCAL = OPTION_SKIP_LOCAL_SHRT;

    private static final String OPTION_HOST_SHRT = "h";
    private static final String OPTION_HOST_LONG = "host";
    private static final String OPTION_HOST = OPTION_HOST_SHRT;

    Options _opts;

    private CmdTest() {
        _opts = new Options();

        _opts.addOption(OPTION_HELP_SHRT, OPTION_HELP_LONG, false, "Print argument usage help.");
        _opts.addOption(OPTION_HOST_SHRT, OPTION_HOST_LONG, true,
                "Override the default hostname used to find where the updates are hosted. Defaults to \""
                        + UpdateHostChecker.getDefaultHostname() + "\".");

        _opts.addOption(OPTION_SKIP_LOCAL_SHRT, OPTION_SKIP_LOCAL_LONG, false,
                "Skip domain suffix resolution of domain suffixes assigned to network adapters on local machine.");

        OptionGroup regexOpts = new OptionGroup();
        regexOpts.addOption(Option.builder().longOpt(OPTION_REGEX_LONG).required()
                .desc("The regular expression used to detect matches in listing entries returned from the update host.")
                .hasArg().build());
        regexOpts.addOption(Option.builder().longOpt(OPTION_REGEX_NAME_LONG).required().desc(
                "The name of the property holding the regular expression in the regular expression properties file.")
                .hasArg().build());
        _opts.addOptionGroup(regexOpts);

        _opts.addOption(Option.builder().longOpt(OPTION_REGEX_PROPS_LONG).hasArg().argName("file path")
                .desc("A path to a .properties file containing regular expressions assigned to individual properties.")
                .build());

        _opts.addOption(Option.builder(OPTION_TIMEOUT_SHRT).longOpt(OPTION_TIMEOUT_LONG).hasArg()
                .desc("Timeout for discovering available updates. Default: 60 seconds").argName("seconds").build());

        _opts.addOption(Option.builder(OPTION_DOMAIN_SHRT).longOpt(OPTION_DOMAIN_LONG)
                .desc("Add search domain suffix. Maybe repeated, also accepts multiple arguments.").hasArgs()
                .argName("domain suffix").build());

        _opts.addOption(Option.builder(OPTION_REGEX_GROUP_SHRT).longOpt(OPTION_REGEX_GROUP_LONG)
                .desc("A named group extracted with the regex match. This will be displayed if group matches are found."
                        + " Maybe repeated, also accepts multiple arguments.")
                .hasArgs().argName("group name").build());

        _opts.addOption(Option.builder().hasArg(false).longOpt(OPTION_NO_DOWNLOAD_LONG)
                .desc("Do not download the latest version of the plugin detected through the version resolution.")
                .build());

        _opts.addOption(Option.builder().hasArg().longOpt(OPTION_MAX_DL_LONG).argName("megabytes")
                .desc("The maximum size, in megabytes, for the plugin to allow download.").build());

        OptionGroup signatureGroup = new OptionGroup();
        signatureGroup.addOption(Option.builder().longOpt(OPTION_SKIP_VERIFY_LONG).hasArg(false)
                .desc("Skip the signature verification of the downloaded payload.").build());
        signatureGroup.addOption(Option.builder().hasArg().longOpt(OPTION_KEY_FILE_LONG)
                .desc("A path to a file with a public key used to verify the signature of the plugin.").build());
        _opts.addOptionGroup(signatureGroup);

    }

    private boolean _updateResponse = false;
    private LatestVersion _responseVersion = null;
    private long _timeoutSeconds = 60;

    private void updateCheckCallback(LatestVersion v) {
        _updateResponse = true;
        _responseVersion = v;
    }

    private UpdateHostChecker hostCheckerFactory(CommandLine cmd)
            throws MisconfiguredException, BadBuilderException, IOException {
        UpdateHostChecker.Builder builderInst = UpdateHostChecker.builder();

        if (cmd.hasOption(OPTION_HOST))
            builderInst.withUpdateHostName(cmd.getOptionValue(OPTION_HOST));

        if (cmd.hasOption(OPTION_DOMAIN)) {
            for (String domainSuffix : cmd.getOptionValues(OPTION_DOMAIN))
                builderInst.addSearchDomainSuffix(domainSuffix);
        }

        if (cmd.hasOption(OPTION_TIMEOUT))
            _timeoutSeconds = Long.parseLong(cmd.getOptionValue(OPTION_TIMEOUT));

        Iterable<String> localSuffixes = null;

        if (!cmd.hasOption(OPTION_SKIP_LOCAL))
            try {
                localSuffixes = DomainSuffixResolver.resolveLocalDomainSuffixes();
            } catch (SocketException e1) {
                System.err.println("Did not resolve local suffixes.");
            }

        if (localSuffixes != null)
            for (String domainSuffix : localSuffixes)
                builderInst.addSearchDomainSuffix(domainSuffix);

        Pattern regexPattern = null;

        if (cmd.hasOption(OPTION_REGEX)) {
            regexPattern = Pattern.compile(cmd.getOptionValue(OPTION_REGEX));
        } else if (cmd.hasOption(OPTION_REGEX_NAME) && cmd.hasOption(OPTION_REGEX_PROPS)) {
            FileInputStream f = new FileInputStream(cmd.getOptionValue(OPTION_REGEX_PROPS));

            Properties props = new Properties();
            props.load(f);
            f.close();

            regexPattern = Pattern.compile(props.getProperty(cmd.getOptionValue(OPTION_REGEX_NAME)));
        }

        return builderInst.withFieldExtractRegex(regexPattern).build();
    }

    private void execute(String[] args) {
        CommandLineParser p = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = p.parse(_opts, args);
        } catch (ParseException ex) {
            cmd = null;
            System.err.println(ex.getMessage());
        }

        if (cmd == null || cmd.hasOption("?") || cmd.getOptions().length == 0) {
            formatter.printHelp("cmdtest [OPTIONS]", _opts);
            return;
        }

        try {
            UpdateHostChecker checkerInst = hostCheckerFactory(cmd);

            Future<?> f = checkerInst.checkForUpdates(this::updateCheckCallback);

            System.out.println("Waiting for update check to respond....");

            try {

                f.get(_timeoutSeconds, TimeUnit.SECONDS);

                if (f.isDone() && !_updateResponse) {
                    System.out.println("Update check completed, no response.");
                } else {
                    System.out.println("Latest version retrieved.");
                    System.out.println(_responseVersion);

                    if (_responseVersion.getRegexMatches().groupCount() > 0 && cmd.hasOption(OPTION_REGEX_GROUP)) {
                        System.out.println("Matched groups:");
                        for (String groupName : cmd.getOptionValues(OPTION_REGEX_GROUP)) {
                            System.out.println(String.format("Name: [%s] Value: [%s]", groupName,
                                    _responseVersion.getRegexMatches().group(groupName)));
                        }

                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException te) {
                System.err.println("Timeout");
                // f.cancel(true);
                checkerInst.forceShutdown();
            }

        } catch (MisconfiguredException | BadBuilderException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    public static void main(String[] args) {
        CmdTest inst = new CmdTest();

        inst.execute(args);

    }
}