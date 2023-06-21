package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.List;
import java.util.Random;

public class CommandBozo extends Command
{
    public static void init()
    {
        CommandData
                .create("bozo")
                .withConstructor(CommandBozo::new)
                .withCommand(Commands
                        .slash("bozo", "Are you a bozo?")
                        .addOption(OptionType.USER, "user", "[Optional] Check the bozo status of a user.", false)
                )
                .setNotOnlyBozocord()
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        OptionMapping userOption = event.getOption("user");

        Random r = new Random();
        boolean isBozo = r.nextFloat() < 0.499F;
        List<String> permanentBozos = List.of("1069804190458708049", "490401640843706368");

        if(permanentBozos.contains(event.getUser().getId()) || (userOption != null && permanentBozos.contains(userOption.getAsUser().getId())))
            isBozo = true;

        this.response = isBozo
                ? (userOption == null ? "You are a %s!".formatted(r.nextFloat() < 0.05F ? "ozob" : "bozo") : userOption.getAsUser().getAsMention() + " is a %s!".formatted(r.nextFloat() < 0.05F ? "ozob" : "bozo"))
                : (userOption == null ? "You are **not** a bozo!" : userOption.getAsUser().getAsMention() + " is **not** a bozo!");

        return true;
    }
}
