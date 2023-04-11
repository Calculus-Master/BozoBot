package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.BozoLogger;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
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

import javax.print.Doc;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CommandLFG extends Command
{
    public static final String LFG_POST_BUTTON = "LFG-POST";
    public static final String LFG_CHANNEL = "1020902364036730920";

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
                                        .addOption(OptionType.STRING, "time", "The time you want to start at. Use hammertime.cyou and copy-paste any of the formats into here.", true),

                                new SubcommandData("edit", "Edit an existing LFG post.")
                                        .addOption(OptionType.STRING, "post-id", "The post ID of the LFG post you're editing.", true)
                                        .addOption(OptionType.STRING, "activity", "Edit the activity name of the LFG post.", false)
                                        .addOption(OptionType.STRING, "time", "Edit the time of the LFG post.", false)
                        )
                )
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
        if(event.getSubcommandName().equals("create"))
        {
            OptionMapping activityOption = event.getOption("activity");
            OptionMapping timeOption = event.getOption("time");

            if(activityOption == null || timeOption == null) return this.error("Either activity or time option is missing.", true);
            else if(!event.getChannel().getId().equals(LFG_CHANNEL)) return this.error("Use " + event.getGuild().getChannelById(TextChannel.class, "1020902364036730920").getAsMention() + ".");

            Random r = new Random();

            String postID = IntStream.range(0, 8).mapToObj(i -> String.valueOf(r.nextInt(10))).collect(Collectors.joining(""));

            String time = this.getUnixTimestamp(timeOption.getAsString());
            if(time.isEmpty()) return this.error("Invalid input. Supported formats are `XdYh` and `Xh`, where X and Y are numbers.", true);

            EmbedBuilder embed = new EmbedBuilder()
                    .addField("Activity", "***" + activityOption.getAsString() + "***", false)
                    .addField("Time", time, true)
                    .addField("Post ID", postID, true)
                    .addBlankField(false)
                    .addField("Yes", this.player.getAsTag(), true)
                    .addField("Maybe", "None", true)
                    .addField("No", "None", true);

            Document postData = new Document();

            postData.append("postID", postID)
                    .append("poster", event.getUser().getId())
                    .append("activity", activityOption.getAsString())
                    .append("time", time)
                    .append("yes", List.of(this.player.getAsTag()))
                    .append("maybe", new ArrayList<String>())
                    .append("no", new ArrayList<String>());

            String baseButtonID = LFG_POST_BUTTON + "-" + postID + "-";
            Button yes = Button.of(ButtonStyle.SUCCESS, baseButtonID + "yes", "Yes");
            Button maybe = Button.of(ButtonStyle.PRIMARY, baseButtonID + "maybe", "Maybe");
            Button no = Button.of(ButtonStyle.DANGER, baseButtonID + "no", "No");

            event.replyEmbeds(embed.build()).addActionRow(yes, maybe, no).queue(ih -> ih.retrieveOriginal().queue(m -> {
                postData.append("messageID", m.getId());
                Mongo.LFGPostDB.insertOne(postData);

                m.createThreadChannel("LFG Post " + postID + " Discussion").queue();
            }));
        }
        else if(event.getSubcommandName().equals("edit"))
        {
            OptionMapping postIDOption = event.getOption("post-id");
            OptionMapping activityOption = event.getOption("activity");
            OptionMapping timeOption = event.getOption("time");

            if(postIDOption == null)
                return this.error("Post ID option missing.");
            else if(activityOption == null && timeOption == null)
                return this.error("You must specify an option to edit, either the activity name or time.", true);

            Document data = Mongo.LFGPostDB.find(Filters.eq("postID", postIDOption.getAsString())).first();
            if(data == null)
                return this.error("Invalid Post ID", true);

            if(!event.getUser().getId().equals(data.getString("poster")))
                return this.error("Only the creator of the LFG post (<@" + data.getString("poster") + ">) can edit it.", true);

            BiConsumer<Message, Pair<String, String>> editer = (message, pair) -> {
                MessageEmbed original = message.getEmbeds().get(0);
                List<MessageEmbed.Field> fields = original.getFields();

                String activity = pair.getFirst().equals("activity") ? pair.getSecond() : fields.stream().filter(f -> f.getName().equals("Activity")).findFirst().get().getValue();

                String time = pair.getFirst().equals("time") ? this.getUnixTimestamp(pair.getSecond()) : fields.stream().filter(f -> f.getName().equals("Time")).findFirst().get().getValue();
                if(time == null || time.isEmpty())
                {
                    time = fields.stream().filter(f -> f.getName().equals("Time")).findFirst().get().getValue();
                    BozoBot.BOT_JDA.openPrivateChannelById(event.getUser().getId()).onSuccess(c -> c.sendMessage("Your LFG post was not edited because of an invalid time input. Supported formats are `XdYh` and `Xh`, where X and Y are numbers.").queue());
                }

                String yes = fields.stream().filter(f -> f.getName().equals("Yes")).findFirst().get().getValue();
                String maybe = fields.stream().filter(f -> f.getName().equals("Maybe")).findFirst().get().getValue();
                String no = fields.stream().filter(f -> f.getName().equals("No")).findFirst().get().getValue();

                EmbedBuilder embed = new EmbedBuilder()
                        .addField("Activity", "" + activity, false)
                        .addField("Time", "" + time, true)
                        .addField("Post ID", postIDOption.getAsString(), true)
                        .addBlankField(false)
                        .addField("Yes", "" + yes, true)
                        .addField("Maybe", "" + maybe, true)
                        .addField("No", "" + no, true);

                message.editMessageEmbeds(embed.build()).queue();
            };

            String activityName = data.getString("activity");
            if(activityOption != null)
            {
                String newActivityName = activityOption.getAsString();

                event.getChannel()
                        .retrieveMessageById(data.getString("messageID"))
                        .queue(m -> editer.accept(m, new Pair<>("activity", "***" + newActivityName + "***")));

                Mongo.LFGPostDB.updateOne(Filters.eq("postID", postIDOption.getAsString()), Updates.set("activity", newActivityName));
            }

            if(timeOption != null)
            {
                String newTime = this.getUnixTimestamp(timeOption.getAsString());
                if(newTime.isEmpty()) return this.error("Invalid input. Supported formats are `XdYh` and `Xh`, where X and Y are numbers.");

                event.getChannel()
                        .retrieveMessageById(data.getString("messageID"))
                        .queue(m -> editer.accept(m, new Pair<>("time", newTime)));

                Mongo.LFGPostDB.updateOne(Filters.eq("postID", postIDOption.getAsString()), Updates.set("time", newTime));
            }

            event.reply("Your LFG post has been edited. Please check the post to make sure the changes are correct.").setEphemeral(true).queue();

            //TODO: Figure out pinging in threads
            List<String> pingMembers = new ArrayList<>();
            pingMembers.addAll(data.getList("yes", String.class));
            pingMembers.addAll(data.getList("maybe", String.class));

            pingMembers = pingMembers.stream().map(s -> "<@" + s + ">").collect(Collectors.toList());
            event.getGuild().getChannelById(TextChannel.class, LFG_CHANNEL).sendMessage(String.join(" ", pingMembers) + "\n\nLFG Post (Activity: " + activityName + ") Updated.").queue();
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
        Document data = Mongo.LFGPostDB.find(Filters.eq("postID", postID)).first();

        if(data == null) return this.error("LFG Post not found. The LFG Post most likely has expired, but, if not, try again in a couple seconds.", true);

        String userID = event.getUser().getId();

        List<String> yes = new ArrayList<>(data.getList("yes", String.class));
        List<String> maybe = new ArrayList<>(data.getList("maybe", String.class));
        List<String> no = new ArrayList<>(data.getList("no", String.class));

        String choice = info[3];
        if(choice.equals("yes"))
        {
            Bson removalUpdate = null;
            if(maybe.contains(userID))
            {
                maybe.remove(userID);
                removalUpdate = Updates.pull("maybe", userID);
                this.response = "Moved from **Maybe** to **Yes**.";
            }
            else if(no.contains(userID))
            {
                no.remove(userID);
                removalUpdate = Updates.pull("no", userID);
                this.response = "Moved from **No** to **Yes**.";
            }
            else if(yes.contains(userID)) return this.error("You're already in the **Yes** list.");
            else this.response = "Added to **Yes**.";

            yes.add(userID);
            Bson push = Updates.push("yes", userID);
            Mongo.LFGPostDB.updateOne(Filters.eq("postID", postID), push);
            if(removalUpdate != null) Mongo.LFGPostDB.updateOne(Filters.eq("postID", postID), removalUpdate);
        }
        else if(choice.equals("maybe"))
        {
            Bson removalUpdate = null;
            if(yes.contains(userID))
            {
                yes.remove(userID);
                removalUpdate = Updates.pull("yes", userID);
                this.response = "Moved from **Yes** to **Maybe**.";
            }
            else if(no.contains(userID))
            {
                no.remove(userID);
                removalUpdate = Updates.pull("no", userID);
                this.response = "Moved from **No** to **Maybe**.";
            }
            else if(maybe.contains(userID)) return this.error("You're already in the **Maybe** list.");
            else this.response = "Added to **Maybe**.";

            maybe.add(userID);
            Bson push = Updates.push("maybe", userID);
            Mongo.LFGPostDB.updateOne(Filters.eq("postID", postID), push);
            if(removalUpdate != null) Mongo.LFGPostDB.updateOne(Filters.eq("postID", postID), removalUpdate);
        }
        else if(choice.equals("no"))
        {
            Bson removalUpdate = null;
            if(yes.contains(userID))
            {
                yes.remove(userID);
                removalUpdate = Updates.pull("yes", userID);
                this.response = "Moved from **Yes** to **No**.";
            }
            else if(maybe.contains(userID))
            {
                maybe.remove(userID);
                removalUpdate = Updates.pull("maybe", userID);
                this.response = "Moved from **Maybe** to **No**.";
            }
            else if(no.contains(userID)) return this.error("You're already in the **No** list.");
            else this.response = "Added to **No**.";

            no.add(userID);
            Bson push = Updates.push("no", userID);
            Mongo.LFGPostDB.updateOne(Filters.eq("postID", postID), push);
            if(removalUpdate != null) Mongo.LFGPostDB.updateOne(Filters.eq("postID", postID), removalUpdate);
        }

        event.getGuild().getChannelById(TextChannel.class, LFG_CHANNEL).retrieveMessageById(data.getString("messageID")).queue(m -> {
            MessageEmbed original = m.getEmbeds().get(0);
            List<MessageEmbed.Field> fields = original.getFields();

            String activity = fields.stream().filter(f -> f.getName().equals("Activity")).findFirst().get().getValue();
            String time = fields.stream().filter(f -> f.getName().equals("Time")).findFirst().get().getValue();

            List<String> allMembers = new ArrayList<>();
            allMembers.addAll(yes);
            allMembers.addAll(maybe);
            allMembers.addAll(no);

            event.getGuild().retrieveMembersByIds(allMembers.stream().map(Long::parseLong).distinct().toList()).onSuccess(memberList -> {
                Function<List<String>, String> converter = list -> list.stream()
                        .map(s -> memberList.stream().filter(mem -> mem.getUser().getId().equals(s)).findFirst().get())
                        .map(mem -> mem.getUser().getName() + "#" + mem.getUser().getDiscriminator())
                        .collect(Collectors.joining("\n"));

                EmbedBuilder embed = new EmbedBuilder()
                        .addField("Activity", "" + activity, false)
                        .addField("Time", "" + time, true)
                        .addField("Post ID", postID, true)
                        .addBlankField(false)
                        .addField("Yes", converter.apply(yes), true)
                        .addField("Maybe", converter.apply(maybe), true)
                        .addField("No", converter.apply(no), true);

                m.editMessageEmbeds(embed.build()).queue();
            });
        });

        return true;
    }

    private String getUnixTimestamp(String input)
    {
        //Best guess for it being a timestamp already
        if(input.startsWith("<t:") && input.lastIndexOf(":") != input.indexOf(":")) return input;

        //<x>d<y>h
        else if(input.matches("\\d*d\\d*h"))
        {
            int days = Integer.parseInt(input.substring(0, input.indexOf("d")));
            int hours = Integer.parseInt(input.substring(input.indexOf("d") + 1, input.indexOf("h")));

            Instant time = Instant.now().plus(days, ChronoUnit.DAYS).plus(hours, ChronoUnit.HOURS);

            long epoch = time.getEpochSecond();

            epoch = (epoch / 900) * 900; //15 minutes

            return "<t:" + epoch + ":F>";
        }
        //<x>h
        else if(input.matches("\\d*h"))
        {
            int hours = Integer.parseInt(input.substring(0, input.indexOf("h")));

            Instant time = Instant.now().plus(hours, ChronoUnit.HOURS);

            long epoch = time.getEpochSecond();

            epoch = (epoch / 900) * 900; //15 minutes

            return "<t:" + epoch + ":F>";
        }
        else return input;
    }
}
