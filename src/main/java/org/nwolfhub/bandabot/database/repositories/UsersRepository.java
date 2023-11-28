package org.nwolfhub.bandabot.database.repositories;

import jakarta.transaction.Transactional;
import org.nwolfhub.bandabot.database.model.WereUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Transactional
public interface UsersRepository extends CrudRepository<WereUser, Integer> {
    WereUser getWereUserByWereId(String wereId);
    List<WereUser> getWereUsersByGoldDebtGreaterThanEqual(Integer goldDebt);
    List<WereUser> getAllByWereIdIn(List<String> wereids);
    WereUser getById(Integer id);
    WereUser getByTelegramId(Long telegramId);
}
