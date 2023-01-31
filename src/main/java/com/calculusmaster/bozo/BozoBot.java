package com.calculusmaster.bozo;

import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.BozoLogger;
import com.calculusmaster.bozo.util.HiddenConfig;
import com.calculusmaster.bozo.util.Listener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BozoBot
{
    public static JDA BOT_JDA;

    public static final List<CommandData> COMMANDS = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException
    {
        JDABuilder bot = JDABuilder
                .createDefault(HiddenConfig.TOKEN)
                .enableIntents(GatewayIntent.getIntents(GatewayIntent.DEFAULT))
                .setActivity(Activity.playing("Bozostiny 2"))
                .addEventListeners(
                        new Listener()
                );

        BOT_JDA = bot.build().awaitReady();

        BozoLogger.init("Slash Commands", Listener::init);

        BOT_JDA.getGuildById("983450314885713940").updateCommands()
                .addCommands(COMMANDS.stream().map(CommandData::getSlashCommandData).toList()).queue();
    }
}
