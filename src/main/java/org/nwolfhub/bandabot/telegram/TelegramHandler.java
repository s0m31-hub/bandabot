package org.nwolfhub.bandabot.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.nwolfhub.bandabot.telegram.requests.QueueExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramHandler {
    private final TelegramBot bot;

    private final QueueExecutor executor;
    private final List<Long> admins;



    public TelegramHandler(TelegramBot bot, QueueExecutor executor, List<Long> admins) {
        this.bot = bot;
        this.executor = executor;
        this.admins = admins;
    }

    public void startListening() {
        bot.setUpdatesListener(list -> {
            for (Update update : list) {
                onUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void onUpdate(Update update) {
        if(update.message()!=null) {
            if(update.message().text()!=null) {
                String text = update.message().text();
                String command = text.toLowerCase();
                Long chat = update.message().chat().id();
                if(command.equals("/start")) {
                    executor.executeRequestNoQueue(new SendMessage(chat, "Успешный запуск"), () -> {}, new ArrayList<>());
                }
            }
        }
    }
}
