package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.commands.*;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
        CommandOhHeavens.init();
        CommandBaltiMoments.init();
        CommandJoyBoyMoments.init();
        CommandHandsomeMoments.init();
        CommandStolasMoments.init();
        CommandToxMoments.init();
        CommandAegisMoments.init();
        CommandHorsMoments.init();
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

    private int messageCounter = 0;
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        this.messageCounter++;

        Random r = new Random();

        if(r.nextFloat() < 0.2F && event.getMessage().getContentRaw().toLowerCase().contains("rose"))
        {
            event.getChannel().sendMessage("\"Strong hands he held a rose,aura burn bright.\" - " + event.getMember().getEffectiveName()).queue();
        }

        List<String> oneWordResponses = List.of("yeah", "no", "L", "lol", "true", "cringe", "based", "smh", "wow");

        if(r.nextInt(8192) == 0) event.getMessage().pin().queue();
        else if(!event.getAuthor().isBot() && r.nextFloat() < 0.05) event.getChannel().sendMessage(oneWordResponses.get(r.nextInt(oneWordResponses.size()))).queue();
        else if(this.messageCounter >= 5 && r.nextFloat() < 0.05F)
        {
            List<String> pool = new ArrayList<>(List.of(
                    "U+1F913", //Nerd
                    "U+1F480", //Skull
                    "U+1F928", //Raised Eyebrow
                    "<:ihitchamp:990700970575036496>", //Pog
                    "<:emoji_34:1026672256866320505>", //Silly Goofy
                    "<:thonkvitor:1072993154468425839>", //Thonkvitor
                    "<:TROLLED:994095493372199012>" //TROLLED
            ));

            Collections.shuffle(pool);
            String emoji = pool.get(0);

            event.getMessage().addReaction(Emoji.fromFormatted(emoji)).queue();

            this.messageCounter = 0;
        }
    }
}
