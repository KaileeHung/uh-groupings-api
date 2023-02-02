package edu.hawaii.its.api.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.Person;

@PropertySource(value = "classpath:application-integrationTest.properties")
@Service("uhIdentiferGenerator")
public class UhIdentifierGenerator {

    @Value("${groupings.api.test.grouping_many}")
    private String GROUPING;

    @Value("${groupings.api.test.admin_user}")
    private String ADMIN;

    @Value("${groupings.api.test.usernames}")
    private List<String> TEST_USERNAMES;

    @Value("${groupings.api.test.uhuuids}")
    private List<String> TEST_UH_NUMBERS;

    @Autowired
    GroupingAssignmentService groupingAssignmentService;

    public static final Log logger = LogFactory.getLog(UhIdentifierGenerator.class);

    public Person getRandomUhIdentifer() {
        // random page number
        int rand = getRandomNumberBetween(1,5);
        boolean foundUser = false;
        Person user = new Person();
        String uhUuid = "";
        String uid = "";
        while (!foundUser) {
            Grouping gr = groupingAssignmentService.getPaginatedGrouping(GROUPING, ADMIN, rand, 20, null, true);

            // random user within the page
            rand = getRandomNumberBetween(0,gr.getBasis().getMembers().size() - 1);

            if (gr.getBasis().getMembers().size() != 0) {
                user = gr.getBasis().getMembers().get(rand);
                uhUuid = gr.getBasis().getMembers().get(rand).getUhUuid();
                uid = gr.getBasis().getMembers().get(rand).getUsername();
                foundUser = true;

            }
            rand = getRandomNumberBetween(1,5);
            if (uid.equals("") || uid == null || uhUuid.equals("") || uhUuid == null) {
                foundUser = false;
            }
        }
        return user;
    }

    public Person getRandomPerson() {
        if (ADMIN.equals("test_user")) {
            int rand = getRandomNumberBetween(0,4);
            return new Person("bobbo", TEST_UH_NUMBERS.get(rand), TEST_USERNAMES.get(rand));
        }
        return getRandomUhIdentifer();
    }

    private static int getRandomNumberBetween(int start, int end) {
        return start + (int) Math.round(Math.random() * (end - start));
    }

}
