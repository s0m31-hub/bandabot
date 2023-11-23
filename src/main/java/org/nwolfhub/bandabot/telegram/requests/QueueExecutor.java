package org.nwolfhub.bandabot.telegram.requests;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AbstractSendRequest;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

@Component
public class QueueExecutor {
    private final TelegramBot bot;
    private final Queue<PendingRequest> requests = new ArrayDeque<>();


    public QueueExecutor(TelegramBot bot) {
        this.bot = bot;
    }

    public Object executeRequest(Class<?> type, Object request, Runnable onResponse) {
        Thread requestingThread
    }
}