package org.nwolfhub.bandabot.wolvesville;

import org.springframework.stereotype.Component;

@Component
public class ClanData {
    public String weretoken;
    public String wereclan;

    public String getWeretoken() {
        return weretoken;
    }

    public ClanData setWeretoken(String weretoken) {
        this.weretoken = weretoken;
        return this;
    }

    public String getWereclan() {
        return wereclan;
    }

    public ClanData setWereclan(String wereclan) {
        this.wereclan = wereclan;
        return this;
    }
}
