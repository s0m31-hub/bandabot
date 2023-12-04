package org.nwolfhub.bandabot.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.nwolfhub.bandabot.database.repositories.QuestRepository;
import org.nwolfhub.bandabot.database.repositories.UsersRepository;
import org.nwolfhub.bandabot.telegram.requests.QueueExecutor;
import org.nwolfhub.bandabot.wolvesville.ClanData;
import org.nwolfhub.utils.Configurator;
import org.nwolfhub.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TelegramHandler {
    private final TelegramBot bot;

    private final QueueExecutor executor;
    private final List<Long> admins;
    private final HashMap<String, Long> bindCodes;
    private final QuestRepository questRepository;
    private final UsersRepository usersRepository;
    private final ClanData data;

    private final List<WereUser> loadedUsers = new ArrayList<>();
    private final HashMap<Long, String> states = new HashMap<>();
    private final List<String> replies = List.of("Кто это тут плохой мальчик?", "Ой, это же ты!", "Кто это тут не платит налоги?",
            "Ты думал, что сможешь спрятаться?", "Уж кто кто, а ты то почему здесь...", "You did that! You!");


    public TelegramHandler(TelegramBot bot, QueueExecutor executor, Configurator configurator, HashMap<String, Long> bindCodes, QuestRepository questRepository, UsersRepository usersRepository, ClanData data) {
        this.bot = bot;
        this.executor = executor;
        this.admins = new ArrayList<>(Arrays.stream(configurator.getValue("admins").split(",")).map(Long::valueOf).toList());
        this.data = data;
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
                    else if (command.equals("меню") || command.equals("/menu")) {
                        menu(from);
                    } else if(command.equals("оплатить задолженность гемами")) {
                        WereUser updated = usersRepository.getByTelegramId(from);
                        if(updated!=null) {
                            states.put(from, "gemsExchange");
                            executor.executeRequest(new SendMessage(from, "Для обмена доступно " + updated.getFreeGems() + " кристаллов. Введите количество для перевода\n\nКурс: 1 гем - 10 золота"));
                        } else {
                            executor.executeRequest(new SendMessage(from, "Нет привязанных игровых аккаунтов").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Привязать аккаунт").callbackData("bindAccount"))));
                        }
                    } else if(command.equals("список должников")) {
                        List<WereUser> inDebt = usersRepository.getAllByGoldDebtGreaterThanEqualAndInClan(1, true);
                        if (inDebt.isEmpty()) {
                            executor.executeRequest(new SendMessage(from, "Вертрудо всех сильно попинал: должников нет!"));
                        } else {
                            StringBuilder badGuys = new StringBuilder();
                            inDebt.sort(Comparator.comparing(WereUser::getGoldDebt));
                            Collections.reverse(inDebt);
                            for (WereUser wereUser : inDebt) {
                                if (!badGuys.isEmpty()) {
                                    badGuys.append("\n");
                                }
                                badGuys.append(wereUser.getUsername()).append(": ").append(wereUser.getGoldDebt()).append(" золота");
                                if (wereUser.getTelegramId() != null && wereUser.getTelegramId().equals(from))
                                    badGuys.append(" (").append(replies.get(new Random().nextInt(replies.size()))).append(")");
                            }
                            executor.executeRequest(new SendMessage(from, badGuys.toString()));
                        }
                    } else if(command.equals("админ панель")) {
                        if(admins.contains(from)) {
                            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(new String[] {"Отключить участия"}, new String[]{"Управление участниками"}, new String[]{"debug prompt"}, new String[]{"Меню"}).resizeKeyboard(true);
                            String textToSend = "Добро пожаловать в панель администратора!\n\nПояснения функций:\n\nОтключить участия - автоматически убирает участие " +
                                    "в квестах у игроков с долгом больше указанного\n\n" +
                                    "Управление участниками - изменение долга/участия конкретного игрока\n\n" +
                                    "Debug prompt - cli-интерфейс с кучей возможностей. Не должен пригодиться за пределами разработки";
                            executor.executeRequest(new SendMessage(from, textToSend).replyMarkup(markup));
                        }
                    } else if(command.equals("отключить участия")) {
                        if(admins.contains(from)) {
                            states.put(from, "toggleOff");
                            executor.executeRequest(new SendMessage(from, "Укажи порог золота"));
                        }
                    } else if(command.equals("управление участниками")) {
                        if(admins.contains(from)) {
                            executor.executeRequest(new SendMessage(from, "Список участников (страница 1)").replyMarkup(buildAdminMembersList(0)));
                        }
                    }
                    else {
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
            } else if(text.contains("adminPanelMembersNext")) {
                executor.executeRequestNoQueue(new EditMessageReplyMarkup(update.callbackQuery().from().id(), update.callbackQuery().message().messageId()).replyMarkup(buildAdminMembersList(Integer.parseInt(text.split("adminPanelMembersNext")[1]) + 44)));

            } else if(text.contains("adminPanelMembersPrev")) {
                executor.executeRequestNoQueue(new EditMessageReplyMarkup(update.callbackQuery().from().id(), update.callbackQuery().message().messageId()).replyMarkup(buildAdminMembersList(Integer.parseInt(text.split("adminPanelMembersPrev")[1]) - 44)));
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
            String debt = related.getGoldDebt()>0?"Твоя задолженность составляет " + related.getGoldDebt() + " голды":"В твоём запасе есть " + related.getGoldDebt()*-1 + " золота";
            executor.executeRequest(new SendMessage(from, "Добро пожаловать, " + related.getUsername() + "\n" + debt + "\nДоступно самоцветов для конвертирования: " + related.getFreeGems()).replyMarkup(buildMenuKeyboard(admins.contains(from))));
        }
    }

    private ReplyKeyboardMarkup buildMenuKeyboard(Boolean admin) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(new KeyboardButton[] {new KeyboardButton("Оплатить задолженность гемами")}, new KeyboardButton[]{new KeyboardButton("Список должников")}).resizeKeyboard(true);
        if(admin) markup.addRow(new KeyboardButton("Админ панель"));
        return markup;
    }

    private InlineKeyboardMarkup buildAdminMembersList(Integer offset) {
        List<WereUser> inDebt = usersRepository.getAllByInClan(true);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton[] lastRow;
        int lastI=0;
        for(int i = offset; i<inDebt.size(); i+=2) {
            lastI=i;
            if(i>44+offset) {
                break;
            } else {
                if (i + 1 < inDebt.size()) {
                    lastRow = new InlineKeyboardButton[]{new InlineKeyboardButton(inDebt.get(i).getUsername()).callbackData("adminPanelMember" + inDebt.get(i).getWereId()),
                            new InlineKeyboardButton(inDebt.get(i + 1).getUsername()).callbackData("adminPanelMember" + inDebt.get(i + 1).getWereId())};
                } else {
                    lastRow = new InlineKeyboardButton[]{new InlineKeyboardButton(inDebt.get(i).getUsername()).callbackData("adminPanelMember" + inDebt.get(i).getWereId())};
                }
                markup.addRow(lastRow);
            }
        }
        if (offset > 0) {
            if(lastI<inDebt.size()) {
                markup.addRow(new InlineKeyboardButton("Назад").callbackData("adminPanelMembersPrev" + offset),
                        new InlineKeyboardButton("Меню").callbackData("menu"),
                        new InlineKeyboardButton("Дальше").callbackData("adminPanelMembersNext" + offset));
            } else {
                markup.addRow(new InlineKeyboardButton("Назад").callbackData("adminPanelMembersPrev" + offset),
                        new InlineKeyboardButton("Меню").callbackData("menu"));
            }
        } else {
            markup.addRow(new InlineKeyboardButton("Меню").callbackData("menu"),
                    new InlineKeyboardButton("Дальше").callbackData("adminPanelMembersNext" + offset));
        }
        return markup;
    }
}