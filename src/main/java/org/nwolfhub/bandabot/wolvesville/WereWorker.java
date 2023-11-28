package org.nwolfhub.bandabot.wolvesville;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pengrad.telegrambot.request.SendPhoto;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
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

    @Autowired
    private QueueExecutor executor;

    public WereWorker(UsersRepository usersRepository, QuestRepository questRepository, ClanData data) {
        this.usersRepository = usersRepository;
        this.questRepository = questRepository;
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
                updateCurrentQuest();
                Thread.sleep(30000);
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
            WereQuest inDB = questRepository.getById(quest.get("id").getAsString());
            if(inDB==null) {
                executor.executeRequestNoQueue(new SendPhoto(executor.mainChat, quest.get("promoImageUrl").getAsString()).caption("Мы начали новый квест!\n\nПрошу всех участников внести 600 золота в казну. Так же напоминаю про норму в 4500 опыта"));
                HashMap<WereUser, Integer> participants = getQuestParticipants(questElement);
                inDB = new WereQuest().setId(quest.get("id").getAsString()).setParticipants(participants.keySet().stream().toList()).setPreviewUrl(quest.get("promoImageUrl").getAsString());
                participants.keySet().forEach(e -> e.addDebt(600));
                usersRepository.saveAll(participants.keySet());
                questRepository.save(inDB);
                System.out.println("Updated quest " + inDB.getId());
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
                try {
                     inDB = questRepository.getById(quest.get("id").getAsString());
                     new PrintWriter(new PrintStream(OutputStream.nullOutputStream())).println(inDB); //does nothing? Ha-ha, useful as fuck!
                } catch (EntityNotFoundException e) {
                    inDB = null;
                }
                if(inDB==null) {
                    HashMap<WereUser, Integer> participants = getQuestParticipants(questElement);
                    inDB = new WereQuest().setId(quest.get("id").getAsString()).setParticipants(participants.keySet().stream().toList()).setPreviewUrl(quest.get("promoImageUrl").getAsString());
                    participants.keySet().forEach(e -> e.addDebt(600));
                    for(WereUser participant:participants.keySet()) {
                        int debtMultiplier = participants.get(participant)/1000;
                        participant.addDebt(debtMultiplier*100);
                    }
                    usersRepository.saveAll(participants.keySet());
                    questRepository.save(inDB);
                    System.out.println("Updated quest " + inDB.getId());
                }
            }
        }
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

    private String getToken() {
        return "Bot " + data.getWeretoken();
    }
}
