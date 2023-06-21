package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CommandQuestions extends Command
{
    public static final String VOTE_KEEP = "QUESTIONS_VOTE_KEEP";
    public static final String VOTE_SHARD = "QUESTIONS_VOTE_SHARD";
    public static final String JONGOS_MID = "JONGOS_MID";

    public static final String VOTE_MOMENT = "QUESTIONS_VOTE_MOMENT";
    public static final String VOTE_MOMENT_BALTI = VOTE_MOMENT + "_BALTI";
    public static final String VOTE_MOMENT_JOYBOY = VOTE_MOMENT + "_JOYBOY";
    public static final String VOTE_MOMENT_HANDSOME = VOTE_MOMENT + "_HANDSOME";
    public static final String VOTE_MOMENT_STOLAS = VOTE_MOMENT + "_STOLAS";
    public static final String VOTE_MOMENT_TOX = VOTE_MOMENT + "_TOX";
    public static final String VOTE_MOMENT_AEGIS = VOTE_MOMENT + "_AEGIS";
    public static final String VOTE_MOMENT_HORS = VOTE_MOMENT + "_HORS";

    public static final List<String> ATTACHMENTS = new ArrayList<>();

    public static void readAttachmentsCached()
    {
        Mongo.QuestionsVotingDB.find().filter(Filters.not(Filters.eq("flag", "shard"))).forEach(d -> CommandQuestions.ATTACHMENTS.add(d.getString("link")));
    }

    public static void init()
    {
        CommandData
                .create("questions")
                .withConstructor(CommandQuestions::new)
                .withButtons(VOTE_KEEP, VOTE_SHARD, JONGOS_MID, VOTE_MOMENT)
                .withCommand(Commands
                        .slash("questions", "Gets a random questions moment.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        TextChannel channel = event.getGuild().getChannelById(TextChannel.class, "998041223489138738");
        if(channel == null) return this.error("Can't access questions channel or it doesn't exist.");

        Random r = new Random();

        String url = ATTACHMENTS.get(r.nextInt(ATTACHMENTS.size()));
        String id = url.split("/")[5];
        this.embed.setImage(url);

        Button keep = Button.of(ButtonStyle.SUCCESS, VOTE_KEEP + "-" + id, "Keep");
        Button shard = Button.of(ButtonStyle.DANGER, VOTE_SHARD + "-" + id, "Shard");
        Button mid = Button.of(ButtonStyle.SECONDARY, JONGOS_MID + "-" + id, "Jongo's Mid");

        Button balti = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_BALTI + "-" + id, "Balti");
        Button joyboy = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_JOYBOY + "-" + id, "Joyboy");
        Button handsome = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_HANDSOME + "-" + id, "Handsome");
        Button stolas = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_STOLAS + "-" + id, "Stolas");

        Button tox = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_TOX + "-" + id, "Tox");
        Button aegis = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_AEGIS + "-" + id, "Aegis");
        Button hors = Button.of(ButtonStyle.SECONDARY, VOTE_MOMENT_HORS + "-" + id, "Hors");

        ReplyCallbackAction a = event.replyEmbeds(this.embed.build());

        //Shard/Keep
        if(Mongo.QuestionsVotingDB.find(Filters.eq("attachmentID", id)).first().getString("flag").equals("none"))
            a.addActionRow(keep, shard, mid);

        //Moments TODO Remove if already a moment - Cache? Optimize
        a
                .addActionRow(balti, joyboy, handsome, stolas)
                .addActionRow(tox, aegis, hors)
                .queue();

        this.embed = null;
        this.response = "";

        return true;
    }

    @Override
    protected boolean buttonLogic(ButtonInteractionEvent event)
    {
        String attachmentID = event.getComponentId().split("-")[1];

        if(event.getComponentId().contains(VOTE_MOMENT))
        {
            String type;
            if(event.getComponentId().startsWith(VOTE_MOMENT_BALTI)) type = "balti";
            else if(event.getComponentId().startsWith(VOTE_MOMENT_JOYBOY)) type = "joyboy";
            else if(event.getComponentId().startsWith(VOTE_MOMENT_HANDSOME)) type = "handsome";
            else if(event.getComponentId().startsWith(VOTE_MOMENT_STOLAS)) type = "stolas";
            else if(event.getComponentId().startsWith(VOTE_MOMENT_TOX)) type = "tox";
            else if(event.getComponentId().startsWith(VOTE_MOMENT_AEGIS)) type = "aegis";
            else if(event.getComponentId().startsWith(VOTE_MOMENT_HORS)) type = "hors";
            else return this.error("Invalid moment type!");

            Document data = Objects.requireNonNull(Mongo.UserMomentsDB.find(Filters.eq("type", type)).first());

            if(data.getList("queued", String.class).contains(attachmentID))
                return this.error("This moment is already queued!", true);
            else if(data.getList("attachments", String.class).contains(attachmentID))
                return this.error("This moment has already been added!", true);
            else
            {
                List<String> all = new ArrayList<>();
                Mongo.UserMomentsDB.find().forEach(d -> {
                    all.addAll(d.getList("queued", String.class));
                    all.addAll(d.getList("attachments", String.class));
                });

                if(all.contains(attachmentID) || all.stream().anyMatch(s -> s.contains(attachmentID)))
                    return this.error("Another user already has this moment either queued or as part of their attachment pool.", true);

                Mongo.UserMomentsDB.updateOne(Filters.eq("type", type), Updates.push("queued", attachmentID));

                Button keep = Button.of(ButtonStyle.SUCCESS, VOTE_KEEP + "-" + attachmentID, "Keep");
                Button shard = Button.of(ButtonStyle.DANGER, VOTE_SHARD + "-" + attachmentID, "Shard");
                Button mid = Button.of(ButtonStyle.SECONDARY, JONGOS_MID + "-" + attachmentID, "Jongo's Mid");

                event.editComponents().setActionRow(keep, shard, mid).queue();
                Member me = event.getGuild().retrieveMemberById("309135641453527040").complete();
                event.getChannel().sendMessage(event.getUser().getAsMention() + ": Moment queued successfully! " + me.getAsMention() + " (" + attachmentID + ")").queue();

                this.response = "";
                this.embed = null;
            }

            return true;
        }
        else
        {
            boolean keep = event.getComponentId().startsWith(VOTE_KEEP);
            Document data = Mongo.QuestionsVotingDB.find(Filters.eq("attachmentID", attachmentID)).first();

            if(data == null) return this.error("Attachment not found!");
            else if(event.getComponentId().startsWith(JONGOS_MID))
            {
                if(event.getUser().getId().equals("309135641453527040"))
                    this.response = data + "";
                else
                {
                    Member jongo = event.getGuild().retrieveMemberById("274068634798915584").complete();
                    this.response = new Random().nextFloat() < 0.1F ? jongo.getAsMention() + "\n" : "I agree.";
                }

                return true;
            }
            else if(data.getList("voters", String.class).contains(event.getUser().getId())) return this.error(event.getUser().getAsMention() + " You've already voted on this attachment!", true);
            else
            {
                Bson query = Filters.eq("attachmentID", attachmentID);

                if(keep) Mongo.QuestionsVotingDB.updateOne(query, Updates.inc("votes_keep", 1));
                else Mongo.QuestionsVotingDB.updateOne(query, Updates.inc("votes_shard", 1));

                Mongo.QuestionsVotingDB.updateOne(query, Updates.push("voters", event.getUser().getId()));

                this.ephemeral = true;
                this.response = event.getUser().getAsMention() + " You successfully voted to " + (keep ? "keep" : "shard") + " this attachment!";

                TextChannel general = event.getGuild().getChannelById(TextChannel.class, "1069872555541938297");
                Member me = event.getGuild().retrieveMemberById("309135641453527040").complete();

                if(me != null && general != null && data.getString("flag").equals("none"))
                {
                    int thresh = 4;
                    if(data.getInteger("votes_keep") + 1 >= thresh)
                        general.sendMessage(me.getAsMention() + " – Attachment ID {%s} has been flagged **to be kept**!".formatted(data.getString("attachmentID"))).queue();
                    else if(data.getInteger("votes_shard") + 1 >= thresh)
                        general.sendMessage(me.getAsMention() + " – Attachment ID {%s} has been flagged **for removal**!".formatted(data.getString("attachmentID"))).queue();
                }

                return true;
            }
        }
    }
}
