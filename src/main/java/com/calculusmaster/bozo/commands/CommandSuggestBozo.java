package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandSuggestBozo extends Command
{
    public static void init()
    {
        CommandData
                .create("suggestbozo")
                .withConstructor(CommandSuggestBozo::new)
                .withCommand(Commands
                        .slash("suggestbozo", "Suggest a potential bozo.")
                        .addOption(OptionType.STRING, "user", "User to suggest.", true)
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        OptionMapping suggestion = event.getOption("user");

        if(suggestion == null)
        {
            event.reply("You must specify a user to suggest!").queue();
            return false;
        }
        else
        {
            String targetUser = suggestion.getAsString();
            String voter = event.getMember().getUser().getName();

            event.reply("Setting up a vote!").setEphemeral(true).queue();

            event.getChannel().sendMessage("Should **" + targetUser + "** become a bozo? (Suggestion By: " + voter + ")").queue(m -> {
                m.addReaction(Emoji.fromFormatted("U+1F44D")).queue();
                m.addReaction(Emoji.fromFormatted("U+1F44E")).queue();
            });
        }

        return true;
    }
}
