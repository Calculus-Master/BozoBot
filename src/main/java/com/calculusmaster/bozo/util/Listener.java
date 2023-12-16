package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.BozoBot;
import com.calculusmaster.bozo.commands.*;
import com.calculusmaster.bozo.commands.core.CommandData;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.calculusmaster.bozo.BozoBot.BOT_JDA;
import static com.calculusmaster.bozo.BozoBot.COMMANDS;
import static com.calculusmaster.bozo.util.BotConfig.BANNED_CHANNELS;

public class Listener extends ListenerAdapter
{
    private static boolean BOZOCORD_LOCK = false;

    public static void init()
    {
        CommandBozo.init();
        CommandStaffQuote.init();
        CommandRandomWackyLoadout.init();
        CommandRandomWackyName.init();
        CommandQuestions.init();
        CommandDev.init();
        CommandOhHeavens.init();
        CommandBaltiMoments.init();
        CommandJoyBoyMoments.init();
        CommandHandsomeMoments.init();
        CommandStolasMoments.init();
        CommandToxMoments.init();
        CommandAegisMoments.init();
        CommandHorsMoments.init();
        CommandSuggestBozo.init();
        CommandLFG.init();
        CommandRemoveNameChanger.init();
        CommandLeaderboard.init();
        CommandPoll.init();
        CommandReminders.init();
        CommandFood.init();
    }

    private CommandData findCommandData(Predicate<CommandData> predicate)
    {
        return COMMANDS.stream().filter(predicate).findFirst().orElse(null);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.getCommandName().equals(event.getName()));

        if(data == null)
        {
            BozoLogger.error(Listener.class, "Slash Command not found: " + event.getName());
            event.reply("An error has occurred.").setEphemeral(true).queue();
        }
        else if(!(event.getChannel() instanceof TextChannel))
        {
            BozoLogger.warn(Listener.class, "Attempted use of Slash Command (%s) used in non-TextChannel (%s, Type: %s)".formatted(event.getName(), event.getChannel().getName(), event.getChannel().getType()));
            event.reply("Slash Command usage outside of standard Text Channels is not currently supported.").setEphemeral(true).queue();
        }
        else
        {
            BozoLogger.info(Listener.class, "Parsing Slash Command: /" + event.getFullCommandName() + " " + event.getOptions().stream().map(o -> o.getName() + ":" + o.getAsString()).collect(Collectors.joining(" ")));
            data.getInstance().parseSlashCommand(event);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.getCommandName().equals(event.getName()));

        if(data == null) BozoLogger.error(Listener.class, "Autocomplete Slash Command not found: " + event.getName());
        else data.getInstance().parseAutocomplete(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.hasButton(event.getComponentId()));

        if(data == null)
        {
            BozoLogger.error(Listener.class, "Button ID not found: " + event.getComponentId());
            event.reply("An error has occurred.").setEphemeral(true).queue();
        }
        else data.getInstance().parseButtonInteraction(event);
    }

    private static final Map<String, CounterData> COUNTER_DATA_MAP = new HashMap<>();

    private static final Map<String, Pair<Integer, Integer>> INTERVALS = new HashMap<>();
    static
    {
        INTERVALS.put("983450314885713940", new Pair<>(5, 5)); //Bozocord
        INTERVALS.put("878207461117550642", new Pair<>(15, 12)); //Onyxcord
        INTERVALS.put("1000959891604779068", new Pair<>(7, 7)); //Avluscord
        INTERVALS.put("943354742384521267", new Pair<>(6, 6)); //Wiacord
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event)
    {
        Poll poll = Poll.getPoll(event.getMessageId());

        if(event.isFromGuild() && poll == null && event.getGuild().getId().equals("983450314885713940"))
            event.retrieveUser().queue(u ->
                    event.getGuild().retrieveMemberById("309135641453527040").queue(m ->
                            m.getUser().openPrivateChannel().queue(c ->
                                    c.sendMessage("Reaction {%s} removed by {%s} in the channel {%s}.".formatted(event.getReaction().getEmoji().getName(), u.getName(), event.getChannel().getAsMention())).queue()
                            )
                    )
            );

        if(!event.getUserId().equals("1069804190458708049") && poll != null && !poll.isAnonymous())
            poll.removeVote(event.getUserId());
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event)
    {
        boolean isBozocord = event.getGuild().getId().equals("983450314885713940");
        if(isBozocord && !event.getReaction().isSelf() && event.getReaction().getEmoji().getAsReactionCode().equals(BotConfig.STARBOARD_REACTION))
        {
            if(event.getReaction().getEmoji().getAsReactionCode().equals(BotConfig.STARBOARD_REACTION))
                Executors.newSingleThreadScheduledExecutor().schedule(() -> event.retrieveMessage().queue(this::checkAndUpdateStarboard), 5, TimeUnit.SECONDS);
        }

        Poll poll = Poll.getPoll(event.getMessageId());
        if(!event.getUserId().equals("1069804190458708049") && poll != null)
        {
            poll.addVote(event.getUserId(), event.getReaction().getEmoji().getAsReactionCode());
            if(poll.isAnonymous()) event.getReaction().removeReaction(event.retrieveUser().complete()).queue();
        }
    }

    private void checkAndUpdateStarboard(Message m)
    {
        MessageReaction reaction = Objects.requireNonNull(m.getReaction(Emoji.fromFormatted(BotConfig.STARBOARD_REACTION)));

        if(reaction.getCount() < BotConfig.STARBOARD_MIN_REACTIONS) return;

        StarboardPost post = StarboardPost.get(m.getId());
        if(post == null)
        {
            StarboardPost newPost = new StarboardPost(m, reaction);
            newPost.createPost();
        }
        else post.updateReactions(reaction.getCount());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if(!event.isFromGuild()) return;
        else if(Poll.getPoll(event.getMessageId()) != null) return;

        Random r = new Random();
        String guildID = event.getGuild().getId();
        String authorID = event.getAuthor().getId();
        String content = event.getMessage().getContentRaw().toLowerCase();

        boolean isBozocord = guildID.equals("983450314885713940");

        if(isBozocord && content.startsWith("<@1069804190458708049>"))
        {
            String query = content.substring("<@1069804190458708049>".length()).trim();

            if(GPTManager.ENABLED && !query.isEmpty())
            {
                if(!GPTManager.canRequest()) event.getChannel().sendMessage("Try again in a minute (rate limits).").queue();
                else event.getChannel().sendMessage(GPTManager.getResponse(query)).queue();

                return;
            }
        }

        if(!COUNTER_DATA_MAP.containsKey(guildID))
        {
            Pair<Integer, Integer> values = INTERVALS.getOrDefault(guildID, new Pair<>(5, 5));
            COUNTER_DATA_MAP.put(guildID, new CounterData(values.getFirst(), values.getSecond()));
        }

        CounterData data = COUNTER_DATA_MAP.get(guildID);
        data.update();

        if(MessageLeaderboardHandler.hasServer(guildID))
            MessageLeaderboardHandler.addUserMessage(guildID, authorID, event.getAuthor().getName());

        if(BANNED_CHANNELS.contains(event.getChannel().getId())) return;

        //Pin
        if(r.nextInt(8192) == 0)
        {
            event.getMessage().pin().queue();
            return;
        }

        //Content-Based Responses (Immediate)
        if(!event.getAuthor().isBot() && content.length() >= 500)
        {
            event.getChannel().sendMessage("i ain't reading all that").queue();
            event.getChannel().sendMessage("i'm happy for you tho").queue();
            event.getChannel().sendMessage("or sorry that happened").queue();
        }
        else if(content.length() <= 100 && r.nextFloat() < 0.01F && event.getGuild().retrieveMember(event.getAuthor()).complete().getRoles().stream().anyMatch(role -> role.getId().equals("1070462116655534101")))
        {
            StringBuilder modified = new StringBuilder();
            for(char c : event.getMessage().getContentRaw().toCharArray()) modified.append(r.nextBoolean() ? String.valueOf(c).toUpperCase() : String.valueOf(c).toLowerCase());
            event.getChannel().sendMessage(modified.toString()).queue();
        }

        if(r.nextFloat() < 0.2F && content.contains("rose"))
            event.getChannel().sendMessage("\"Strong hands he held a " + (r.nextFloat() < 0.2F ? "cock" : "rose") +  ",aura burn bright.\" - " + event.getMember().getEffectiveName()).queue();

        if(!event.getAuthor().isBot() && r.nextFloat() < 0.75F && content.contains("upended"))
        {
            if(r.nextFloat() < 0.8F)
            {
                event.getChannel().sendMessage("***THE UPENDED***").queue();
                event.getChannel().sendMessage("***A TITLE SUITABLE FOR THAT WHICH TURNS WORLDS UPSIDE DOWN***").queue();
            }
            else
            {
                event.getChannel().sendMessage("***THE APPENDED***").queue();
                event.getChannel().sendMessage("***A TITLE SUITABLE FOR THAT WHICH EXTENDS***").queue();
            }
        }

        if(isBozocord && r.nextFloat() < 0.1F && content.contains("mods"))
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " they're watching you...").queue();

        //General Responses (Delayed in Bozocord)
        if(data.messageCounterResponses >= data.responseInterval && !event.getAuthor().isBot() && r.nextFloat() < 0.05F)
        {
            List<String> oneWordResponses = new ArrayList<>(BotConfig.ONE_WORD_RESPONSES);
            oneWordResponses.add(event.getAuthor().getAsMention());

            String response;

            if(BotConfig.UNIQUE_RESPONSES.containsKey(authorID) && r.nextFloat() < 0.15F)
                response = BotConfig.UNIQUE_RESPONSES.get(authorID);
            else if(isBozocord && Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(role -> role.getId().equals("1015047797420085329")) && r.nextFloat() < 0.01F)
                response ="<#1020858333521002556>";
            else if(r.nextFloat() < 0.05F && r.nextFloat() < 0.35F && List.of("998041223489138738", "983450314885713943").contains(event.getChannel().getId()))
                response = BotConfig.D2_RESPONSES.get(r.nextInt(BotConfig.D2_RESPONSES.size()));
            else
                response = oneWordResponses.get(r.nextInt(oneWordResponses.size()));

            if(isBozocord && !BOZOCORD_LOCK)
            {
                BOZOCORD_LOCK = true;

                event.getChannel()
                        .sendTyping()
                        .delay((long)(BotConfig.BOZOCORD_MESSAGE_DELAY * 1000L), TimeUnit.MILLISECONDS)
                        .queue(v -> event.getChannel().sendMessage(response)
                                .queue(m ->
                                {
                                    BOZOCORD_LOCK = false;
                                    data.messageCounterResponses = 0;
                                }
                        ));
            }
            else
            {
                event.getChannel().sendMessage(response).queue();
                data.messageCounterResponses = 0;
            }
        }
        else if(event.getAuthor().isBot() && r.nextFloat() < (content.equalsIgnoreCase("best bot") ? 0.025F : 0.05F))
            event.getChannel().sendMessage("best bot").queue();

        //General Reactions
        if(data.messageCounterReactions >= data.reactionInterval && r.nextFloat() < 0.05F)
        {
            String emoji = BotConfig.REACTIONS_POOL.get(r.nextInt(BotConfig.REACTIONS_POOL.size()));

            event.getMessage().addReaction(Emoji.fromFormatted(emoji)).queue();

            data.messageCounterReactions = 0;
        }

        //Updating Questions Voting
        if(isBozocord && event.getChannel().getId().equals("998041223489138738") && !event.getMessage().getAttachments().isEmpty())
            event.getMessage().getAttachments().forEach(a ->
            {
                Document attachmentData = new Document(new LinkedHashMap<>());

                attachmentData.append("attachmentID", a.getId());
                attachmentData.append("link", a.getUrl());
                attachmentData.append("voters", new ArrayList<>());
                attachmentData.append("votes_keep", 0);
                attachmentData.append("votes_shard", 0);
                attachmentData.append("flag", "none");

                Mongo.QuestionsVotingDB.insertOne(attachmentData);
                BozoLogger.info(Listener.class, "Inserted new attachment data into QuestionsVotingDB (ID: " + a.getId() + ").");
            });
    }

    private static void sendOneWordResponse(String authorID, Random r, MessageReceivedEvent event, List<String> oneWordResponses, boolean isBozocord)
    {
        if(BotConfig.UNIQUE_RESPONSES.containsKey(authorID) && r.nextFloat() < 0.15F)
            event.getChannel().sendMessage(BotConfig.UNIQUE_RESPONSES.get(authorID)).queue();
        else if(isBozocord && Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(role -> role.getId().equals("1015047797420085329")) && r.nextFloat() < 0.05F)
            event.getChannel().sendMessage("<#1020858333521002556>").queue();
        else if(r.nextFloat() < 0.05F && r.nextFloat() < 0.35F && List.of("998041223489138738", "983450314885713943").contains(event.getChannel().getId()))
            event.getChannel().sendMessage(BotConfig.D2_RESPONSES.get(r.nextInt(BotConfig.D2_RESPONSES.size()))).queue();
        else event.getChannel().sendMessage(oneWordResponses.get(r.nextInt(oneWordResponses.size()))).queue();
    }

    private static class CounterData
    {
        private int messageCounterReactions = 0;
        private final int reactionInterval;
        private int messageCounterResponses = 0;
        private final int responseInterval;

        CounterData(int reactionInterval, int responseInterval)
        {
            this.reactionInterval = reactionInterval;
            this.responseInterval = responseInterval;
        }

        public void update()
        {
            this.messageCounterReactions++;
            this.messageCounterResponses++;
        }
    }
}
