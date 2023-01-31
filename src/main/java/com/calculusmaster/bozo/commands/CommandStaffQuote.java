package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class CommandStaffQuote extends Command
{
    public static void init()
    {
        CommandData
                .create("staff")
                .withConstructor(CommandStaffQuote::new)
                .withCommand(Commands
                        .slash("staff", "Gets a random staff quote.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        //TODO: Use actual staff quotes channel
        TextChannel channel = event.getGuild().getChannelById(TextChannel.class, "983450314885713943" /*"1040896688673529886"*/);
        if(channel == null) return this.error("Can't access staff quotes channel or it doesn't exist.");

        Random r = new Random();

        try
        {
            List<Message> pool = new ArrayList<>();

            channel.getIterableHistory().takeAsync(500).get().forEach(m -> {
                if(!m.getAttachments().isEmpty()) pool.add(m);
            });

            pool.forEach(m -> {
                Message quote = pool.get(r.nextInt(pool.size()));
                this.embed.setImage(quote.getAttachments().get(0).getUrl());
            });
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
