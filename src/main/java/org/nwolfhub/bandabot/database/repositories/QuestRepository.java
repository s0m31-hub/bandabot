package org.nwolfhub.bandabot.database.repositories;

import org.nwolfhub.bandabot.database.model.WereQuest;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface QuestRepository extends CrudRepository<WereQuest, String> {
    List<WereQuest> getAllByParticipantsContaining(WereUser user);

    WereQuest getById(String id);
}