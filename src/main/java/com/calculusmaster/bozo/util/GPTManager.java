package com.calculusmaster.bozo.util;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GPTManager
{
    public static boolean ENABLED = false;

    private static OpenAiService SERVICE;
    private static AtomicInteger requests = new AtomicInteger(0);

    public static void init()
    {
        if(!ENABLED) return;

        SERVICE = new OpenAiService(HiddenConfig.OPEN_AI_TOKEN);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> requests.set(0), 0, 1, TimeUnit.MINUTES);
    }

    public static boolean canRequest()
    {
        return requests.get() <= 3;
    }

    public static String getResponse(String input)
    {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo-0613")
                .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), input)))
                .maxTokens(256)
                .build();

        requests.getAndIncrement();

        return SERVICE.createChatCompletion(request).getChoices().get(0).getMessage().getContent();
    }
}
