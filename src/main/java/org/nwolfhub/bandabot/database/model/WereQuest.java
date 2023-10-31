package org.nwolfhub.bandabot.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class WereQuest {
    @Id
    private String id;
    @OneToMany
    @JoinColumn(name = "participants")
    private List<WereUser> participants;
    private String previewUrl;

    public void setId(String id) {
        this.id = id;
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
