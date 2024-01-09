package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.BingoManager;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CommandBingo extends Command
{
    public static void init()
    {
        CommandData
                .create("bingo")
                .withConstructor(CommandBingo::new)
                .withCommand(Commands
                        .slash("bingo", "Bingo board stuff.")
                        .addSubcommands(
                                new SubcommandData("add", "Add an entry to the list of possible Bingo Board entries.")
                                        .addOption(OptionType.STRING, "entry", "The entry to add.", true),
                                new SubcommandData("delete", "Remove an entry from the list of possible Bingo Board entries.")
                                        .addOption(OptionType.INTEGER, "number", "The entry number to remove. Use /bingo list to see the entries.", true),
                                new SubcommandData("edit", "Edit an entry in the list of possible Bingo Board entries.")
                                        .addOption(OptionType.INTEGER, "number", "The entry number to edit. Use /bingo list to see the entries.", true)
                                        .addOption(OptionType.STRING, "entry", "The new entry.", true),
                                new SubcommandData("list", "List all possible Bingo Board entries.")
                                        .addOption(OptionType.STRING, "search", "Search for entries that contain this string.", false),
                                new SubcommandData("complete", "Mark a square as completed.")
                                        .addOption(OptionType.STRING, "square", "The square (A1, B3, etc.) to mark as completed. The letter is the row and the number is the column.", true, true),
                                new SubcommandData("board", "View the current Bingo Board."),
                                new SubcommandData("add-free-space", "Set a Bingo entry as a free space.")
                                        .addOption(OptionType.INTEGER, "number", "The entry number to set as free. Use /bingo list to see the entries.", true),
                                new SubcommandData("remove-free-space", "Remove a Bingo entry as a free space.")
                                        .addOption(OptionType.INTEGER, "number", "The entry number to remove as free. Use /bingo list-free-spaces to see the entries.", true),
                                new SubcommandData("list-free-spaces", "List all entries that can be free spaces.")
                        )
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        String subcommand = Objects.requireNonNull(event.getSubcommandName());

        if(subcommand.equals("add"))
        {
            String entry = Objects.requireNonNull(event.getOption("entry")).getAsString();

            entry = entry
                    .replaceAll("(?<!\\\\)#", "\\\\#")
                    .replaceAll("(?<!\\\\)_", "\\\\_")
                    .replaceAll("(?<!\\\\)%", "\\\\%");

            BingoManager.addEntry(entry);

            this.response = "Added `" + entry + "` to the Bingo Board.";
        }
        else if(subcommand.equals("delete"))
        {
            int index = Objects.requireNonNull(event.getOption("number")).getAsInt() - 1;

            if(index < 0 || index >= BingoManager.ENTRIES.size()) return this.error("Invalid index!");

            String removed = BingoManager.getEntry(index);
            BingoManager.removeEntry(index);

            this.response = "Removed `" + removed + "` (#" + (index + 1) + ") from the Bingo Board.";
        }
        else if(subcommand.equals("edit"))
        {
            int index = Objects.requireNonNull(event.getOption("number")).getAsInt() - 1;
            String entry = Objects.requireNonNull(event.getOption("entry")).getAsString();

            if(index < 0 || index >= BingoManager.ENTRIES.size()) return this.error("Invalid index!");

            String old = BingoManager.getEntry(index);
            BingoManager.editEntry(index, entry);

            this.response = "Edited `" + old + "` (#" + (index + 1) + ") to `" + entry + "` in the Bingo Board.";
        }
        else if(subcommand.equals("list"))
        {
            if(BingoManager.ENTRIES.isEmpty()) return this.error("No entries in the Bingo Board!");

            String search = event.getOption("search") != null ? event.getOption("search").getAsString() : "";

            List<String> list = new ArrayList<>();

            for(int i = 0; i < BingoManager.ENTRIES.size(); i++)
            {
                String e = BingoManager.ENTRIES.get(i);

                if(search.isEmpty() || e.toLowerCase().contains(search.toLowerCase()))
                    list.add((i + 1) + ": " + e);
            }

            String out = String.join("\n", list);

            List<String> split = SplitUtil.split(out, 2000, SplitUtil.Strategy.NEWLINE);

            //Send first as a reply, send remaining as messages
            event.reply(split.get(0)).queue();
            for(int i = 1; i < split.size(); i++)
                event.getChannel().sendMessage(split.get(i)).queue();
        }
        else if(subcommand.equals("complete"))
        {
            String square = Objects.requireNonNull(event.getOption("square")).getAsString();

            if(!BingoManager.isValidSquare(square))
                return this.error("Invalid square! Valid inputs are A1, B3, C5, D2, etc. Letters (rows) are A-E and numbers (columns) are 1-5.");

            int[] coords = BingoManager.parseSquareCoordinate(square);

            if(BingoManager.isSquareCompleted(coords[0], coords[1]))
                return this.error("Square already completed!");

            BingoManager.completeSquare(square, coords[0], coords[1]);

            this.response = "Marked `" + square + "` as completed! Use `/bingo board` to see the updated board.";
        }
        else if(subcommand.equals("board"))
        {
            BingoManager.sendBingoBoard();

            this.response = "Sent!";
            this.ephemeral = true;
        }
        else if(subcommand.equals("add-free-space"))
        {
            int index = Objects.requireNonNull(event.getOption("number")).getAsInt() - 1;

            if(index < 0 || index >= BingoManager.ENTRIES.size()) return this.error("Invalid index!");

            String entry = BingoManager.getEntry(index);

            if(BingoManager.FREE_SPACES.contains(index)) return this.error("This entry is already marked as a possible free space!");

            BingoManager.addFreeSpace(index);

            this.response = "Added `" + entry + "` (#" + (index + 1) + ") to the list of possible free spaces.";
        }
        else if(subcommand.equals("remove-free-space"))
        {
            int index = Objects.requireNonNull(event.getOption("number")).getAsInt() - 1;

            if(index < 0 || index >= BingoManager.ENTRIES.size()) return this.error("Invalid index!");

            String entry = BingoManager.getEntry(index);

            if(!BingoManager.FREE_SPACES.contains(index)) return this.error("This entry is not marked as a free space!");

            BingoManager.removeFreeSpace(index);

            this.response = "Removed `" + entry + "` (#" + (index + 1) + ") from the list of free spaces.";
        }
        else if(subcommand.equals("list-free-spaces"))
        {
            if(BingoManager.FREE_SPACES.isEmpty()) return this.error("No entries in the list of free spaces! Any entry can be a free space.");

            List<String> list = new ArrayList<>();

            for(int i : BingoManager.FREE_SPACES)
                list.add((i + 1) + ": " + BingoManager.ENTRIES.get(i));

            String out = String.join("\n", list);

            List<String> split = SplitUtil.split(out, 2000, SplitUtil.Strategy.NEWLINE);

            //Send first as a reply, send remaining as messages
            event.reply(split.get(0)).queue();
            for(int i = 1; i < split.size(); i++)
                event.getChannel().sendMessage(split.get(i)).queue();
        }
        else return this.error("Invalid subcommand!");

        return true;
    }

    @Override
    protected boolean autocompleteLogic(CommandAutoCompleteInteractionEvent event)
    {
        if(event.getFocusedOption().getName().equals("square"))
        {
            List<String> squares = List.of("A1", "A2", "A3", "A4", "A5",
                    "B1", "B2", "B3", "B4", "B5",
                    "C1", "C2", "C3", "C4", "C5",
                    "D1", "D2", "D3", "D4", "D5",
                    "E1", "E2", "E3", "E4", "E5");

            String input = event.getFocusedOption().getValue();

            event.replyChoiceStrings(squares.stream().filter(s -> s.startsWith(input)).toList()).queue();
            return true;
        }
        else return this.error("Something went wrong.");
    }
}
