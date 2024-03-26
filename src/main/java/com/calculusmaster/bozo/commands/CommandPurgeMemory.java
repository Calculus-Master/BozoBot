package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.ClaudeInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.List;
import java.util.Random;

public class CommandPurgeMemory extends Command
{
    public static void init()
    {
        CommandData
                .create("purge")
                .withConstructor(CommandPurgeMemory::new)
                .withCommand(Commands
                        .slash("purge", "Purge AI memory.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        ClaudeInterface.MESSAGES.clear();

        this.response = "Cleared!";
        return true;
    }
}
