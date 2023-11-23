package org.nwolfhub.bandabot.telegram.requests;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AbstractSendRequest;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

@Component
public class QueueExecutor {
    private final TelegramBot bot;
    private final Queue<PendingRequest> requests = new ArrayDeque<>();
    private Thread running;


    public QueueExecutor(TelegramBot bot) {
        this.bot = bot;
        running = senderThread();
        running.start();
    }

    @PreDestroy
    private void destroy() {
        System.out.println("Closing runner thread");
        running.interrupt();
    }

    public void executeRequest(Class<?> type, Object request, Runnable onResponse, List<BaseResponse> responses) {
        PendingRequest pending = new PendingRequest(onResponse, request, type, responses);
        requests.add(pending);
    }

    private Thread senderThread() {
        return new Thread(() -> {
            while (true) {
                if(!requests.isEmpty()) {
                    PendingRequest taken = requests.poll();
                    BaseResponse response = bot.execute((BaseRequest<AbstractSendRequest, BaseResponse>) taken.request);
                    if(!response.isOk() && response.errorCode()==429) {
                        requests.add(taken);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    taken.setResponses(List.of(response));
                    taken.onResponse.run();
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