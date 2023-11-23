package org.nwolfhub.bandabot.database.model;

import jakarta.persistence.*;

@Entity
@Table(name="wereusers", schema = "clanbot")
public class WereUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "uid_inc")
    @SequenceGenerator(name = "uid_inc", sequenceName = "clanbot.userid_increaser", allocationSize=1)
    private Integer id;
    private String wereId;
    private Integer goldDebt;

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

    public WereUser(Integer id, String wereId, Integer goldDebth) {
        this.id = id;
        this.wereId = wereId;
        this.goldDebt = goldDebth;
    }

    public WereUser() {}
}
