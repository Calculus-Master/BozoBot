package com.calculusmaster.bozo;

import com.calculusmaster.bozo.commands.CommandQuestions;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.events.IdiotListEvent;
import com.calculusmaster.bozo.events.TimeManager;
import com.calculusmaster.bozo.util.*;
import com.calculusmaster.bozo.events.NameChangeRoleEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BozoBot
{
    public static JDA BOT_JDA;

    public static final List<CommandData> COMMANDS = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException
    {
        BozoLogger.suppressMongo();
        BotConfig.init();

        BozoLogger.init("Questions Attachments", CommandQuestions::readAttachmentsCached);
        BozoLogger.init("Message Leaderboards", MessageLeaderboardHandler::init);

        JDABuilder bot = JDABuilder
                .createDefault(HiddenConfig.TOKEN)
                .enableIntents(GatewayIntent.getIntents(GatewayIntent.DEFAULT))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("Bozostiny 2"))
                .addEventListeners(
                        new Listener()
                );

        BOT_JDA = bot.build().awaitReady();

        Message.suppressContentIntentWarning();

        BozoLogger.init("Slash Commands", Listener::init);

        Guild bozocord = Objects.requireNonNull(BOT_JDA.getGuildById("983450314885713940"));
        Guild onyxhub = BOT_JDA.getGuildById("878207461117550642");
        Guild avluscord = BOT_JDA.getGuildById("1000959891604779068");
        Guild wiacord = BOT_JDA.getGuildById("943354742384521267");

        bozocord.updateCommands().addCommands(COMMANDS.stream().map(CommandData::getSlashCommandData).toList()).queue();
        if(onyxhub != null) onyxhub.updateCommands().addCommands(COMMANDS.stream().filter(cd -> !cd.isOnlyBozocord()).map(CommandData::getSlashCommandData).toList()).queue();
        if(avluscord != null) avluscord.updateCommands().addCommands(COMMANDS.stream().filter(cd -> !cd.isOnlyBozocord() && !cd.getCommandName().equals("lfg")).map(CommandData::getSlashCommandData).toList()).queue();
        if(wiacord != null) wiacord.updateCommands().addCommands(COMMANDS.stream().filter(cd -> !cd.isOnlyBozocord()).map(CommandData::getSlashCommandData).toList()).queue();

        //It's Bozo Time
        BOT_JDA.getChannelById(TextChannel.class, "1069872555541938297").sendMessage("It's Bozo'in Time.").queue();

        //Events
        TimeManager.init();
        //CommandLFG.cleanUpPosts(); //TODO: Disabled temporarily

        GPTManager.init();
    }
}
