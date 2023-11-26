package org.nwolfhub.bandabot.database.model;

import jakarta.persistence.*;

@Entity
@Table(name="debtchanges", schema = "clanbot")
public class DebtChange {
    @Id
    private String id;
    @OneToOne
    private WereUser user;
    private Integer gold;
    private Integer gems;
    @OneToOne
    private WereQuest quest;
    @Enumerated(EnumType.ORDINAL)
    private ChangeType type;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public WereUser getUser() {
        return user;
    }

    public DebtChange setUser(WereUser user) {
        this.user = user;
        return this;
    }

    public Integer getGold() {
        return gold;
    }

    public DebtChange setGold(Integer gold) {
        this.gold = gold;
        return this;
    }

    public Integer getGems() {
        return gems;
    }

    public DebtChange setGems(Integer gems) {
        this.gems = gems;
        return this;
    }

    public WereQuest getQuest() {
        return quest;
    }

    public DebtChange setQuest(WereQuest quest) {
        this.quest = quest;
        return this;
    }

    public ChangeType getType() {
        return type;
    }

    public DebtChange setType(ChangeType type) {
        this.type = type;
        return this;
    }

    public DebtChange() {}

    public DebtChange(String id, WereUser user, Integer gold, Integer gems, WereQuest quest, ChangeType type) {
        this.id = id;
        this.user = user;
        this.gold = gold;
        this.gems = gems;
        this.quest = quest;
        this.type = type;
    }

    public static enum ChangeType {
        contribution,
        participation
    }
}
