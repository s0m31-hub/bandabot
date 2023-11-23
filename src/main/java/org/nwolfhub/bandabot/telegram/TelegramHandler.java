package org.nwolfhub.bandabot.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.nwolfhub.bandabot.database.repositories.DebtRepository;
import org.nwolfhub.bandabot.database.repositories.QuestRepository;
import org.nwolfhub.bandabot.database.repositories.UsersRepository;
import org.nwolfhub.bandabot.telegram.requests.QueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TelegramHandler {
    private final TelegramBot bot;
    private final DebtRepository debtRepository;
    private final UsersRepository usersRepository;
    private final QuestRepository questRepository;
    private final QueueExecutor executor;
    private final List<Long> admins;



    public TelegramHandler(TelegramBot bot, DebtRepository debtRepository, UsersRepository usersRepository, QuestRepository questRepository, QueueExecutor executor, List<Long> admins) {
        this.bot = bot;
        this.debtRepository = debtRepository;
        this.usersRepository = usersRepository;
        this.questRepository = questRepository;
        this.executor = executor;
        bot.setUpdatesListener(new UpdatesListener() {
            @Override
            public int process(List<Update> list) {
                for(Update update:list) {
                    onUpdate(update);
                }
                return CONFIRMED_UPDATES_ALL;
            }
        });
        this.admins = admins;
    }

    private void onUpdate(Update update) {
        if(update.message()!=null) {
            if(update.message().text()!=null) {
                String text = update.message().text();
                String command = text.toLowerCase();
                if(command.equals("/start")) {
                    executor.executeRequest();
                }
            }
        }
    }
}
