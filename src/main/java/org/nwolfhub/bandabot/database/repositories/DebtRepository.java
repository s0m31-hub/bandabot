package org.nwolfhub.bandabot.database.repositories;

import org.nwolfhub.bandabot.database.model.DebtChange;
import org.nwolfhub.bandabot.database.model.WereQuest;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface DebtRepository extends CrudRepository<DebtChange, String> {
    List<DebtChange> getDebtChangesByUserOrQuest(WereUser user, WereQuest quest);
}
