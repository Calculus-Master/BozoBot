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

import java.util.*;
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
        CommandSuggestBozo.init();
        CommandLFG.init();
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

        String content = event.getMessage().getContentRaw().toLowerCase();

        if(r.nextFloat() < 0.2F && content.contains("rose"))
        {
            event.getChannel().sendMessage("\"Strong hands he held a " + (r.nextFloat() < 0.2F ? "cock" : "rose") +  ",aura burn bright.\" - " + event.getMember().getEffectiveName()).queue();
        }

        if(!event.getAuthor().isBot() && r.nextFloat() < 0.75F && content.contains("upended"))
        {
            if(r.nextFloat() < 0.8F)
            {
                event.getChannel().sendMessage("***THE UPENDED***").queue();
                event.getChannel().sendMessage("***A TITLE SUITABLE FOR THAT WHICH TURNS WORLDS UPSIDE DOWN***").queue();
            }
            else
            {
                event.getChannel().sendMessage("***THE APPENDED***").queue();
                event.getChannel().sendMessage("***A TITLE SUITABLE FOR THAT WHICH EXTENDS***").queue();
            }
        }

        if(r.nextInt(8192) == 0) event.getMessage().pin().queue();
        else if(!event.getAuthor().isBot() && r.nextFloat() < 0.05)
        {
            List<String> oneWordResponses = List.of("yeah", "no", "L", "lol", "true", "cringe", "based", "smh", "wow", "ok", "bruh", "bozo", ":)", "wrong", "whar", "real", "simp", "mid", "hi", "perfect", "interesting", "lmao", "heh", "ikr", "bye");

            if(event.getAuthor().getId().equals("490401640843706368") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("grape").queue();
            else if(event.getAuthor().getId().equals("776195690149576704") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("ok pvp bot").queue();
            else if(event.getAuthor().getId().equals("429601532363931659") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("it really is that shrimple").queue();
            else if(event.getAuthor().getId().equals("160843328898727936") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("ikr, hunters are so mid").queue();
            else if(event.getAuthor().getId().equals("752237938779226173") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("chainsword>chainsawman").queue();
            else if(event.getAuthor().getId().equals("274068634798915584") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("misinfo!!!!!").queue();
            else if(event.getAuthor().getId().equals("445222471332003840") && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage("demo best perk").queue();
            else if(Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(role -> role.getId().equals("1015047797420085329")) && r.nextFloat() < 0.05F)
                event.getChannel().sendMessage("join clan bozo").queue();
            else event.getChannel().sendMessage(oneWordResponses.get(r.nextInt(oneWordResponses.size()))).queue();
        }
        else if(event.getAuthor().isBot() && event.getAuthor().getId().equals("1069804190458708049") && r.nextFloat() < 0.05F && !content.equalsIgnoreCase("best bot"))
            event.getChannel().sendMessage("best bot").queue();
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
