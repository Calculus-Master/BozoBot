package com.calculusmaster.bozo.events;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.util.BozoLogger;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NameChangeRoleEvent
{
    public static void startNameChangeCycler()
    {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(NameChangeRoleEvent::checkNameChangeCycler, 0, 1, TimeUnit.HOURS);
    }

    public static void checkNameChangeCycler()
    {
        Document d = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "name_change_cycler")).first());

        int hours = d.getInteger("hours") - 1;

        if(hours == 0)
        {
            try
            {
                NameChangeRoleEvent.cycleNameChangeRole();
                Mongo.Misc.updateOne(Filters.eq("type", "name_change_cycler"), Updates.set("hours", 13));
            }
            catch(NullPointerException e)
            {
                BozoLogger.error(NameChangeRoleEvent.class, "NPE caught in name changer role redistribution event.");
            }
        }
        else Mongo.Misc.updateOne(Filters.eq("type", "name_change_cycler"), Updates.set("hours", hours));
    }

    public static void cycleNameChangeRole() throws NullPointerException
    {
        Guild bozoServer = Objects.requireNonNull(BozoBot.BOT_JDA.getGuildById("983450314885713940"));
        Role basicBozoRole = bozoServer.getRoleById("983456276803624961");
        Role nameChangerBozoRole = bozoServer.getRoleById("1075631470913278013");
        TextChannel bozoChannel = Objects.requireNonNull(bozoServer.getTextChannelById("1069872555541938297"));

        Random r = new Random();

        int nameChangers = 1;

        List<String> inactiveBozos = List.of("282742780797779968", "277272207535767554", "149137630855036928", "339137070759149570");

        bozoServer.findMembersWithRoles(basicBozoRole).onSuccess(m -> {
            List<Member> members = new ArrayList<>(m);
            members.removeIf(mem -> mem.getUser().isBot() || mem.getRoles().contains(nameChangerBozoRole) || inactiveBozos.contains(mem.getId()));

            BozoLogger.info(NameChangeRoleEvent.class, "Loaded " + members.size() + " Members: " + m.stream().map(ISnowflake::getId).collect(Collectors.joining(", ")));

            //Remove all current name changers
            bozoServer.findMembersWithRoles(nameChangerBozoRole)
                    .onSuccess(currentNameChangers -> {

                        BozoLogger.info(NameChangeRoleEvent.class, "Found " + currentNameChangers.size() + " Current Adept Name Changers: " + currentNameChangers.stream().map(ISnowflake::getId).collect(Collectors.joining(", ")));
                        currentNameChangers.forEach(cnc -> bozoServer.removeRoleFromMember(cnc, nameChangerBozoRole).queue());

                        //Add new name changers
                        int newNameChangerCount = 0;
                        while(newNameChangerCount < nameChangers)
                        {
                            Member newNameChanger = members.get(r.nextInt(members.size()));
                            members.remove(newNameChanger);

                            BozoLogger.info(NameChangeRoleEvent.class, "Adding New Adept Name Changer: " + newNameChanger.getEffectiveName() + " (" + newNameChanger.getId() + ")");
                            bozoServer.addRoleToMember(newNameChanger, nameChangerBozoRole).queue();

                            newNameChangerCount++;
                        }

                        bozoChannel.sendMessage("<@309135641453527040> New Adept Name Changers have been selected.").queue();
                    })
                    .onError(t -> bozoChannel.sendMessage("<@309135641453527040> Failed removing role from users.").queue());
        }).onError(t -> bozoChannel.sendMessage("<@309135641453527040> Failed giving role to users.").queue());
    }
}
