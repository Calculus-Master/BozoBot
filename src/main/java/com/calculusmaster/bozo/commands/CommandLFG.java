package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.BotConfig;
import com.calculusmaster.bozo.util.BozoLogger;
import com.calculusmaster.bozo.util.LFGPost;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandLFG extends Command
{
    public static final String LFG_POST_BUTTON = "LFG-POST";

    public static void init()
    {
        CommandData
                .create("lfg")
                .withConstructor(CommandLFG::new)
                .withButtons(LFG_POST_BUTTON)
                .withCommand(Commands
                        .slash("lfg", "Make an lfg post!")
                        .addSubcommands(
                                new SubcommandData("create", "Create a new LFG post.")
                                        .addOption(OptionType.STRING, "activity", "The activity you're setting a post for.", true)
                                        .addOption(OptionType.STRING, "time", "The time you want to start at. Use hammertime.cyou and copy-paste any of the formats into here.", true)
                                        .addOption(OptionType.INTEGER, "players", "The number of players you need. -1 to ignore this option.", true),

                                new SubcommandData("edit", "Edit an existing LFG post.")
                                        .addOption(OptionType.STRING, "post-id", "The post ID of the LFG post you're editing.", true)
                                        .addOption(OptionType.STRING, "activity", "Edit the activity name of the LFG post.", false)
                                        .addOption(OptionType.STRING, "time", "Edit the time of the LFG post.", false)
                                        .addOption(OptionType.INTEGER, "players", "Edit the number of players needed. -1 to ignore.", false)
                        )
                )
                .setNotOnlyBozocord()
                .register();
    }

    public static void cleanUpPosts()
    {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long current = Instant.now().getEpochSecond();
            List<String> toDelete = new ArrayList<>();
            Mongo.LFGPostDB.find().forEach(d -> {
                String timestamp = d.getString("time");

                try
                {
                    String target = timestamp.substring(timestamp.indexOf(":") + 1, timestamp.lastIndexOf(":"));
                    long savedTime = Long.parseLong(target);

                    if(current > savedTime) toDelete.add(d.getString("postID"));
                }
                catch(Exception e)
                {
                    BozoLogger.warn(CommandLFG.class, "Skipping automatic deletion of LFG Post " + d.getString("postID") + " due to non-numeric timestamp.");
                }
            });

            toDelete.forEach(s -> Mongo.LFGPostDB.deleteOne(Filters.eq("postID", s)));
        }, 1, 1, TimeUnit.HOURS);
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        if(event.getGuild().equals("1000959891604779068")) return this.error("This command is disabled on this server.");

        if(event.getSubcommandName().equals("create"))
        {
            OptionMapping activityOption = event.getOption("activity");
            OptionMapping timeOption = event.getOption("time");
            OptionMapping playersOption = event.getOption("players");

            if(activityOption == null || timeOption == null || playersOption == null) return this.error("A required option is missing.", true);
            else if(!BotConfig.VALID_LFG_CHANNELS.contains(event.getChannel().getId()))
                return this.error("This command can only be used in one of these channels: " + BotConfig.VALID_LFG_CHANNELS.stream().map(s -> "<#" + s + ">").collect(Collectors.joining(" ")) + ".", true);

            LFGPost post = new LFGPost();

            if(!post.setTime(timeOption.getAsString()))
                return this.error("""
                        Invalid input.
                        Supported formats are:
                         - A unix timestamp specifying the exact time for the activity.
                         - `XdYh`: X days and Y hours from now.
                         - `Xh`: X hours from now.
                         - Any text: Custom "time" that will be displayed as-is.
                        """);

            post.setActivity(activityOption.getAsString());
            post.setPostUser(event.getUser());
            post.setPlayers(Math.max(0, playersOption.getAsInt()));

            if(new Random().nextFloat() < 0.05F) post.addUser(BozoBot.BOT_JDA.getSelfUser(), "maybe");

            String baseButtonID = LFG_POST_BUTTON + "-" + post.getPostID() + "-";
            Button yes = Button.of(ButtonStyle.SUCCESS, baseButtonID + "yes", "Yes");
            Button maybe = Button.of(ButtonStyle.PRIMARY, baseButtonID + "maybe", "Maybe");
            Button no = Button.of(ButtonStyle.DANGER, baseButtonID + "no", "No");

            event.replyEmbeds(post.createEmbed().build())
                    .addActionRow(yes, maybe, no)
                    .queue(ih -> ih.retrieveOriginal().queue(m -> {
                        post.setMessage(m);
                        post.upload();

                        m.createThreadChannel("LFG Post Discussion (Post ID: " + post.getPostID() + ")")
                                .queue(thread -> thread.addThreadMember(event.getUser()).queue());
                    }));
        }
        else if(event.getSubcommandName().equals("edit"))
        {
            OptionMapping postIDOption = Objects.requireNonNull(event.getOption("post-id"));
            OptionMapping activityOption = event.getOption("activity");
            OptionMapping timeOption = event.getOption("time");
            OptionMapping playersOption = event.getOption("players");

            if(activityOption == null && timeOption == null)
                return this.error("You must specify an option to edit, either the activity name or time.");

            LFGPost post = LFGPost.get(postIDOption.getAsString());

            if(post == null) return this.error("Invalid Post ID");
            else if(!event.getUser().getId().equals(post.getPostUserID()))
                return this.error("Only the creator of the LFG Post (" + post.getPostUserName() + ") can edit it.", true);

            List<String> changelog = new ArrayList<>();

            if(timeOption != null)
            {
                String oldTime = post.getTime();
                boolean t = post.setTime(timeOption.getAsString());

                if(!t)
                    return this.error("""
                        Invalid input.
                        Supported formats are:
                         - A unix timestamp specifying the exact time for the activity.
                         - `XdYh`: X days and Y hours from now.
                         - `Xh`: X hours from now.
                         - Any text: Custom "time" that will be displayed as-is.
                        """);
                else
                    changelog.add("Time: %s (was ||%s||)".formatted(post.getTime(), oldTime));
            }

            if(activityOption != null)
            {
                if(activityOption.getAsString().isEmpty()) return this.error("Activity name cannot be empty.");
                else
                {
                    changelog.add("Activity Name: \"%s\" (was ||\"%s\"||)".formatted(activityOption.getAsString(), post.getActivity()));
                    post.setActivity(activityOption.getAsString());
                }
            }

            if(playersOption != null)
            {
                changelog.add("Players: %s (was ||%s||)".formatted(playersOption.getAsInt(), post.getPlayers()));
                post.setPlayers(Math.max(0, playersOption.getAsInt()));
            }

            post.update();

            event.reply("Your LFG post has been edited. Please check the post to make sure the changes are correct.").setEphemeral(true).queue();

            Guild guild = Objects.requireNonNull(event.getGuild());
            TextChannel channel = Objects.requireNonNull(guild.getChannelById(TextChannel.class, post.getChannelID()));

            channel.retrieveMessageById(post.getMessageID()).queue(m ->
            {
                m.editMessageEmbeds(post.createEmbed().build()).queue();

                if(m.getStartedThread() != null)
                {
                    List<String> pings = new ArrayList<>();
                    pings.addAll(post.yes()); pings.addAll(post.maybe()); pings.addAll(post.no());
                    pings.remove(post.getPostUserID());

                    String pingsMessage = pings.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" "));
                    m.getStartedThread().sendMessage("**LFG Post Updated!**\n" + String.join("\n", changelog) + "\n\n" + pingsMessage).queue();
                }
            });
        }

        this.embed = null;
        this.response = "";

        return true;
    }

    @Override
    protected boolean buttonLogic(ButtonInteractionEvent event)
    {
        this.ephemeral = true;

        //LFG-POST-<postID>-<yes/maybe/no>
        String[] info = event.getButton().getId().split("-");

        String postID = info[2];
        LFGPost post = LFGPost.get(postID);

        if(post == null) return this.error("LFG Post not found.", true);

        String userID = event.getUser().getId();
        boolean isNewUser = !post.yes().contains(userID) && !post.maybe().contains(userID) && !post.no().contains(userID);

        String choice = info[3];
        switch(choice)
        {
            case "yes" ->
            {
                if(post.yes().contains(userID)) return this.error("You're already in the **Yes** list.");

                post.addUser(event.getUser(), "yes");
                post.update();

                this.response = "Added to **Yes**.";
            }
            case "maybe" ->
            {
                if(post.maybe().contains(userID)) return this.error("You're already in the **Maybe** list.");

                post.addUser(event.getUser(), "maybe");
                post.update();

                this.response = "Added to **Maybe**.";
            }
            case "no" ->
            {
                if(post.no().contains(userID)) return this.error("You're already in the **No** list.");

                post.addUser(event.getUser(), "no");
                post.update();

                this.response = "Added to **No**.";
            }
            default -> //Shouldn't be possible to reach
            {
                return this.error("Invalid choice.");
            }
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        TextChannel channel = Objects.requireNonNull(guild.getChannelById(TextChannel.class, post.getChannelID()));

        channel.retrieveMessageById(post.getMessageID()).queue(m ->
        {
            m.editMessageEmbeds(post.createEmbed().build()).queue();

            if(isNewUser && m.getStartedThread() != null) m.getStartedThread().addThreadMember(event.getUser()).queue();
        });

        return true;
    }
}
