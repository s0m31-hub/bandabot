package org.nwolfhub.bandabot.wolvesville;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.nwolfhub.bandabot.database.model.WereQuest;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.nwolfhub.bandabot.database.repositories.QuestRepository;
import org.nwolfhub.bandabot.database.repositories.UsersRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class WereWorker {
    private final UsersRepository usersRepository;
    private final QuestRepository questRepository;
    private volatile OkHttpClient client;
    private static final String baseUrl = "https://api.wolvesville.com";
    private ClanData data;

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

    private void workerThread() {
        while (client != null) {
            Thread.onSpinWait();
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("WereWorker finished");
    }

    private void updateHistory() throws IOException {
        System.out.println("Resolving quests");
        Response allTheQuestsSheSaid = client.newCall(new Request.Builder().url(baseUrl + "/clans/" + data.getWereclan() + "/quests/history")
                .addHeader("Authorization", getToken()).build()).execute();
        if(!allTheQuestsSheSaid.isSuccessful()) {
            allTheQuestsSheSaid.close();
            throw new IOException("Request to wolvesville has failed");
        } else {
            String response = allTheQuestsSheSaid.message();
            allTheQuestsSheSaid.close();
            JsonArray quests = JsonParser.parseString(response).getAsJsonArray();
            for(JsonElement questElement:quests) {
                JsonObject questObject = questElement.getAsJsonObject();
                JsonObject quest = questObject.get("quest").getAsJsonObject();
                WereQuest inDB = questRepository.getById(quest.get("id").getAsString());
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
        return "Bot" + data.getWeretoken();
    }
}