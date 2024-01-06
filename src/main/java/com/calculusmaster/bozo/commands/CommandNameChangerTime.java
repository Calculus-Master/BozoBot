package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bson.Document;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CommandNameChangerTime extends Command
{
    public static void init()
    {
        CommandData
                .create("name-changer-time")
                .withConstructor(CommandNameChangerTime::new)
                .withCommand(Commands
                        .slash("name-changer-time", "Check the time the Name Changer will cycle at.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        Document time = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "time_data")).first()).get("name_changer", Document.class);

        long t = time.getLong("next_time");
        this.response = "The Name Changer changes at <t:%s:F> (<t:%s:R>).".formatted(t, t);

        return true;
    }
}
