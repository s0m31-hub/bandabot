package org.nwolfhub.bandabot.telegram.requests;

import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pengrad.telegrambot.response.SendResponse;

import java.util.List;

public class PendingRequest {
    public Runnable onResponse;
    public SendMessage request;
    public List<SendResponse> responses;

    public Runnable getOnResponse() {
        return onResponse;
    }

    public PendingRequest setOnResponse(Runnable onResponse) {
        this.onResponse = onResponse;
        return this;
    }

    public Object getRequest() {
        return request;
    }

    public PendingRequest setRequest(SendMessage request) {
        this.request = request;
        return this;
    }

    public List<SendResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<SendResponse> responses) {
        this.responses = responses;
    }

    public PendingRequest(Runnable onResponse, SendMessage request, List<SendResponse> responses) {
        this.onResponse = onResponse;
        this.request = request;
        this.responses=responses;
    }
}
