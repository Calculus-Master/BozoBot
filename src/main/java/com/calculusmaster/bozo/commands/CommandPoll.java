package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.Poll;
import kotlin.Pair;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

public class CommandPoll extends Command
{
    public static void init()
    {
        CommandData
                .create("poll")
                .withConstructor(CommandPoll::new)
                .withCommand(Commands
                        .slash("poll", "Create a poll.")
                        .addOption(OptionType.STRING, "question", "The question you're polling for.", true)
                        .addOption(OptionType.BOOLEAN, "anonymous", "Whether or not the poll is anonymous.", false)
                        .addOption(OptionType.STRING, "time", "Time the poll ends after. Use HammerTime. If left out, results update in real time.", false)
                        .addOptions(IntStream.range(0, 10).mapToObj(i -> new OptionData(OptionType.STRING, "option" + (i + 1), "Option " + (i + 1), false)).toList()
                ))
                .setNotOnlyBozocord()
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        OptionMapping questionOption = Objects.requireNonNull(event.getOption("question"));
        String question = questionOption.getAsString();

        OptionMapping anonymousOption = event.getOption("anonymous");
        boolean anonymous = anonymousOption != null && anonymousOption.getAsBoolean();

        OptionMapping timeOption = event.getOption("time");
        String time = timeOption != null ? timeOption.getAsString() : "";

        if(!time.isEmpty() && !time.matches("<t:[0-9]*:[dDtTfFR]>")) return this.error("Time format doesn't match \"<t:[0-9]*:[dDtTfFR]>\"! Use HammerTime to copy one of its formats.", false);

        List<OptionMapping> choiceOptions = new ArrayList<>();
        for(int i = 0; i < 10; i++) choiceOptions.add(event.getOption("option" + (i + 1)));
        choiceOptions.removeIf(Objects::isNull);

        List<Pair<String, String>> options = new ArrayList<>();
        if(choiceOptions.isEmpty())
        {
            options.add(new Pair<>("<:thunmb:1037162867276906557>", "Yes"));
            options.add(new Pair<>("<:JongoNuhUh:1089413964745683006>", "No"));
        }
        else if(choiceOptions.size() == 2)
        {
            options.add(new Pair<>("<:thunmb:1037162867276906557>", choiceOptions.get(0).getAsString()));
            options.add(new Pair<>("<:JongoNuhUh:1089413964745683006>", choiceOptions.get(1).getAsString()));
        }
        else if(choiceOptions.size() == 3)
        {
            options.add(new Pair<>("<:thunmb:1037162867276906557>", choiceOptions.get(0).getAsString()));
            options.add(new Pair<>("<:thonkvitor:1072993154468425839>", choiceOptions.get(1).getAsString()));
            options.add(new Pair<>("<:JongoNuhUh:1089413964745683006>", choiceOptions.get(2).getAsString()));
        }
        else
        {
            List<String> alphaReactions = List.of("\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9", "\uD83C\uDDEA", "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF");
            for(int i = 0; i < choiceOptions.size(); i++)
                options.add(new Pair<>(alphaReactions.get(i), choiceOptions.get(i).getAsString()));
        }

        Poll poll = new Poll(question, options, time, anonymous);

        poll.sendAndUpload(event.getChannel().asTextChannel());

        this.response = "Poll created!"; this.ephemeral = true;

        return true;
    }
}
