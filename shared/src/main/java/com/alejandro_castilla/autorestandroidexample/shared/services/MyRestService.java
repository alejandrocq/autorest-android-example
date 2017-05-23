package com.alejandro_castilla.autorestandroidexample.shared.services;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import rx.Observable;

@AutoRestGwt @Path("https://api.bf4stats.com/api/") // Example API URL (Battlefield 4 stats)
@Produces("application/json;charset=utf-8") @Consumes("application/json;charset=utf-8")
public interface MyRestService {
    @GET @Path("onlinePlayers")
    Observable<Data> onlinePlayers();

    /**
     * Response DTO of BF4 API.
     */
    class Data {
        public Platform pc;

        public class Platform {
            public String label;
            public int count;
            public int peak24;
        }
    }
}
