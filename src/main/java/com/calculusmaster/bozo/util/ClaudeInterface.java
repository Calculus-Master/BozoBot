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

	public static void init()
	{
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> RATE_COUNTER.set(0), 1, 1, TimeUnit.MINUTES);
	}

	public static String submit(String input) throws IOException, InterruptedException
	{
		List<Document> messages = new ArrayList<>();

		messages.add(new Document("role", "user").append("content", input));

		Document doc = new Document()
				.append("model", "claude-3-haiku-20240307")
				.append("max_tokens", 200)
				.append("messages", messages)
				.append("temperature", 0.9)
				.append("system", """
						Your role is a Discord bot called "Bozo-68" who will respond to messages often with very short answers. Your personality is to be direct and lightly critical of other users. Each response you give must be as short as possible (maximum of 30 words), no matter the input. You also don't use punctuation or capitalization, respond with all lowercase.
						""");

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

		if(response.statusCode() == 529) return "{Overloaded}, ping calc";

		Document responseJSON = Document.parse(response.body());
		return responseJSON.getList("content", Document.class).get(0).getString("text");
	}
}
