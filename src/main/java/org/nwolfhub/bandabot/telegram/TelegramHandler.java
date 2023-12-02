package org.nwolfhub.bandabot.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.*;
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

    private final List<WereUser> loadedUsers = new ArrayList<>();
    private final HashMap<Long, String> states = new HashMap<>();


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
                try {
                    onUpdate(update);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                    else if (command.equals("меню")) {
                        menu(from);
                    } else if(command.equals("оплатить задолженность гемами")) {
                        WereUser updated = usersRepository.getByTelegramId(from);
                        if(updated!=null) {
                            states.put(from, "gemsExchange");
                            executor.executeRequest(new SendMessage(from, "Для обмена доступно " + updated.getFreeGems() + ". Введите количество для перевода\n\nКурс: 1 гем - 10 золота"));
                        } else {
                            executor.executeRequest(new SendMessage(from, "Нет привязанных игровых аккаунтов").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Привязать аккаунт").callbackData("bindAccount"))));
                        }
                    } else {
                        if(states.get(from)!=null && states.get(from).equals("gemsExchange")) {
                            try {
                                int amount = Integer.parseInt(command);
                                if(amount>0) {
                                    executor.executeRequest(new SendMessage(from, "Обмен " + amount + " кристаллов на " + amount*10 + " золота").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton[]{new InlineKeyboardButton("Подтвердить").callbackData("gemtrade" + amount)},
                                            new InlineKeyboardButton[]{new InlineKeyboardButton("Отмена").callbackData("menu")})));
                                }
                            } catch (NumberFormatException e) {
                                executor.executeRequest(new SendMessage(from, "Не похоже на число"));
                            }
                            states.put(from, "menu");
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
            } else if(text.equals("menu")) {
                menu(from);
            } else {
                if(text.contains("gemtrade")) {
                    WereUser related = usersRepository.getByTelegramId(from);
                    if(related!=null) {
                        Integer amount = Integer.valueOf(text.split("gemtrade")[1]);
                        if(related.getFreeGems()>=amount) {
                            related.addDebt(amount*-10);
                            related.addGems(amount*-1);
                            usersRepository.save(related);
                            executor.executeRequestNoQueue(new AnswerCallbackQuery(id).text("Успешный перевод"));
                        } else {
                            executor.executeRequestNoQueue(new AnswerCallbackQuery(id).text("Недостаточно кристаллов").showAlert(true));
                        }
                        menu(from);
                    }
                }
            }
        }
    }

    private void menu(Long from) {
        WereUser related = usersRepository.getByTelegramId(from);
        if (related == null) {
            executor.executeRequest(new SendMessage(from, "Нет привязанных игровых аккаунтов").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Привязать аккаунт").callbackData("bindAccount"))));
        } else {
            states.put(from, "menu");
            executor.executeRequest(new SendMessage(from, "Добро пожаловать, " + related.getUsername() + "\nТвоя задолженность составляет " + related.getGoldDebt() + " голды\nДоступно самоцветов для конвертирования: " + related.getFreeGems()).replyMarkup(buildMenuKeyboard(admins.contains(from))));
        }
    }

    private ReplyKeyboardMarkup buildMenuKeyboard(Boolean admin) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(new KeyboardButton[] {new KeyboardButton("Оплатить задолженность гемами")}, new KeyboardButton[]{new KeyboardButton("Список должников")}).resizeKeyboard(true);
        if(admin) markup.addRow(new KeyboardButton("Админ панель"));
        return markup;
    }
}