package com.checkmarx;

import java.net.SocketException;
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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CmdTest {

    Options _opts;

    private CmdTest() {
        _opts = new Options();

        _opts.addOption("?", "help", false, "Print argument usage help.");
        _opts.addOption("h", "host", true, "Override the default hostname used to find where the updates are hosted."
                + "  Defaults to \"" + UpdateHostChecker.getDefaultHostname() + "\".");

        _opts.addOption("sl", "skipLocal", false,
                "Skip domain suffix resolution of domain suffixes assigned to network adapters on local machine.");

        _opts.addOption(Option.builder("r").longOpt("regex").required().desc(
                "The regular expression used to detect matches in listing entries" + " returned from the update host.")
                .hasArg().build());

        _opts.addOption(Option.builder().longOpt("timeout").hasArg()
                .desc("Timeout for discovering available updates. Default: 60 seconds").argName("seconds").build());

        _opts.addOption(Option.builder("d")
                .desc("Add search domain suffix. Maybe repeated, also accepts multiple arguments.").hasArgs().build());

    }

    private boolean _updateResponse = false;
    private LatestVersion _responseVersion = null;
    private long _timeoutSeconds = 60;

    private void updateCheckCallback(LatestVersion v) {
        _updateResponse = true;
        _responseVersion = v;
    }

    private UpdateHostChecker hostCheckerFactory(CommandLine cmd) throws MisconfiguredException, BadBuilderException {
        UpdateHostChecker.Builder builderInst = UpdateHostChecker.builder();

        if (cmd.hasOption("h"))
            builderInst = builderInst.withUpdateHostName(cmd.getOptionValue("h"));

        if (cmd.hasOption("d")) {
            for (String domainSuffix : cmd.getOptionValues("d"))
                builderInst = builderInst.addSearchDomainSuffix(domainSuffix);
        }

        if (cmd.hasOption("timeout"))
            _timeoutSeconds = Long.parseLong(cmd.getOptionValue("timeout"));

        Iterable<String> localSuffixes = null;

        if (!cmd.hasOption("skipLocal"))
            try {
                localSuffixes = DomainSuffixResolver.resolveLocalDomainSuffixes();
            } catch (SocketException e1) {
                System.err.println("Did not resolve local suffixes.");
            }

        if (localSuffixes != null)
            for (String domainSuffix : localSuffixes)
                builderInst = builderInst.addSearchDomainSuffix(domainSuffix);

        return builderInst.withFieldExtractRegex(Pattern.compile(cmd.getOptionValue("r"))).build();
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

                    if (_responseVersion.getRegexMatches().groupCount() > 0) {
                        System.out.println("Matched groups:");
                        for (int i = 0; i < _responseVersion.getRegexMatches().groupCount(); i++) {
                            System.out.println(String.format("Name: [%s] Value: [%s]",
                                    _responseVersion.getRegexMatches().group(i), "foo"));
                        }

                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException te) {
                System.err.println ("Timeout");
                f.cancel(true);
            }

        } catch (MisconfiguredException | BadBuilderException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        CmdTest inst = new CmdTest();

        inst.execute(args);

    }
}