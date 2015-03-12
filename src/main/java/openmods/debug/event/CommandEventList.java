package openmods.debug.event;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import net.minecraft.command.*;
import net.minecraft.util.ChatComponentTranslation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CommandEventList implements ICommand {

    private interface StringOutput {
        public void output(ICommandSender sender);
    }

    private final StringOutput log_output = new StringOutput() {
        @Override
        public void output(ICommandSender sender) {
            String data = dataToString();
            Log.info("Event debug:\n %s", data);
            sender.addChatMessage(new ChatComponentTranslation("openmods.debug.log.log_ok"));
        }
    };

    private final StringOutput clipboard_output = new StringOutput() {
        @Override
        public void output(ICommandSender sender) {
            String data = dataToString();

            try {
                StringSelection selection = new StringSelection(data);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            } catch (Exception e) {
                Log.warn(e, "Failed to copy data to clipboard");
                throw new CommandException("openmods.debug.log.clipboard_fail");
            }

            sender.addChatMessage(new ChatComponentTranslation("openmods.debug.log.clipboard_ok"));
        }
    };

    private final StringOutput file_output = new StringOutput() {
        @Override
        public void output(ICommandSender sender) {
            try {
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
                String filename = "event-debug-" + formatter.format(new Date()) + ".txt";
                File file = new File(filename);
                OutputStream os = new FileOutputStream(file);
                PrintWriter writer = new PrintWriter(os);
                try {
                    collector.visitStats(new EventStatsPrinter(writer));
                } finally {
                    writer.close();
                }
                sender.addChatMessage(new ChatComponentTranslation("openmods.debug.log.file_ok", file.getAbsolutePath()));
            } catch (Exception e) {
                Log.warn(e, "Failed to store data");
                throw new CommandException("openmods.debug.log.file_fail");
            }
        }
    };

    private final Map<String, StringOutput> outputs = ImmutableMap.of(
            "log", log_output,
            "clipboard", clipboard_output,
            "file", file_output
            );

    private final EventCollector collector;
    private final String name;
    private final boolean restricted;

    public CommandEventList(String name, boolean restricted, EventCollector collector) {
        this.collector = collector;
        this.name = name;
        this.restricted = restricted;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return name + " <" + Joiner.on('|').join(outputs.keySet()) + ">";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final StringOutput output;
        if (args.length == 0) {
            output = file_output;
        } else if (args.length == 1) {
            String outputArg = args[0];
            output = outputs.get(outputArg);
            if (output == null) {
                final String alts = Joiner.on(',').join(outputs.keySet());
                throw new CommandException("openmods.debug.log.unknown_output", alts);
            }
        } else {
            throw new SyntaxErrorException();
        }

        output.output(sender);
    }

    private String dataToString() {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        collector.visitStats(new EventStatsPrinter(writer));
        writer.close();

        String data = buffer.toString();
        return data;
    }

    private static List<String> filterPrefixes(String prefix, Collection<String> proposals) {
        prefix = prefix.toLowerCase();

        List<String> result = Lists.newArrayList();
        for (String s : proposals)
            if (s.startsWith(prefix))
                result.add(s);

        return result;
    }

    @Override
    public List<?> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return (args.length == 1) ? filterPrefixes(args[0], outputs.keySet()) : null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        return name.compareTo(((ICommand)o).getCommandName());
    }

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public List<?> getCommandAliases() {
        return null;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        int level = restricted ? 4 : 0;
        return sender.canCommandSenderUseCommand(level, name);
    }

}
