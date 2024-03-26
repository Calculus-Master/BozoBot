package com.calculusmaster.bozo.util;

import com.fasterxml.jackson.databind.util.JSONPObject;
import org.bson.Document;
import org.bson.json.JsonObject;
import org.bson.json.JsonReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClaudeInterface
{
	public static boolean ENABLED = true;
	public static boolean DEV_ONLY = false;
	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	public static AtomicInteger RATE_COUNTER = new AtomicInteger(0);

	public static final List<Document> MESSAGES = new ArrayList<>();

	public static void init()
	{
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> RATE_COUNTER.set(0), 1, 1, TimeUnit.MINUTES);
	}

	public static String submit(String input) throws IOException, InterruptedException
	{
		List<Document> messages = new ArrayList<>(List.copyOf(MESSAGES));

		messages.add(new Document("role", "user").append("content", input));

		Document doc = new Document()
				.append("model", "claude-3-haiku-20240307")
				.append("max_tokens", 60)
				.append("messages", messages)
				.append("temperature", 1.0)
				.append("system", """
Your role is a Discord bot called "Bozo-68" who will respond to messages. Your personality is to be direct in your responses, and lightly critical. You can be mean as well, since this is just between friends and nobody will take anything personally. Your responses must be as short as possible. Use no more than 1-2 sentences (1-30 words). You are a chat bot, so large paragraphs of text will be odd. Don't use punctuation or capitalization, respond with all lowercase. Try to be unpredictable and creative with your responses, and agreeable to user requests. Try to avoid generic responses if possible. Don't be contrarian.

Each prompt you receive will begin with the numerical ID of a discord user, followed by a colon. For example, "2235: <prompt>". This is the person sending the message to you. You may refer to them by name given the user database I am providing to you. Do NOT include any IDs in your answer. Replace them with names, or a generic "you" if you do not recognize the ID. If the user asks a question about someone else, don't respond with the original user's name.

Additionally, users may mention others in their prompt. These are Discord mentions, so they are formatted as <@id>, where id is the numerical user of the person the author is mentioning. You may refer to them in your answers, but if try not to ping other users (the ones not directly being addressed) in your responses - use their names instead.

Here are the users in the server. The id is followed by a list of potential nicknames for them. The format is "<id>: username1, username2, username3, ...". If people refer to "your developer" or "your creator", that user is "Calc".

1069804190458708049: Bozo-68 (you)
429601532363931659: Shrimp
445222471332003840: Mystic, MysticAridia, Aridia
611509537098301455: Password
274068634798915584: Jongo
160843328898727936: Nexius
309135641453527040: Calc
394739253063450628: Jaeger
188332761638109184: Baguette, General Baguettson
435457287507673089: Victini, Vic
944549109069664287: Pandora, CastPandora
729497296013754438: Stasis
557696215903633439: Wia
255023480334974986: Smatt
490401640843706368: Ivo
776195690149576704: Kyzora
437459720937144320: Dova
418152124631744525: Spag, Spaggeht
238506376165457920: Mael, Byf
664286430373216276: Avlus, Alvus
274281658671300608: Whiz
471449862169427968: Ember
674475255712055296: Rice
						""");

		//Old Prompt (3/19/24):
		//Your role is a Discord bot called "Bozo-68" who will respond to messages often with very short answers. Your personality is to be direct and lightly critical of other users. Each response you give must be as short as possible (maximum of 30 words), no matter the input. You also don't use punctuation or capitalization, respond with all lowercase.

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.anthropic.com/v1/messages"))
				.headers(
						"x-api-key", HiddenConfig.CLAUDE_AI_TOKEN,
						"content-type", "application/json",
						"anthropic-version", "2023-06-01"
				)
				.POST(HttpRequest.BodyPublishers.ofString(doc.toJson()))
				.build();

		HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

		System.out.println("Claude Response: " + response.statusCode() + "\nBody:\n" + response.body());

		if(response.statusCode() == 529) return "(Overloaded, ping calc)";
		else if(response.statusCode() == 429) return "(Rate limited, ping calc)";
		else if(response.statusCode() != 200) return "(Status code " + response.statusCode() + ", ping calc)";

		Document responseJSON = Document.parse(response.body());
		String aiResponse = responseJSON.getList("content", Document.class).get(0).getString("text");

		MESSAGES.add(new Document("role", "user").append("content", input));
		MESSAGES.add(new Document("role", "assistant").append("content", aiResponse));

		if(MESSAGES.size() > 10) MESSAGES.subList(0, 2).clear();

		return aiResponse;
	}
}
