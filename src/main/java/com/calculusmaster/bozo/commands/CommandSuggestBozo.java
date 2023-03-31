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

            String yes = "<:thunmb:1037162867276906557>";
            String yesNoRole = "<:thonkvitor:1072993154468425839>";
            String no = "<:JongoNuhUh:1089413964745683006>";

            event.getChannel().sendMessage("Should **" + targetUser + "** become a bozo? (Suggestion By: " + voter + ")\n").queue(m -> {
                m.addReaction(Emoji.fromFormatted(yes)).queue();
                m.addReaction(Emoji.fromFormatted(yesNoRole)).queue();
                m.addReaction(Emoji.fromFormatted(no)).queue();
                m.pin().queue();
            });
        }

        return true;
    }
}
