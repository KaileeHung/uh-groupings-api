package edu.hawaii.its.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import edu.hawaii.its.api.configuration.SpringBootWebApplication;
import edu.hawaii.its.api.type.Person;

@ActiveProfiles("integrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class TestUhIdentifierGenerator {

    @Autowired
    UhIdentifierGenerator uhIdentifierGenerator;

    @Test
    public void getRandomUhIdentiferTest() {
        ArrayList<Person> arr = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            try {
                Person person = uhIdentifierGenerator.getRandomUhIdentifer();
                System.out.println(person);
                arr.add(person);
                if (person.getUsername() == null || person.getUsername().equals("")) {
                    fail("Should not have empty usernames");
                }
                if (person.getUhUuid() == null || person.getUhUuid().equals("")) {
                    fail("Should not have empty uhuuid");
                }
            } catch (Exception e) {
                System.out.println(e);
                fail("Should not have thrown an exception");
            }
        }
    }

}
