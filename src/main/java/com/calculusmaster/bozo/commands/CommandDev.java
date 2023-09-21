package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.events.GhostPingEvent;
import com.calculusmaster.bozo.events.NameChangeRoleEvent;
import com.calculusmaster.bozo.util.BotConfig;
import com.calculusmaster.bozo.util.MessageLeaderboardHandler;
import com.calculusmaster.bozo.util.Mongo;
import com.calculusmaster.bozo.util.StarboardPost;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CommandDev extends Command
{
    public static void init()
    {
        CommandData
                .create("dev")
                .withConstructor(CommandDev::new)
                .withCommand(Commands
                        .slash("dev", "Secret developer powers.")
                        .addOption(OptionType.STRING, "command", "Command", true)
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        OptionMapping command = event.getOption("command");

        if(!event.getUser().getId().equals("309135641453527040")) return this.error("Nice try, bozo.");
        else if(command == null) return this.error("Invalid command?");
        else if(command.getAsString().equalsIgnoreCase("savequestionsattachments"))
        {
            TextChannel channel = event.getGuild().getChannelById(TextChannel.class, "998041223489138738");

            if(channel != null) event.deferReply().queue(ih -> {
                try
                {
                    List<Message> pool = new ArrayList<>();

                    channel.getIterableHistory().takeAsync(23000).get().forEach(m -> {
                        if(!m.getAttachments().isEmpty()) pool.add(m);
                    });

                    pool.forEach(m -> m.getAttachments().forEach(a -> {
                        Document data = new Document()
                                .append("attachmentID", a.getId())
                                .append("link", a.getUrl())
                                .append("voters", new ArrayList<>())
                                .append("votes_keep", 0)
                                .append("votes_shard", 0)
                                .append("flag", "none");

                        Mongo.QuestionsVotingDB.insertOne(data);
                    }));

                    ih.editOriginal("Done!").queue();
                }
                catch(InterruptedException | ExecutionException e)
                {
                    e.printStackTrace();
                    ih.editOriginal("Retrieval failed!").queue();
                }
            });

            this.embed = null;
            this.response = "";

            return true;
        }
        else if(command.getAsString().equalsIgnoreCase("cachequestionsattachments"))
        {
            CommandQuestions.readAttachmentsCached();
        }
        else if(command.getAsString().equalsIgnoreCase("deleteflaggedattachments"))
        {
            Mongo.QuestionsVotingDB.deleteMany(Filters.eq("flag", "delete"));
        }
        else if(command.getAsString().startsWith("flagattachment"))
        {
            String[] msg = command.getAsString().split("-");

            boolean delete = msg[1].equals("delete");

            if(!delete) Mongo.QuestionsVotingDB.updateOne(Filters.eq("attachmentID", msg[2]), Updates.set("flag", "keep"));
            else Mongo.QuestionsVotingDB.updateOne(Filters.eq("attachmentID", msg[2]), Updates.set("flag", "delete"));
        }
        else if(command.getAsString().startsWith("queryattachment"))
        {
            String[] msg = command.getAsString().split("-");

            event.getChannel().sendMessage(Mongo.QuestionsVotingDB.find(Filters.eq("attachmentID", msg[1])).first().toString()).queue();
        }
        else if(command.getAsString().equals("attachmentflagqueue"))
        {
            List<String> res = new ArrayList<>();
            Mongo.QuestionsVotingDB.find(Filters.eq("flag", "none")).forEach(d -> {
                if(d.getInteger("votes_keep") + 1 >= 4)
                    res.add("ID {%s} – **KEEP**".formatted(d.getString("attachmentID")));
                else if(d.getInteger("votes_shard") + 1 >= 4)
                    res.add("ID {%s} – **SHARD**".formatted(d.getString("attachmentID")));
            });

            event.getChannel().sendMessage(String.join("\n", res)).queue();
        }
        else if(command.getAsString().equals("autoflag"))
        {
            List<String> keep = new ArrayList<>(), shard = new ArrayList<>();
            Mongo.QuestionsVotingDB.find(Filters.eq("flag", "none")).forEach(d -> {
                if(d.getInteger("votes_keep") + 1 >= 4)
                    keep.add(d.getString("attachmentID"));
                else if(d.getInteger("votes_shard") + 1 >= 4)
                    shard.add(d.getString("attachmentID"));
            });

            for(String s : keep) Mongo.QuestionsVotingDB.updateOne(Filters.eq("attachmentID", s), Updates.set("flag", "keep"));
            for(String s : shard) Mongo.QuestionsVotingDB.updateOne(Filters.eq("attachmentID", s), Updates.set("flag", "shard"));

            event.getChannel().sendMessage("Flagged **" + keep.size() + "** attachments to keep and **" + shard.size() + "** attachments to shard.").queue();
            CommandQuestions.readAttachmentsCached();
        }
        else if(command.getAsString().startsWith("approvemoment"))
        {
            String type = command.getAsString().split("-")[1];
            String id = command.getAsString().split("-")[2];

            String link = Mongo.QuestionsVotingDB.find(Filters.eq("attachmentID", id)).first().getString("link");
            Mongo.UserMomentsDB.updateOne(
                    Filters.eq("type", type),
                    Updates.combine(
                            Updates.push("attachments", link),
                            Updates.pull("queued", id)
                    )
            );
        }
        else if(command.getAsString().equals("queuedmoments"))
        {
            List<String> desc = new ArrayList<>();

            Mongo.UserMomentsDB.find().forEach(d -> desc.add(d.getString("type") + ": " + String.join(", ", d.getList("queued", String.class))));

            event.getChannel().sendMessage(String.join("\n", desc)).queue();
        }
        else if(command.getAsString().equals("cyclenamechanger"))
            NameChangeRoleEvent.cycleNameChangeRole();
        else if(command.getAsString().equals("checknamechanger"))
            NameChangeRoleEvent.checkNameChangeCycler();
        else if(command.getAsString().equalsIgnoreCase("ghostping"))
            GhostPingEvent.ghostPing();
        else if(command.getAsString().equals("autoapprovemoments"))
        {
            Mongo.UserMomentsDB.find().forEach(d ->
            {
                String type = d.getString("type");
                d.getList("queued", String.class).forEach(attachmentID ->
                {
                    String link = Mongo.QuestionsVotingDB.find(Filters.eq("attachmentID", attachmentID)).first().getString("link");
                    Mongo.UserMomentsDB.updateOne(Filters.eq("type", type), Updates.push("attachments", link));
                });
                Mongo.UserMomentsDB.updateOne(Filters.eq(d.getString("type"), type), Updates.set("queued", new ArrayList<>()));
            });
        }
        else if(command.getAsString().equals("updateconfig"))
        {
            BotConfig.init();

            event.getChannel().sendMessage("Config Updated!\n" + Mongo.Misc.find(Filters.eq("type", "config")).first()).queue();
        }
        else if(command.getAsString().startsWith("addreaction"))
        {
            String reactionId = command.getAsString().split("-")[1];
            Mongo.Misc.updateOne(Filters.eq("type", "config"), Updates.push("reactions_pool", reactionId));
        }
        else if(command.getAsString().equals("addmessagecounts"))
        {
            long start = System.currentTimeMillis();
            event.getChannel().getIterableHistory().forEachAsync(m -> {
                MessageLeaderboardHandler.addUserMessage(m.getGuild().getId(), m.getAuthor().getId(), m.getAuthor().getName());
                return true;
            }, t -> event.getChannel().sendMessage("Error: " + t.getMessage()).queue()).thenRun(() -> event.getChannel().sendMessage("Messages counted (Time: %s ms)! <@309135641453527040>".formatted(System.currentTimeMillis() - start)).queue());
        }
        else if(command.getAsString().startsWith("addstarboardpost"))
        {
            String messageID = command.getAsString().split("-")[1];
            event.getChannel().retrieveMessageById(messageID).queue(m ->
            {
                MessageReaction reaction = m.getReaction(Emoji.fromFormatted(BotConfig.STARBOARD_REACTION));
                if(reaction != null)
                {
                    StarboardPost post = new StarboardPost(m, reaction);
                    post.createPost();
                    event.getChannel().sendMessage(event.getUser().getAsMention() + " - Starboard Post added.").queue();
                }
                else event.getChannel().sendMessage(event.getUser().getAsMention() + " - Failed to add Starboard post.").queue();
            });
        }
        else return this.error("Invalid command!");

        event.reply(event.getUser().getAsMention() + " Done!").setEphemeral(true).queue();
        return true;
    }
}
