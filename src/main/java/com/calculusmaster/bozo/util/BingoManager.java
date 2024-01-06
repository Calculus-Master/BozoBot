package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.BozoBot;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BingoManager
{
    public static final List<String> ENTRIES = new ArrayList<>();
    public static final List<Integer> FREE_SPACES = new ArrayList<>();
    private static final String PATH = System.getProperty("user.dir") + "/";
    private static final String BINGO_IMAGE_PATH = PATH + "latex2png/bingo_board_output.png";
    private static boolean[][] BOARD_COMPLETION = new boolean[5][5];
    private static final Bson QUERY = Filters.eq("type", "bingo_board");

    private static final ExecutorService UPDATER = Executors.newSingleThreadExecutor();

    public static void init()
    {
        Document data = Objects.requireNonNull(Mongo.Misc.find(QUERY).first());

        ENTRIES.addAll(data.getList("contents", String.class).stream().map(s -> s
                .replaceAll("(?<!\\\\)#", "\\\\#")
                .replaceAll("(?<!\\\\)_", "\\\\_")
                .replaceAll("(?<!\\\\)%", "\\\\%")
        ).toList());

        FREE_SPACES.addAll(data.getList("free_spaces", Integer.class));
    }

    //Bingo Board Generator
    public static void createBingoBoard()
    {
        BozoLogger.info(BingoManager.class, "Bingo Board: Starting to create a new one!");

        //Write entries to file
        String inputPath = PATH + "bingo_generator/input.txt";
        try(FileWriter w = new FileWriter(inputPath))
        {
            List<String> write = new ArrayList<>(List.copyOf(ENTRIES));

            int fsIndex;
            if(!FREE_SPACES.isEmpty())
                fsIndex = FREE_SPACES.get(new Random().nextInt(FREE_SPACES.size()));
            else fsIndex = new Random().nextInt(ENTRIES.size());

            String freeSpace = ENTRIES.get(fsIndex);

            write.remove(fsIndex);
            Collections.shuffle(write);
            write.add(0, freeSpace);

            w.write(String.join("\n", write));
        } catch(Exception ignored) {}

        BozoLogger.info(BingoManager.class, "Bingo Board: Entries finished being read from the database.");

        //Generation - script build
        String pyPath = PATH + "bingo_generator/bingo_generator.py";
        String boardTemplatePath = PATH + "bingo_generator/board_template.tex";
        String docTemplatePath = PATH + "bingo_generator/document_template.tex";
        String outputPath = PATH + "latex2png/output.tex";

        String[] args = {"python3", pyPath, "1", "-b", boardTemplatePath, "-d", docTemplatePath, "<", inputPath, ">", outputPath};

        BozoLogger.info(BingoManager.class, "Bingo Board: Starting to create the shell script for bingo_generator!");

        //Write command to shell script
        String shellPath = PATH + "bingo_generator/temp_gen_bingo.sh";
        try(FileWriter w = new FileWriter(shellPath))
        {
            w.write(String.join(" ", args));
        } catch(Exception ignored) {}

        BozoLogger.info(BingoManager.class, "Bingo Board: bingo_generator shell script has been written.");

        //Make shell script executable & run it
        try
        {
            BozoLogger.info(BingoManager.class, "Bingo Board: Starting to make shell script executable!");

            Process chmodProcess = new ProcessBuilder("chmod", "+x", shellPath).redirectErrorStream(true).inheritIO().start();
            int exc = chmodProcess.waitFor();

            BozoLogger.info(BingoManager.class, "Bingo Board: Finished making the shell script executable! Exit code: " + exc + ".");

            BozoLogger.info(BingoManager.class, "Bingo Board: Starting to execute shell script!");

            Process p = new ProcessBuilder(shellPath).redirectErrorStream(true).inheritIO().start();
            int ec = p.waitFor();
            BozoLogger.info(BingoManager.class, "Bingo Board: Executing the Bingo Generator shell script is done! Exit code: " + ec + ".");
        }
        catch(Exception ignored) {}

        //Run latex2png
        try
        {
            BozoLogger.info(BingoManager.class, "Bingo Board: Starting to run latex2png!");
            args = new String[]{"./LaTeX2PNG", "-i", "output.tex", "-o", BINGO_IMAGE_PATH};

            Process p = new ProcessBuilder(args).redirectErrorStream(true).directory(new File(PATH + "latex2png/")).inheritIO().start();
            int ec = p.waitFor();
            BozoLogger.info(BingoManager.class, "Bingo Board: LaTeX2PNG is done! Exit code: " + ec + ".");
        }
        catch(Exception ignored) {}

        try
        {
            BozoLogger.info(BingoManager.class, "Bingo Board: Starting to scale down output image.");

            int w = 2000, h = 2000;
            Image image = ImageIO.read(new File(BINGO_IMAGE_PATH))
                    .getSubimage(0, 0, 16969, 16969)
                    .getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);

            BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            bufferedImage.getGraphics().drawImage(image, 0, 0, null);

            BozoLogger.info(BingoManager.class, "Bingo Board: Finished editing output image, writing to disk...");

            ImageIO.write(bufferedImage, "png", new File(BINGO_IMAGE_PATH));
            ImageIO.write(bufferedImage, "png", new File(PATH + "latex2png/bingo_board_output_unmarked.png"));

            BozoLogger.info(BingoManager.class, "Bingo Board: Output image has been written to disk.");
        }
        catch (Exception ignored) {}

        //Reset board data
        Document data = Objects.requireNonNull(Mongo.Misc.find(QUERY).first());
        String messageID = data.getString("message_id");
        if(!messageID.isEmpty())
        {
            TextChannel channel = Objects.requireNonNull(BozoBot.BOT_JDA
                    .getGuildById("983450314885713940")
                    .getChannelById(TextChannel.class, "1192397256649867275"));

            channel.retrieveMessageById(messageID).queue(m -> {
                if(data.getInteger("bingo_count") == 0 && m.isPinned()) m.unpin().queue();
                m.delete().queue();
            });
        }

        OffsetDateTime odt = OffsetDateTime.now();
        String date = "%s %s, %s".formatted(odt.getMonth().getDisplayName(TextStyle.FULL, Locale.US), odt.getDayOfMonth(), odt.getYear());

        BOARD_COMPLETION = new boolean[5][5];
        Mongo.Misc.updateOne(QUERY, Updates.combine(
                Updates.set("completion", new ArrayList<String>()),
                Updates.set("date", date),
                Updates.set("bingo_count", 0),
                Updates.set("message_id", "")
        ));

        BingoManager.sendBingoBoard(true);
    }

    public static void sendBingoBoard()
    {
        BingoManager.sendBingoBoard(false);
    }

    public static void sendBingoBoard(boolean setID)
    {
        TextChannel channel = Objects.requireNonNull(BozoBot.BOT_JDA
                .getGuildById("983450314885713940")
                .getChannelById(TextChannel.class, "1192397256649867275"));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Daily Bingo – " + Mongo.Misc.find(QUERY).first().getString("date"))
                .setImage("attachment://bingo.png");

        int bingoCount = BingoManager.getBingoCount();
        if(bingoCount > 0) embed.setDescription("Bingo(s): **" + bingoCount + "**");

        channel.sendMessageEmbeds(embed.build())
                .setFiles(FileUpload.fromData(new File(BINGO_IMAGE_PATH), "bingo.png"))
                .queue(m -> {
                    if(setID)
                    {
                        Mongo.Misc.updateOne(QUERY, Updates.set("message_id", m.getId()));

                        if(!m.isPinned()) m.pin().queue();
                    }
                });
    }

    public static void editBingoBoard()
    {
        TextChannel channel = Objects.requireNonNull(BozoBot.BOT_JDA
                .getGuildById("983450314885713940")
                .getChannelById(TextChannel.class, "1192397256649867275"));

        Document data = Objects.requireNonNull(Mongo.Misc.find(QUERY).first());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Daily Bingo – " + data.getString("date"))
                .setImage("attachment://bingo.png");

        int bingoCount = BingoManager.getBingoCount();
        if(bingoCount > 0) embed.setDescription("Bingo(s): **" + bingoCount + "**");

        channel.editMessageById(data.getString("message_id"), new MessageEditBuilder()
                .setEmbeds(embed.build())
                .setFiles(FileUpload.fromData(new File(BINGO_IMAGE_PATH), "bingo.png"))
                .build()).queue();
    }

    public static void completeSquare(String inputString, int x, int y)
    {
        BOARD_COMPLETION[x][y] = true;

        int startX = 0, startY = 0;
        int gridSize = 2000 / 5;

        try
        {
            BufferedImage bingoBoard = ImageIO.read(new File(BINGO_IMAGE_PATH));
            Image redX = ImageIO.read(new File(PATH + "latex2png/red_x.png")).getScaledInstance(gridSize, gridSize, BufferedImage.SCALE_DEFAULT);

            //Swap x & y so draw area matches the board (letter = row (y), number = column (x))
            int temp = y;
            y = x;
            x = temp;

            bingoBoard.getGraphics().drawImage(
                    redX,
                    startX + x * gridSize,
                    startY + y * gridSize,
                    null
            );

            ImageIO.write(bingoBoard, "png", new File(BINGO_IMAGE_PATH));
        } catch(Exception ignored) {}

        Mongo.Misc.updateOne(QUERY, Updates.push("completion", inputString));

        int oldBingoCount = BingoManager.getBingoCount();
        int bingoCount = BingoManager.checkBingo();

        if(bingoCount > oldBingoCount)
        {
            Mongo.Misc.updateOne(QUERY, Updates.set("bingo_count", bingoCount));

            TextChannel channel = Objects.requireNonNull(BozoBot.BOT_JDA
                    .getGuildById("983450314885713940")
                    .getChannelById(TextChannel.class, "1192397256649867275"));

            channel.sendMessage("Bingo! (Count: %s -> %s)".formatted(oldBingoCount, bingoCount)).queue();

            BingoManager.sendBingoBoard();
            BingoManager.editBingoBoard();
        }
    }

    public static boolean isSquareCompleted(int x, int y)
    {
        return BOARD_COMPLETION[x][y];
    }

    private static int getBingoCount()
    {
        return Objects.requireNonNull(Mongo.Misc.find(QUERY).first()).getInteger("bingo_count");
    }

    private static int checkBingo()
    {
        int count = 0;

        //Check rows
        for(int i = 0; i < 5; i++)
        {
            boolean bingo = true;
            for(int j = 0; j < 5; j++)
                if (!BOARD_COMPLETION[i][j]) { bingo = false; break; }
            if(bingo) count++;
        }

        //Check columns
        for(int i = 0; i < 5; i++)
        {
            boolean bingo = true;
            for(int j = 0; j < 5; j++) if(!BOARD_COMPLETION[j][i]) { bingo = false; break; }
            if(bingo) count++;
        }

        //Check diagonals
        boolean bingo = true;
        for(int i = 0; i < 5; i++) if(!BOARD_COMPLETION[i][i]) { bingo = false; break; }
        if(bingo) count++;

        bingo = true;
        for(int i = 0; i < 5; i++) if(!BOARD_COMPLETION[i][4 - i]) { bingo = false; break; }
        if(bingo) count++;

        return count;
    }

    public static int[] parseSquareCoordinate(String input)
    {
        if(!BingoManager.isValidSquare(input)) return null;

        int x = List.of("A", "B", "C", "D", "E").indexOf(input.substring(0, 1));
        int y = Integer.parseInt(input.substring(1)) - 1;

        return new int[]{x, y};
    }

    public static boolean isValidSquare(String input)
    {
        return input.length() == 2 && List.of("A", "B", "C", "D", "E").contains(input.substring(0, 1)) && List.of("1", "2", "3", "4", "5").contains(input.substring(1));
    }

    //Board Free Space Updater
    private static void updateFreeSpaces()
    {
        UPDATER.submit(() -> Mongo.Misc.updateOne(QUERY, Updates.set("free_spaces", FREE_SPACES)));
    }

    public static void addFreeSpace(int index)
    {
        FREE_SPACES.add(index);
        BingoManager.updateFreeSpaces();
    }

    public static void removeFreeSpace(int index)
    {
        FREE_SPACES.remove((Integer)index);
        BingoManager.updateFreeSpaces();
    }

    //Bingo Board Entry Updater
    private static void updateEntries()
    {
        UPDATER.submit(() -> Mongo.Misc.updateOne(QUERY, Updates.set("contents", ENTRIES)));
    }

    public static void addEntry(String entry)
    {
        ENTRIES.add(entry);
        BingoManager.updateEntries();
    }

    public static String getEntry(int index)
    {
        return ENTRIES.get(index);
    }

    public static void removeEntry(int index)
    {
        ENTRIES.remove(index);
        BingoManager.updateEntries();

        if(FREE_SPACES.contains(index)) BingoManager.removeFreeSpace(index);
        else
        {
            for(int i = 0; i < FREE_SPACES.size(); i++)
            {
                int fsi = FREE_SPACES.get(i);
                if(fsi > index) FREE_SPACES.set(i, fsi - 1);
            }
        }
    }

    public static void editEntry(int index, String entry)
    {
        ENTRIES.set(index, entry);
        BingoManager.updateEntries();
    }
}
