package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class CommandQuestions extends Command
{
    public static void init()
    {
        CommandData
                .create("questions")
                .withConstructor(CommandQuestions::new)
                .withCommand(Commands
                        .slash("questions", "Gets a random questions moment.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        //TODO: Use actual staff quotes channel
        TextChannel channel = event.getGuild().getChannelById(TextChannel.class, "998041223489138738");
        if(channel == null) return this.error("Can't access questions channel or it doesn't exist.");

        Random r = new Random();

        event.deferReply().queue(ih -> {
            try
            {
                EmbedBuilder e = new EmbedBuilder();
                List<Message> pool = new ArrayList<>();

                channel.getIterableHistory().takeAsync(23000).get().forEach(m -> {
                    if(!m.getAttachments().isEmpty()) pool.add(m);
                });

                Message quote = pool.get(r.nextInt(pool.size()));
                this.embed.setImage(quote.getAttachments().get(0).getUrl());

                ih.editOriginalEmbeds(e.build()).queue();
            }
            catch(InterruptedException | ExecutionException e)
            {
                e.printStackTrace();
                ih.editOriginal("Retrieval failed!").queue();
            }
        });

        this.embed = null;
        this.response = "";

        return true;
    }
}
