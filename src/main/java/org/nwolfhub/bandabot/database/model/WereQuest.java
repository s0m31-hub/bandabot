package org.nwolfhub.bandabot.database.model;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Entity
@Table(name="werequests", schema = "clanbot")
@Transactional
public class WereQuest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "quest_inc")
    @SequenceGenerator(name = "quest_inc", sequenceName = "clanbot.questid_increaser", allocationSize=1)
    private Integer innerId;

    private String wereId;
    @NotNull
    @JoinTable(name = "clanbot.map_that_leads_to_you", joinColumns = @JoinColumn(name = "innerid"), inverseJoinColumns = @JoinColumn(name = "id"))
    @ManyToMany(fetch = FetchType.LAZY)
    private List<WereUser> participants;
    private String previewUrl;
    public WereQuest setWereId(String id) {
        this.wereId = id;
        return this;
    }

    public String getWereId() {
        return wereId;
    }

    public List<WereUser> getParticipants() {
        return participants;
    }

    public WereQuest setParticipants(List<WereUser> participants) {
        this.participants = participants;
        return this;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public WereQuest setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
        return this;
    }

    public Integer getInnerId() {
        return innerId;
    }

    public WereQuest setInnerId(Integer innerId) {
        this.innerId = innerId;
        return this;
    }

    public WereQuest() {}

    public WereQuest(Integer innerid, String wereid, List<WereUser> participants, String previewUrl) {
        this.innerId = innerid;
        this.wereId = wereid;
        this.participants = participants;
        this.previewUrl = previewUrl;
    }
}
