package com.mycompany.myapp;

import com.mycompany.myapp.domain.PersistentAuditEvent;
import com.mycompany.myapp.repository.PersistenceAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.scheduler.Schedulers;

@SpringBootTest
public class FailValidationIT {

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Test
    public void test() {
        PersistentAuditEvent event = new PersistentAuditEvent();
        persistenceAuditEventRepository.save(event).subscribeOn(Schedulers.parallel()).block();
    }

}
