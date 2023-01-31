package com.calculusmaster.bozo.commands.core;

import com.calculusmaster.bozo.BozoBot;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;
import java.util.function.Supplier;

public class CommandData
{
    //Core
    private final String commandName;
    private Supplier<Command> supplier;
    private SlashCommandData slashCommandData;

    private List<String> buttonIDs;

    private CommandData(String commandName)
    {
        this.commandName = commandName;
        this.supplier = null;
        this.slashCommandData = null;

        this.buttonIDs = List.of();
    }

    public void register()
    {
        BozoBot.COMMANDS.add(this);
    }

    public static CommandData create(String commandName)
    {
        return new CommandData(commandName);
    }

    public CommandData withConstructor(Supplier<Command> supplier)
    {
        this.supplier = supplier;
        return this;
    }

    public CommandData withCommand(SlashCommandData slashCommandData)
    {
        this.slashCommandData = slashCommandData;
        return this;
    }

    public CommandData withButtons(String... buttonIDs)
    {
        this.buttonIDs = List.of(buttonIDs);
        return this;
    }

    public SlashCommandData getSlashCommandData()
    {
        return this.slashCommandData;
    }

    public boolean hasButton(String buttonID)
    {
        return this.buttonIDs.contains(buttonID) || this.buttonIDs.stream().anyMatch(buttonID::contains);
    }

    public Command getInstance()
    {
        Command instance = this.supplier.get();
        instance.setCommandData(this);
        return instance;
    }

    public String getCommandName()
    {
        return this.commandName;
    }
}
