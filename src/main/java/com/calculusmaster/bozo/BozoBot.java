package com.calculusmaster.bozo;

import com.calculusmaster.bozo.commands.CommandQuestions;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.events.GhostPingEvent;
import com.calculusmaster.bozo.util.*;
import com.calculusmaster.bozo.events.NameChangeRoleEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.List;

public class BozoBot
{
    public static JDA BOT_JDA;

    public static final List<CommandData> COMMANDS = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException
    {
        BozoLogger.suppressMongo();

        BozoLogger.init("Questions Attachments", CommandQuestions::readAttachmentsCached);

        JDABuilder bot = JDABuilder
                .createDefault(HiddenConfig.TOKEN)
                .enableIntents(GatewayIntent.getIntents(GatewayIntent.DEFAULT))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("Bozostiny 2"))
                .addEventListeners(
                        new Listener()
                );

        BOT_JDA = bot.build().awaitReady();

        Message.suppressContentIntentWarning();

        BozoLogger.init("Slash Commands", Listener::init);

        BOT_JDA.getGuildById("983450314885713940").updateCommands()
                .addCommands(COMMANDS.stream().map(CommandData::getSlashCommandData).toList()).queue();

        //It's Bozo Time
        BOT_JDA.getChannelById(TextChannel.class, "1069872555541938297").sendMessage("It's Bozo'in Time.").queue();

        //Events
        NameChangeRoleEvent.startNameChangeCycler();
        GhostPingEvent.startGhostPingCycler();
    }
}
