package org.nwolfhub.bandabot.database.repositories;

import org.nwolfhub.bandabot.database.model.WereUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface UsersRepository extends CrudRepository<WereUser, Integer> {
    WereUser getWereUserByWereId(String wereId);
    List<WereUser> getWereUsersByGoldDebtGreaterThanEqual(Integer goldDebt);

}
