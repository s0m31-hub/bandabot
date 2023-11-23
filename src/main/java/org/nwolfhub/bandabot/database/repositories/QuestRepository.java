package org.nwolfhub.bandabot.database.repositories;

import org.nwolfhub.bandabot.database.model.WereQuest;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface QuestRepository extends CrudRepository<WereQuest, String> {
    WereQuest getAllByParticipantsContaining(WereUser user);
}
