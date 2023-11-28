package org.nwolfhub.bandabot.database.model;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.List;

@Entity
@Table(name="werequests", schema = "clanbot")
@Transactional
public class WereQuest {
    @Id
    private String id;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "participants")
    private List<WereUser> participants;
    private String previewUrl;
    public WereQuest setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
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

    public WereQuest() {}

    public WereQuest(String id, List<WereUser> participants, String previewUrl) {
        this.id = id;
        this.participants = participants;
        this.previewUrl = previewUrl;
    }
}
