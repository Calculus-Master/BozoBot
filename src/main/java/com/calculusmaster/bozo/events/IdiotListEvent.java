package com.calculusmaster.bozo.events;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.util.BozoLogger;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class IdiotListEvent
{
    public static void triggerIdiotListPing()
    {
        Guild bozoServer = Objects.requireNonNull(BozoBot.BOT_JDA.getGuildById("983450314885713940"));
        Role basicBozoRole = bozoServer.getRoleById("983456276803624961");
        TextChannel idiotListChannel = Objects.requireNonNull(bozoServer.getTextChannelById("1089732136359186432"));

        List<String> inactiveBozos = Mongo.Misc.find(Filters.eq("type", "idiot_list_immune")).first().getList("list", String.class);

        Random r = new Random();

        bozoServer.findMembersWithRoles(basicBozoRole).onSuccess(m -> {
            List<Member> members = m.stream().filter(x -> !inactiveBozos.contains(x.getId())).toList();
            Member victim = members.get(r.nextInt(members.size()));

            idiotListChannel.sendMessage(victim.getAsMention()).queue();

            BozoLogger.info(IdiotListEvent.class, "Idiot list pinged " + victim.getUser().getName() + " (" + victim.getId() + ")!");
        }).onError(t -> BozoLogger.error(IdiotListEvent.class, "Error in idiot list ping event: " + t.getMessage()));
    }
}
