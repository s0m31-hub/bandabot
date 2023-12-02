package org.nwolfhub.bandabot.database.model;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

@Entity
@Table(name="wereusers", schema = "clanbot")
@Transactional
public class WereUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "uid_inc")
    @SequenceGenerator(name = "uid_inc", sequenceName = "clanbot.userid_increaser", allocationSize=1)
    private Integer id;
    @Column(nullable = false)
    private String wereId;
    @Column(nullable = false)
    public String username;
    @Column(nullable = false)
    private Integer goldDebt = 0;
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer freeGems = 0;
    @Column(nullable = true)
    private Long telegramId;
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean inClan = false;
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public Integer getGoldDebt() {
        return goldDebt;
    }

    public WereUser setGoldDebt(Integer goldDebth) {
        this.goldDebt = goldDebth;
        return this;
    }

    public String getWereId() {
        return wereId;
    }

    public WereUser setWereId(String wereId) {
        this.wereId = wereId;
        return this;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public WereUser setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public WereUser setUsername(String username) {
        this.username = username;
        return this;
    }
    public WereUser addDebt(Integer debt) {
        this.goldDebt = goldDebt+debt;
        return this;
    }

    public Boolean getInClan() {
        return inClan;
    }

    public WereUser setInClan(Boolean inClan) {
        this.inClan = inClan;
        return this;
    }

    public WereUser addGems(Integer amount) {
        this.freeGems+=amount;
        return this;
    }

    public WereUser(Integer id, String wereId, Integer goldDebt, String username, Long telegramId, Integer freeGems, Boolean inClan) {
        this.id = id;
        this.wereId = wereId;
        this.goldDebt = goldDebt;
        this.telegramId = telegramId;
        this.username = username;
        this.freeGems = freeGems;
        this.inClan = inClan;
    }

    public WereUser() {}

    public Integer getFreeGems() {
        return freeGems;
    }

    public WereUser setFreeGems(Integer freeGems) {
        this.freeGems = freeGems;
        return this;
    }
}
