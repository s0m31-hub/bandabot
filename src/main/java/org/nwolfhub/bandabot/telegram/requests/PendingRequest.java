package org.nwolfhub.bandabot.telegram.requests;

import com.pengrad.telegrambot.response.BaseResponse;

import java.util.List;

public class PendingRequest {
    public Runnable onResponse;
    public Object request;
    public List<BaseResponse> responses;

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

    public PendingRequest setRequest(Object request) {
        this.request = request;
        return this;
    }

    public List<BaseResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<BaseResponse> responses) {
        this.responses = responses;
    }

    public PendingRequest(Runnable onResponse, Object request, List<BaseResponse> responses) {
        this.onResponse = onResponse;
        this.request = request;
        this.responses=responses;
    }
}
