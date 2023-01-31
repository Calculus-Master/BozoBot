package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.commands.*;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.calculusmaster.bozo.BozoBot.COMMANDS;

public class Listener extends ListenerAdapter
{
    public static void init()
    {
        CommandBozo.init();
        CommandStaffQuote.init();
        CommandRandomWackyLoadout.init();
        CommandRandomWackyName.init();
        CommandQuestions.init();
        CommandDev.init();
    }

    private CommandData findCommandData(Predicate<CommandData> predicate)
    {
        return COMMANDS.stream().filter(predicate).findFirst().orElse(null);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.getCommandName().equals(event.getName()));

        if(data == null)
        {
            BozoLogger.error(Listener.class, "Slash Command not found: " + event.getName());
            event.reply("An error has occurred.").setEphemeral(true).queue();
        }
        else if(!(event.getChannel() instanceof TextChannel))
        {
            BozoLogger.warn(Listener.class, "Attempted use of Slash Command (%s) used in non-TextChannel (%s, Type: %s)".formatted(event.getName(), event.getChannel().getName(), event.getChannel().getType()));
            event.reply("Slash Command usage outside of standard Text Channels is not currently supported.").setEphemeral(true).queue();
        }
        else
        {
            BozoLogger.info(Listener.class, "Parsing Slash Command: /" + event.getFullCommandName() + " " + event.getOptions().stream().map(o -> o.getName() + ":" + o.getAsString()).collect(Collectors.joining(" ")));
            data.getInstance().parseSlashCommand(event);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.getCommandName().equals(event.getName()));

        if(data == null) BozoLogger.error(Listener.class, "Autocomplete Slash Command not found: " + event.getName());
        else data.getInstance().parseAutocomplete(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.hasButton(event.getComponentId()));

        if(data == null)
        {
            BozoLogger.error(Listener.class, "Button ID not found: " + event.getComponentId());
            event.reply("An error has occurred.").setEphemeral(true).queue();
        }
        else data.getInstance().parseButtonInteraction(event);
    }
}
