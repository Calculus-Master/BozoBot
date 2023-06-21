package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandOhHeavens extends Command
{
    public static void init()
    {
        CommandData
                .create("ohheavens")
                .withConstructor(CommandOhHeavens::new)
                .withCommand(Commands
                        .slash("ohheavens", "( ͡° ͜ʖ ͡°)")
                )
                .setNotOnlyBozocord()
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        this.embed.setImage("https://cdn.discordapp.com/attachments/1019727118868938922/1069999092836614234/IMG_20230128_200258_428.jpg");
        return true;
    }
}
