package com.calculusmaster.bozo.commands.core;

import com.calculusmaster.bozo.util.BozoLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class Command
{
    private CommandData commandData;

    protected User player;
    protected Guild server;
    protected TextChannel channel;

    protected EmbedBuilder embed;
    protected String response;
    protected boolean ephemeral;

    public Command()
    {
        this.player = null;
        this.server = null;
        this.channel = null;
        this.ephemeral = false;
    }

    //For Subclasses
    protected boolean error(String errorMessage)
    {
        return this.error(errorMessage, true);
    }

    protected boolean error(String errorMessage, boolean ephemeral)
    {
        this.ephemeral = ephemeral;
        this.response = errorMessage;
        return false;
    }

    //Internal
    protected void setPlayer(User player)
    {
        this.player = player;
    }

    protected void setServer(Guild server)
    {
        this.server = server;
    }

    protected void setChannel(TextChannel channel)
    {
        this.channel = channel;
    }

    protected void initResponses()
    {
        this.embed = new EmbedBuilder();
        this.response = "";
    }

    protected void respond(Consumer<String> text, Consumer<MessageEmbed> embed)
    {
        if(!this.response.isEmpty() && (this.embed != null && !this.embed.isEmpty()))
        {
            text.accept(this.response);
            this.channel.sendMessageEmbeds(this.embed.build()).queue();
        }
        else if(!this.response.isEmpty()) text.accept(this.response);
        else if(this.embed != null && !this.embed.isEmpty()) embed.accept(this.embed.build());
    }

    //Parsers
    public void parseSlashCommand(SlashCommandInteractionEvent event)
    {
        this.setPlayer(event.getUser());
        this.setServer(Objects.requireNonNull(event.getGuild()));
        this.setChannel(event.getChannel().asTextChannel());

        this.initResponses();

        boolean result;
        try
        {
            long i = System.currentTimeMillis();
            result = this.slashCommandLogic(event);
            long f = System.currentTimeMillis();

            BozoLogger.info(Command.class, "Slash Command Logic took " + (f - i) + "ms.");
        }
        catch (Exception exception)
        {
            String stack = Arrays.stream(exception.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));

            this.response = "**" + exception + "**\n:" + stack.substring(0, Math.min(2048, stack.length()));
            this.embed = null;

            exception.printStackTrace();

            this.respond(s -> event.reply(s).queue(), e -> event.replyEmbeds(e).queue());
            return;
        }

        if(!result && this.embed != null && !this.embed.isEmpty()) this.embed.setColor(Color.RED);
        if(!result && !this.response.isEmpty()) this.response = "[ERROR] " + this.response;

        this.respond(s -> event.reply(s).setEphemeral(this.ephemeral).queue(), e -> event.replyEmbeds(e).setEphemeral(this.ephemeral).queue());
    }

    public void parseAutocomplete(CommandAutoCompleteInteractionEvent event)
    {
        this.setPlayer(event.getUser());
        this.setServer(Objects.requireNonNull(event.getGuild()));
        this.setChannel(Objects.requireNonNull(event.getChannel()).asTextChannel());

        this.initResponses();
        boolean result = this.autocompleteLogic(event);
    }

    public void parseButtonInteraction(ButtonInteractionEvent event)
    {
        this.setPlayer(event.getUser());
        this.setServer(Objects.requireNonNull(event.getGuild()));
        this.setChannel(Objects.requireNonNull(event.getChannel()).asTextChannel());

        this.initResponses();
        boolean result = this.buttonLogic(event);
        this.respond(s -> event.reply(s).setEphemeral(this.ephemeral).queue(), e -> event.replyEmbeds(e).setEphemeral(this.ephemeral).queue());
    }

    //Overrides
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event) { return false; }
    protected boolean autocompleteLogic(CommandAutoCompleteInteractionEvent event) { return false; }
    protected boolean buttonLogic(ButtonInteractionEvent event) { return false; }

    //Misc
    public void setCommandData(CommandData commandData)
    {
        this.commandData = commandData;
    }
}
