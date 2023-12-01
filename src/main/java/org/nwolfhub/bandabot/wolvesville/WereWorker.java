package org.nwolfhub.bandabot.wolvesville;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.nwolfhub.bandabot.database.model.WereQuest;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.nwolfhub.bandabot.database.repositories.QuestRepository;
import org.nwolfhub.bandabot.database.repositories.UsersRepository;
import org.nwolfhub.bandabot.telegram.requests.QueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
public class WereWorker {
    private final UsersRepository usersRepository;
    private final QuestRepository questRepository;
    private volatile OkHttpClient client;
    private static final String baseUrl = "https://api.wolvesville.com";
    private ClanData data;
    private final HashMap<String, Long> bindCodes;

    @Autowired
    private QueueExecutor executor;


    public WereWorker(UsersRepository usersRepository, QuestRepository questRepository, ClanData data, HashMap<String, Long> bindCodes) {
        this.usersRepository = usersRepository;
        this.questRepository = questRepository;
        this.bindCodes = bindCodes;
        this.client = new OkHttpClient();
        this.data = data;
    }

    @PreDestroy
    private void destroy() {
        System.out.println("Beginning to close WereWorker process");
        client = null;
    }

    public void launch() throws IOException {
        updateHistory();
        new Thread(this::workerThread).start();
    }

    private void workerThread() {
        while (client != null) {
            Thread.onSpinWait();
            try {
                getChatMessages();
                updateCurrentQuest();
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("WereWorker finished");
    }

    private void updateCurrentQuest() throws IOException {
        Response allTheQuestsSheSaid = client.newCall(new Request.Builder().url(baseUrl + "/clans/" + data.getWereclan() + "/quests/active")
                .addHeader("Authorization", getToken()).build()).execute();
        if(!allTheQuestsSheSaid.isSuccessful()) {
            allTheQuestsSheSaid.close();
            System.out.println("Failed to get current quest info");
        } else {
            String response = allTheQuestsSheSaid.body().string();
            allTheQuestsSheSaid.close();
            JsonElement questElement = JsonParser.parseString(response);
            JsonObject questObject = questElement.getAsJsonObject();
            JsonObject quest = questObject.get("quest").getAsJsonObject();
            WereQuest inDB = questRepository.getByWereId(quest.get("id").getAsString());
            if(inDB==null) {
                executor.executeRequestNoQueue(new SendPhoto(executor.mainChat, quest.get("promoImageUrl").getAsString()).caption("Мы начали новый квест!\n\nПрошу всех участников внести 600 золота в казну. Так же напоминаю про норму в 4500 опыта"));
                HashMap<WereUser, Integer> participants = getQuestParticipants(questElement);
                WereQuest newQuest = new WereQuest().setWereId(quest.get("id").getAsString()).setParticipants(participants.keySet().stream().toList()).setPreviewUrl(quest.get("promoImageUrl").getAsString());
                participants.keySet().forEach(e -> e.addDebt(600));
                usersRepository.saveAll(participants.keySet());
                questRepository.save(newQuest);
                System.out.println("Updated quest " + newQuest.getWereId());
            }
        }
    }
    private void updateHistory() throws IOException {
        System.out.println("Resolving quests");
        Response allTheQuestsSheSaid = client.newCall(new Request.Builder().url(baseUrl + "/clans/" + data.getWereclan() + "/quests/history")
                .addHeader("Authorization", getToken()).build()).execute();
        if(!allTheQuestsSheSaid.isSuccessful()) {
            allTheQuestsSheSaid.close();
            throw new IOException("Request to wolvesville has failed");
        } else {
            String response = allTheQuestsSheSaid.body().string();
            allTheQuestsSheSaid.close();
            JsonArray quests = JsonParser.parseString(response).getAsJsonArray();
            for(JsonElement questElement:quests) {
                JsonObject questObject = questElement.getAsJsonObject();
                JsonObject quest = questObject.get("quest").getAsJsonObject();
                WereQuest inDB;
                inDB = questRepository.getByWereId(quest.get("id").getAsString());
                if(inDB==null) {
                    HashMap<WereUser, Integer> participants = getQuestParticipants(questElement);
                    WereQuest newQuest = new WereQuest().setWereId(quest.get("id").getAsString()).setParticipants(participants.keySet().stream().toList()).setPreviewUrl(quest.get("promoImageUrl").getAsString());
                    participants.keySet().forEach(e -> e.addDebt(600));
                    for(WereUser participant:participants.keySet()) {
                        int debtMultiplier = participants.get(participant)/1000;
                        participant.addDebt(debtMultiplier*100);
                    }
                    usersRepository.saveAll(participants.keySet());
                    questRepository.save(newQuest);
                    System.out.println("Updated quest " + newQuest.getWereId());
                }
            }
        }
        System.out.println("Resolved quests");
    }

    private HashMap<WereUser, Integer> getQuestParticipants(JsonElement quest) {
        JsonArray participants = quest.getAsJsonObject().get("participants").getAsJsonArray();
        HashMap<String, Integer> listOfIds = new HashMap<>();
        HashMap<String, String> idToUsername = new HashMap<>();
        for(JsonElement participantElement:participants) {
            listOfIds.put(participantElement.getAsJsonObject().get("playerId").getAsString(), participantElement.getAsJsonObject().get("xp").getAsInt());
            idToUsername.put(participantElement.getAsJsonObject().get("playerId").getAsString(), participantElement.getAsJsonObject().get("username").getAsString());
        }
        List<WereUser> users = usersRepository.getAllByWereIdIn(listOfIds.keySet().stream().toList());
        boolean hadUpdates = false;
        for(String id:listOfIds.keySet()) {
            if(users.stream().noneMatch(user -> user.getWereId().equals(id))) {
                WereUser user = new WereUser().setWereId(id).setUsername(idToUsername.get(id));
                users.add(user);
                hadUpdates = true;
            }
        }
        if(hadUpdates) {
            usersRepository.saveAll(users);
            System.out.println("Saving newly registered users");
            users = usersRepository.getAllByWereIdIn(listOfIds.keySet().stream().toList());
        }
        HashMap<WereUser, Integer> finalMap = new HashMap<>();
        for(WereUser user:users) {
            finalMap.put(user, listOfIds.get(user.getWereId()));
        }
        return finalMap;
    }

    private void getChatMessages() throws IOException {
        Response response = client.newCall(new Request.Builder().url(baseUrl + "/clans/" + data.getWereclan() + "/chat").get().addHeader("Authorization", getToken()).build()).execute();
        if(response.isSuccessful()) {
            String body = response.body().string();
            response.close();
            JsonArray messages = JsonParser.parseString(body).getAsJsonArray();
            for(int i = messages.size()-1; i>=0; i--) {
                JsonElement messageElement = messages.get(i);
                JsonObject message = messageElement.getAsJsonObject();
                if(message.has("playerId") && message.has("msg") && !message.get("isSystem").getAsBoolean()) {
                    String from = message.get("playerId").getAsString();
                    String text = message.get("msg").getAsString();
                    if(text.length()==4) {
                        String code = text.toUpperCase();
                        if(bindCodes.containsKey(code)) {
                            Long author = bindCodes.get(code);
                            WereUser wereUser = usersRepository.getByWereId(from);
                            if(wereUser==null) {
                                executor.executeRequest(new SendMessage(from, "Ты ещё не участвовал в квестах! Регистрация займёт чуть больше времени :)"));
                                wereUser = new WereUser().setWereId(from).setGoldDebt(0);
                                Response userGetResponse = client.newCall(new Request.Builder().url(baseUrl + "/players/" + from).addHeader("Authorization", getToken()).build()).execute();
                                if(userGetResponse.isSuccessful()) {
                                    String body1 = userGetResponse.body().string();
                                    userGetResponse.close();
                                    String username = JsonParser.parseString(body1).getAsJsonObject().get("username").getAsString();
                                    wereUser.setUsername(username);
                                } else {
                                    executor.executeRequest(new SendMessage(from, "Не удалось получить информацию о твоём профиле :(\n\nНе отправляй новый код, я попробую ещё раз через время!"));
                                    continue;
                                }
                            }
                            bindCodes.remove(code);
                            wereUser.setTelegramId(author);
                            usersRepository.save(wereUser);
                            executor.executeRequest(new SendMessage(author, "Добро пожаловать, " + wereUser.getUsername() + "!"));
                        }
                    }
                }
            }
        }
    }

    private String getToken() {
        return "Bot " + data.getWeretoken();
    }
}
