package com.uaepay.basis.compensation.event.test;

import com.uaepay.basis.compensation.event.test.eventhandler.Test2EventHandler;
import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.uaepay.basis.compensation.event.domain.ExecuteResult;
import com.uaepay.basis.compensation.event.domain.convertor.impl.CompensationEventConverter;
import com.uaepay.basis.compensation.event.domain.enums.ExecuteStatus;
import com.uaepay.basis.compensation.event.test.eventhandler.TestEventHandler;
import com.uaepay.basis.compensation.event.test.repository.TestRepository;

@ExtendWith(SpringExtension.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplicationTestBase {

    @Autowired
    protected TestRepository test;

    @Autowired
    protected TestEventHandler testEventHandler;

    @Autowired
    protected Test2EventHandler test2EventHandler;

    @Autowired
    protected CompensationEventConverter compensationEventConverter;

    protected void assertExecuteResult(ExecuteResult result, ExecuteStatus expectExecuteStatus,
        String expectErrorMessage) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectExecuteStatus, result.getStatus());
        Assertions.assertEquals(expectErrorMessage, result.getErrorMessage());
    }

}
