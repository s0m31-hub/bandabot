package org.nwolfhub.bandabot.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.nwolfhub.bandabot.database.repositories.QuestRepository;
import org.nwolfhub.bandabot.database.repositories.UsersRepository;
import org.nwolfhub.bandabot.telegram.requests.QueueExecutor;
import org.nwolfhub.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class TelegramHandler {
    private final TelegramBot bot;

    private final QueueExecutor executor;
    private final List<Long> admins;
    private final HashMap<String, Long> bindCodes;
    private final QuestRepository questRepository;
    private final UsersRepository usersRepository;


    public TelegramHandler(TelegramBot bot, QueueExecutor executor, List<Long> admins, HashMap<String, Long> bindCodes, QuestRepository questRepository, UsersRepository usersRepository) {
        this.bot = bot;
        this.executor = executor;
        this.admins = admins;
        this.bindCodes = bindCodes;
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
        if (update.message() != null) {
            if (update.message().text() != null) {
                String text = update.message().text();
                String command = text.toLowerCase();
                Long chat = update.message().chat().id();
                Long from = update.message().from().id();
                if (from.equals(chat)) {
                    if (command.equals("/start")) {
                        executor.executeRequest(new SendMessage(from, "Добро пожаловать в банду! Здесь ты можешь управлять своими долгами, а так же смотреть за их списком в нашем клане").replyMarkup(new ReplyKeyboardMarkup("Меню").resizeKeyboard(true)));
                    }
                    if (command.equals("меню")) {
                        WereUser related = usersRepository.getByTelegramId(from);
                        if (related == null) {
                            executor.executeRequest(new SendMessage(from, "Ты не зарегистрировал аккаунт!").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Привязать аккаунт").callbackData("bindAccount"))));
                        } else {
                            executor.executeRequest(new SendMessage(from, "Добро пожаловать, " + related.getUsername() + "\nТвоя задолженность составляет " + related.getGoldDebt() + " голды\nДоступно самоцветов для конвертирования: " + related.getFreeGems()));
                        }
                    }
                }
            }
        } else if (update.callbackQuery() != null) {
            String id = update.callbackQuery().id();
            String text = update.callbackQuery().data();
            String command = text.toLowerCase();
            Long from = update.callbackQuery().from().id();
            if (text.equals("bindAccount")) {
                WereUser related = usersRepository.getByTelegramId(from);
                if (related == null) {
                    String code = bindCodes.keySet().stream().filter(e -> bindCodes.get(e).equals(from)).findFirst().orElse(Utils.generateString(4)).toUpperCase();
                    bindCodes.put(code, from);
                    executor.executeRequest(new SendMessage(from, "Для продолжения напиши в чат клана \\(в игре\\) твой код: `" + code + "`\n\nP\\.s\\. Привязка занимает до 3 минут, не нужно флудить как в тот чат, так и сюда").parseMode(ParseMode.MarkdownV2));
                    executor.executeRequestNoQueue(new AnswerCallbackQuery(id).text("Код: " + code));
                }
            }
        }
    }
}