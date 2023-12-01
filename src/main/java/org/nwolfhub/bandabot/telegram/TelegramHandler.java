package org.nwolfhub.bandabot.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.nwolfhub.bandabot.database.repositories.QuestRepository;
import org.nwolfhub.bandabot.database.repositories.UsersRepository;
import org.nwolfhub.bandabot.telegram.requests.QueueExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramHandler {
    private final TelegramBot bot;

    private final QueueExecutor executor;
    private final List<Long> admins;
    private final List<Long> toIgnore = new ArrayList<>();
    private final QuestRepository questRepository;
    private final UsersRepository usersRepository;


    public TelegramHandler(TelegramBot bot, QueueExecutor executor, List<Long> admins, QuestRepository questRepository, UsersRepository usersRepository) {
        this.bot = bot;
        this.executor = executor;
        this.admins = admins;
        this.questRepository = questRepository;
        this.usersRepository = usersRepository;
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
                Long from = update.message().from().id();
                if(from.equals(chat)) {
                    if(command.equals("/start")) {
                        executor.executeRequest(new SendMessage(from, "Добро пожаловать в банду! Здесь ты можешь управлять своими долгами, а так же смотреть за их списком в нашем клане").replyMarkup(new ReplyKeyboardMarkup("Меню").resizeKeyboard(true)), () -> {}, new ArrayList<>());
                    }
                    if(command.equals("меню")) {
                        if(!toIgnore.contains(from)) {
                            executor.executeRequest(new SendMessage(from, "Ищем вас в базе данных"), () -> {}, new ArrayList<>());
                            WereUser related = usersRepository.getByTelegramId(from);
                            if(related==null) {
                                toIgnore.add(from);
                                executor.executeRequest(new SendMessage(from, "Ты не зарегестрировал аккаунт!"), () -> {}, new ArrayList<>());
                            }
                        }
                    }
                }
            }
        }
    }
}
