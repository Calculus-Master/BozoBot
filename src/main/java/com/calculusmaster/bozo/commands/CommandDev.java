package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.events.IdiotListEvent;
import com.calculusmaster.bozo.events.NameChangeRoleEvent;
import com.calculusmaster.bozo.events.TimeManager;
import com.calculusmaster.bozo.util.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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
        else if(command.getAsString().equalsIgnoreCase("ghostping"))
            IdiotListEvent.triggerIdiotListPing();
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

            Document d = Mongo.Misc.find(Filters.eq("type", "config")).first();
            d.put("reactions_pool", d.getList("reactions_pool", String.class).size());
            event.getChannel().sendMessage("Config Updated!\n" + d).queue();
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
        else if(command.getAsString().equals("updatetimeintervals"))
            TimeManager.readIntervals();
        else if(command.getAsString().equals("updatetimedata"))
            TimeManager.readTimes();
        else if(command.getAsString().startsWith("downloademoji"))
        {
            String path = command.getAsString().split("-")[1];
            BozoBot.BOT_JDA.getGuildById("983450314885713940").retrieveEmojis().queue(list -> {
                for(RichCustomEmoji rce : list)
                {
                    System.out.println("Downloading " + rce.getName() + "...");
                    rce.getImage().downloadToFile(new File(path + "/" + rce.getName() + ".png"));
                }
            });
        }
        else if(command.getAsString().equals("generatebingoboard"))
            Executors.newSingleThreadScheduledExecutor().execute(BingoManager::createBingoBoard);
        else if(command.getAsString().startsWith("completebingosquare"))
        {
           String input = command.getAsString().split("-")[1];

           int[] parsed = BingoManager.parseSquareCoordinate(input);
           BingoManager.completeSquare(input, parsed[0], parsed[1]);
        }
        else if(command.getAsString().equals("scaledownbingoboard"))
        {
            try
            {
                BozoLogger.info(BingoManager.class, "Bingo Board: Starting to scale down output image.");

                int w = 2000, h = 2000;
                Image image = ImageIO.read(new File(System.getProperty("user.dir") + "/latex2png/bingo_board_output.png"))
                        .getSubimage(0, 0, 16969, 16969)
                        .getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);

                BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                bufferedImage.getGraphics().drawImage(image, 0, 0, null);

                BozoLogger.info(BingoManager.class, "Bingo Board: Finished editing output image, writing to disk...");

                ImageIO.write(bufferedImage, "png", new File(System.getProperty("user.dir") + "/latex2png/bingo_board_output.png"));
                ImageIO.write(bufferedImage, "png", new File(System.getProperty("user.dir") + "/latex2png/bingo_board_output_unmarked.png"));

                BozoLogger.info(BingoManager.class, "Bingo Board: Output image has been written to disk.");
            }
            catch(Exception e)
            {
                BozoLogger.error(BingoManager.class, "Bingo Board: Failed to scale down output image! Exception: \n");
                e.printStackTrace();
            }
        }
        else if(command.getAsString().equals("memory"))
        {
            Runtime r = Runtime.getRuntime();
            long total = r.totalMemory();
            long free = r.freeMemory();
            event.getChannel().sendMessage("Free: %s MB (%s B)\nTotal: %s MB (%s B)".formatted(free / 1024L / 1024L, free, total / 1024L / 1024L, total)).queue();
        }
        else if(command.getAsString().equals("manualbozo"))
        {
            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById("983456276803624961")).queue();
        }
        else if(command.getAsString().equals("resetnickname"))
        {
            event.getGuild().modifyNickname(event.getGuild().getMemberById("1069804190458708049"), BozoBot.BOT_JDA.getSelfUser().getName()).queue();
        }
        else if(command.getAsString().equals("toggleai"))
        {
            ClaudeInterface.ENABLED = !ClaudeInterface.ENABLED;
            event.getChannel().sendMessage("AI Enabled: " + ClaudeInterface.ENABLED).queue();
        }
        else if(command.getAsString().equals("toggleaidevonly"))
        {
            ClaudeInterface.DEV_ONLY = !ClaudeInterface.DEV_ONLY;
            event.getChannel().sendMessage("AI Dev Only: " + ClaudeInterface.DEV_ONLY).queue();
        }
        else if(command.getAsString().equals("getairatelimit"))
        {
            event.getChannel().sendMessage("Rate Limit Counter: " + ClaudeInterface.RATE_COUNTER.get()).queue();
        }
        else return this.error("Invalid command!");

        event.reply(event.getUser().getAsMention() + " Done!").setEphemeral(true).queue();
        return true;
    }
}
