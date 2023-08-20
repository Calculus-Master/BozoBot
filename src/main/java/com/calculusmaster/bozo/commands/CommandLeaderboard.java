package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.MessageLeaderboardHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommandLeaderboard extends Command
{
    public static void init()
    {
        CommandData
                .create("leaderboard")
                .withConstructor(CommandLeaderboard::new)
                .withCommand(Commands
                        .slash("leaderboard", "Check the message leaderboard!")
                )
                .setNotOnlyBozocord()
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        List<MessageLeaderboardHandler.LeaderboardEntry> sourceEntries = new ArrayList<>(MessageLeaderboardHandler.LEADERBOARDS.get(this.server.getId()).values());

        List<MessageLeaderboardHandler.LeaderboardEntry> sorted = sourceEntries
                .stream()
                .filter(e -> e.getCount() > 5)
                .sorted((e1, e2) -> e2.getCount() - e1.getCount())
                .toList();

        List<String> text = new ArrayList<>();
        for(int i = 0; i < sorted.size(); i++) text.add((i + 1) + ". " + (sorted.get(i).getUsername().equals(event.getUser().getName()) ? "**" + sorted.get(i).getUsername() + "**" : sorted.get(i).getUsername()) + " | " + sorted.get(i).getCount());

        this.embed.setTitle("Bozo Leaderboard")
                .setDescription(String.join("\n", text))
                .setTimestamp(Instant.now());

        return true;
    }
}
