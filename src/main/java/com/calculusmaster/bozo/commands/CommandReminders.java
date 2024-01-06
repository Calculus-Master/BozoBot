package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.Mongo;
import com.calculusmaster.bozo.util.Reminder;
import com.calculusmaster.bozo.util.ReminderManager;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.bson.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CommandReminders extends Command
{
    public static void init()
    {
        CommandData
                .create("reminders")
                .withConstructor(CommandReminders::new)
                .withCommand(Commands
                        .slash("reminders", "Create and view reminders.")
                        .addSubcommands(
                                new SubcommandData("create", "Create a reminder.")
                                        .addOption(OptionType.STRING, "content", "What to remind you.", true)
                                        .addOption(OptionType.INTEGER, "days", "Days until the reminder.", false)
                                        .addOption(OptionType.INTEGER, "hours", "Hours until the reminder.", false)
                                        .addOption(OptionType.INTEGER, "minutes", "Minutes until the reminder.", false)
                                        .addOption(OptionType.INTEGER, "seconds", "Seconds until the reminder.", false)
                                        .addOption(OptionType.STRING, "hammer-time", "HammerTime timestamp of when to remind you.", false)
                                        .addOption(OptionType.BOOLEAN, "manual-removal", "Whether this reminder is removed automatically after the time, or stays around in your list.", false)
                                        .addOption(OptionType.BOOLEAN, "dm", "Whether to DM you the reminder. If false, it'll be sent in the channel this command is used.", false),
                                new SubcommandData("list", "List all of your reminders."),
                                new SubcommandData("remove", "Removes a reminder from your list.")
                                        .addOption(OptionType.INTEGER, "number", "The number of the reminder to remove.", true)
                        )
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        String subcommand = Objects.requireNonNull(event.getSubcommandName());

        if(subcommand.equals("create"))
        {
            OptionMapping daysOption = event.getOption("days");
            OptionMapping hoursOption = event.getOption("hours");
            OptionMapping minutesOption = event.getOption("minutes");
            OptionMapping secondsOption = event.getOption("seconds");
            OptionMapping hammerTimeOption = event.getOption("hammer-time");

            if(daysOption == null && hoursOption == null && minutesOption == null && secondsOption == null && (hammerTimeOption == null || !hammerTimeOption.getAsString().matches("<t:[0-9]*:[dDtTfFR]>"))) return this.error("You must specify a time!");

            OptionMapping contentOption = Objects.requireNonNull(event.getOption("content"));

            String ht = hammerTimeOption != null ? hammerTimeOption.getAsString() : "";
            long baseTime = hammerTimeOption == null ? Instant.now().getEpochSecond() : Long.parseLong(ht.substring(ht.indexOf(":") + 1, ht.lastIndexOf(":")));

            int days = daysOption != null ? daysOption.getAsInt() : 0;
            int hours = hoursOption != null ? hoursOption.getAsInt() : 0;
            int minutes = minutesOption != null ? minutesOption.getAsInt() : 0;
            int seconds = secondsOption != null ? secondsOption.getAsInt() : 0;

            hours += days * 24;
            minutes += hours * 60;
            seconds += minutes * 60;
            baseTime += seconds;

            boolean manualRemoval = event.getOption("manual-removal") != null && event.getOption("manual-removal").getAsBoolean();
            boolean dm = event.getOption("dm") != null && event.getOption("dm").getAsBoolean();

            Reminder r = dm
                    ? new Reminder(event.getUser(), contentOption.getAsString(), baseTime, manualRemoval)
                    : new Reminder(event.getUser(), contentOption.getAsString(), baseTime, manualRemoval, event.getGuild().getId(), event.getChannel().getId());

            this.response = "Reminder created! Ends <t:%s:R>".formatted(r.getEndTimestamp());
            this.ephemeral = true;

            ReminderManager.addReminder(event.getUser().getId(), r);
        }
        else if(subcommand.equals("list"))
        {

        }
        else if(subcommand.equals("remove"))
        {

        }
        else return this.error("An error has occurred.");

        return true;
    }
}
