package com.checkmarx;

import java.net.SocketException;
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

        _opts.addOption(Option.builder("d")
                .desc("Add search domain suffix. Maybe repeated, also accepts multiple arguments.").hasArgs().build());

    }

    private void updateCheckCallback(LatestVersion v) {
        System.out.println("Latest version response detected.");
        System.out.println(v);
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

        UpdateHostChecker.Builder builderInst = UpdateHostChecker.builder();

        if (cmd.hasOption("h"))
            builderInst = builderInst.withUpdateHostName(cmd.getOptionValue("h"));

        if (cmd.hasOption("d")) {
            for (String domainSuffix : cmd.getOptionValues("d"))
                builderInst = builderInst.addSearchDomainSuffix(domainSuffix);
        }

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

        try {
            UpdateHostChecker checkerInst = builderInst.withFieldExtractRegex(Pattern.compile(cmd.getOptionValue("r")))
                    .build();

            checkerInst.checkForUpdates(this::updateCheckCallback);

        } catch (MisconfiguredException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadBuilderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        CmdTest inst = new CmdTest();

        inst.execute(args);

    }
}