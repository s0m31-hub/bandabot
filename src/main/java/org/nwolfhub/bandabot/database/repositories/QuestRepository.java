package org.nwolfhub.bandabot.database.repositories;

import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.nwolfhub.bandabot.database.model.WereQuest;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Transactional
public interface QuestRepository extends CrudRepository<WereQuest, Integer> {
    List<WereQuest> getAllByParticipantsContaining(WereUser user);
    WereQuest getByInnerId(Integer id);
    WereQuest getByWereId(String wereId);

}
