package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.events.NameChangeRoleEvent;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.Instant;
import java.util.Objects;

public class CommandRemoveNameChanger extends Command
{
    public static void init()
    {
        CommandData
                .create("remove-name-changer")
                .withConstructor(CommandRemoveNameChanger::new)
                .withCommand(Commands
                        .slash("remove-name-changer", "Cringe.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        if(event.getMember().getRoles().stream().noneMatch(r -> r.getId().equals("1075631470913278013")))
            return this.error("You're not an Adept Name Changer.", true);

        NameChangeRoleEvent.cycleNameChangeRole();

        int interval = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "time_data")).first()).getInteger("name_changer.interval");
        Mongo.Misc.updateOne(Filters.eq("type", "time_data"), Updates.set("name_changer.next_time", Instant.now().getEpochSecond() + (interval * 60L * 60)));

        this.ephemeral = true;
        this.response = "You surrendered your power.";

        return true;
    }
}
