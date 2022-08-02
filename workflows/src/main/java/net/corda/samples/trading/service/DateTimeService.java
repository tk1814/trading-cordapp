package net.corda.samples.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@CordaService
public class DateTimeService extends SingletonSerializeAsToken {

    private final AppServiceHub serviceHub;
    final Request httpRequest = new Request.Builder().url("https://worldtimeapi.org/api/timezone/Etc/UTC").build();

    public DateTimeService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    public LocalDateTime getDateTime() {
        LocalDateTime settlementDate;
        try {
            Response httpResponse = new OkHttpClient().newCall(httpRequest).execute();

            if (httpResponse.code() == 200 && httpResponse.body() != null) {
                String value = httpResponse.body().string();
                JsonNode jsonNode = new ObjectMapper().readTree(value);
                settlementDate = LocalDateTime.parse(jsonNode.get("utc_datetime").toString().substring(1, 24));
            } else {
                settlementDate = LocalDateTime.now(ZoneOffset.UTC);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return settlementDate;
    }
}
