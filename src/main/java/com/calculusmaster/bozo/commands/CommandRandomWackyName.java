package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class CommandRandomWackyName extends Command
{
    public static void init()
    {
        CommandData
                .create("name")
                .withConstructor(CommandRandomWackyName::new)
                .withCommand(Commands
                        .slash("name", "Shows you a random god-tier username.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        TextChannel channel = event.getGuild().getChannelById(TextChannel.class, "1060094853494616095");
        if(channel == null) return this.error("Can't access names channel or it doesn't exist.");

        Random r = new Random();

        try
        {
            List<Message> pool = new ArrayList<>();

            channel.getIterableHistory().takeAsync(400).get().forEach(m -> {
                if(!m.getAttachments().isEmpty()) pool.add(m);
            });

            Message quote = pool.get(r.nextInt(pool.size()));
            this.embed.setImage(quote.getAttachments().get(0).getUrl());
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
