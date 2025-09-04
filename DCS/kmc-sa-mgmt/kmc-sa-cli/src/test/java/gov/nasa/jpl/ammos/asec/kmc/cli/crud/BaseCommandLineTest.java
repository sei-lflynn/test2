package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.sadb.BaseH2Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;

public class BaseCommandLineTest extends BaseH2Test {
    public CommandLine getCmd(Callable<Integer> app, boolean overrideOutput) {
        CommandLine cmd = new CommandLine(app);
        if (overrideOutput) {
            PrintWriter writer = new PrintWriter(new StringWriter());
            cmd.setOut(writer);
            cmd.setErr(writer);
        }
        return cmd;
    }

    public CommandLine getCmd(Callable<Integer> app, boolean overrideOutput, PrintWriter out, PrintWriter err) {
        CommandLine cmd = new CommandLine(app);
        if (overrideOutput) {
            if (out == null) {
                out = new PrintWriter(new StringWriter());
            }
            cmd.setOut(out);
            if (err == null) {
                err = new PrintWriter(new StringWriter());
            }
            cmd.setErr(err);
        }
        return cmd;
    }
}
