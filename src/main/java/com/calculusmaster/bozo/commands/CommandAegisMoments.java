package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bson.Document;

import java.util.List;
import java.util.Random;

public class CommandAegisMoments extends Command
{
    public static void init()
    {
        CommandData
                .create("aegis")
                .withConstructor(CommandAegisMoments::new)
                .withCommand(Commands
                        .slash("aegis", "Breathing slows down my runs.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        Document data = Mongo.UserMomentsDB.find(Filters.eq("type", "aegis")).first();

        if(data == null) return this.error("Aegis Moments database JSON not found!");

        List<String> attachmentPool = data.getList("attachments", String.class);
        List<String> queuedPool = data.getList("queued", String.class);

        if(attachmentPool.isEmpty()) return this.error("No Aegis Moments are available yet!" + (queuedPool.isEmpty() ? " None are in queue!" : " There are " + queuedPool.size() + " moments waiting to be added!"), true);

        this.embed.setImage(attachmentPool.get(new Random().nextInt(attachmentPool.size())));

        return true;
    }
}
