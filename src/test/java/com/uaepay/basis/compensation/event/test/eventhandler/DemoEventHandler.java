package com.uaepay.basis.compensation.event.test.eventhandler;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.uaepay.basis.compensation.event.domain.CompensationEvent;
import com.uaepay.basis.compensation.event.domain.ExecuteResult;
import com.uaepay.basis.compensation.event.service.CompensationEventHandler;

@Service
public class DemoEventHandler implements CompensationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoEventHandler.class);
    private static final String MAIN_TYPE = "demoMainType";

    public static final CompensationEvent.Builder buildEvent() {
        String eventKey = new DateTime().toString("yyyyMMddHHmmssSSS");
        return CompensationEvent.newBuilder(eventKey, MAIN_TYPE, "-");
    }

    @Override
    public String getServiceCode() {
        return MAIN_TYPE;
    }

    @Override
    public ExecuteResult handleEvent(CompensationEvent compensationEvent) {
        LOGGER.info("处理demo事件: {}", compensationEvent);
        return ExecuteResult.success();
    }

}
