package com.calculusmaster.bozo.events;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.util.BozoLogger;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.Document;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GhostPingEvent
{
    public static void startGhostPingCycler()
    {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(GhostPingEvent::checkGhostPingCycler, 0, 1, TimeUnit.HOURS);
    }

    public static void checkGhostPingCycler()
    {
        Document d = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "ghost_ping_cycler")).first());

        int hours = d.getInteger("hours") - 1;

        if(hours == 0)
        {
            try
            {
                GhostPingEvent.ghostPing();
                Mongo.Misc.updateOne(Filters.eq("type", "ghost_ping_cycler"), Updates.set("hours", new Random().nextInt(10, 15)));
            }
            catch(NullPointerException e)
            {
                BozoLogger.error(GhostPingEvent.class, "NPE caught in ghost ping event.");
            }
        }
        else Mongo.Misc.updateOne(Filters.eq("type", "ghost_ping_cycler"), Updates.set("hours", hours));
    }

    public static void ghostPing()
    {
        Guild bozoServer = Objects.requireNonNull(BozoBot.BOT_JDA.getGuildById("983450314885713940"));
        Role basicBozoRole = bozoServer.getRoleById("983456276803624961");
        TextChannel ghostPingChannel = Objects.requireNonNull(bozoServer.getTextChannelById("1089732136359186432"));

        List<String> inactiveBozos = List.of("282742780797779968", "277272207535767554", "149137630855036928", "339137070759149570");

        Random r = new Random();

        bozoServer.findMembersWithRoles(basicBozoRole).onSuccess(m -> {
            List<Member> members = m.stream().filter(x -> !inactiveBozos.contains(x.getId())).toList();
            Member victim = members.get(r.nextInt(members.size()));

            ghostPingChannel.sendMessage(victim.getAsMention()).queue();

            BozoLogger.info(GhostPingEvent.class, "Ghost pinged " + victim.getUser().getAsTag() + " (" + victim.getId() + ")!");
        }).onError(t -> BozoLogger.error(GhostPingEvent.class, "Error in ghost ping event: " + t.getMessage()));
    }
}
