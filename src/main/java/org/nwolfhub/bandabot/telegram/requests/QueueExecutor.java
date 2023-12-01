package org.nwolfhub.bandabot.telegram.requests;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Component
public class QueueExecutor {
    private final TelegramBot bot;
    public final Long mainChat;
    private final Queue<PendingRequest> requests = new ArrayDeque<>();
    private Thread running;


    public QueueExecutor(TelegramBot bot, Long mainChat) {
        this.bot = bot;
        this.mainChat = mainChat;
        running = senderThread();
        running.start();
    }

    @PreDestroy
    private void destroy() {
        System.out.println("Closing runner thread");
        running.interrupt();
    }

    public void executeRequest(SendMessage request, Runnable onResponse, List<SendResponse> responses) {
        PendingRequest pending = new PendingRequest(onResponse, request, responses);
        requests.offer(pending);
    }
    public void executeRequest(SendMessage request) {
        PendingRequest pending = new PendingRequest(() -> {}, request, new ArrayList<>());
        requests.offer(pending);
    }

    public void executeRequestNoQueue(SendPhoto request) {
        bot.execute(request);
    }
    public void executeRequestNoQueue(AnswerCallbackQuery request) {
        bot.execute(request);
    }



    private Thread senderThread() {
        return new Thread(() -> {
            while (true) {
                if(!requests.isEmpty()) {
                    PendingRequest taken = requests.poll();
                    SendResponse response = bot.execute(taken.request);
                    if(!response.isOk() && response.errorCode()==429) {
                        requests.offer(taken);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        taken.setResponses(List.of(response));
                        taken.onResponse.run();
                    }
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

}