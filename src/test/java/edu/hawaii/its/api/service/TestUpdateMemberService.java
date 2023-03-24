package edu.hawaii.its.api.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import edu.hawaii.its.api.configuration.SpringBootWebApplication;
import edu.hawaii.its.api.exception.AccessDeniedException;
import edu.hawaii.its.api.exception.UhMemberNotFoundException;
import edu.hawaii.its.api.groupings.GroupingsReplaceGroupMembersResult;
import edu.hawaii.its.api.type.Person;
import edu.hawaii.its.api.wrapper.AddMemberCommand;
import edu.hawaii.its.api.wrapper.RemoveMemberCommand;
import edu.hawaii.its.api.wrapper.RemoveMembersCommand;

import edu.internet2.middleware.grouperClient.ws.GcWebServiceError;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ActiveProfiles("integrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class TestUpdateMemberService {

    @Value("${groupings.api.test.grouping_many}")
    private String GROUPING;

    @Value("${groupings.api.test.grouping_many_basis}")
    private String GROUPING_BASIS;

    @Value("${groupings.api.test.grouping_many_include}")
    private String GROUPING_INCLUDE;

    @Value("${groupings.api.test.grouping_many_exclude}")
    private String GROUPING_EXCLUDE;

    @Value("${groupings.api.test.grouping_many_owners}")
    private String GROUPING_OWNERS;

    @Value("${groupings.api.grouping_admins}")
    private String GROUPING_ADMINS;

    @Value("${groupings.api.test.admin_user}")
    private String ADMIN;

    @Value("${groupings.api.success}")
    private String SUCCESS;

    @Value("${groupings.api.failure}")
    private String FAILURE;

    @Value("${groupings.api.test.uh-numbers}")
    private List<String> TEST_UH_NUMBERS;

    @Value("${groupings.api.test.uh-usernames}")
    private List<String> TEST_UH_USERNAMES;

    @Autowired
    private UpdateMemberService updateMemberService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private UhIdentifierGenerator uhIdentifierGenerator;

    private final String GROUP_NOT_FOUND = "GROUP_NOT_FOUND";
    private final String SUCCESS_ALREADY_EXISTED = "SUCCESS_ALREADY_EXISTED";
    private final String SUCCESS_WASNT_IMMEDIATE = "SUCCESS_WASNT_IMMEDIATE";

    private Person testPerson;

    @BeforeEach
    public void init() {
        new RemoveMemberCommand(GROUPING_ADMINS, TEST_UH_NUMBERS.get(0)).execute();
        new RemoveMembersCommand(GROUPING_INCLUDE, TEST_UH_NUMBERS).execute();
        new RemoveMembersCommand(GROUPING_EXCLUDE, TEST_UH_NUMBERS).execute();

        new RemoveMemberCommand(GROUPING_ADMINS, TEST_UH_USERNAMES.get(0)).execute();
        new RemoveMembersCommand(GROUPING_INCLUDE, TEST_UH_USERNAMES).execute();
        new RemoveMembersCommand(GROUPING_EXCLUDE, TEST_UH_USERNAMES).execute();

        testPerson = uhIdentifierGenerator.getRandomPerson();

        new RemoveMemberCommand(GROUPING_ADMINS, testPerson.getUhUuid()).execute();
        new RemoveMemberCommand(GROUPING_INCLUDE, testPerson.getUhUuid()).execute();
        new RemoveMemberCommand(GROUPING_EXCLUDE, testPerson.getUhUuid()).execute();
        new RemoveMemberCommand(GROUPING_OWNERS, testPerson.getUhUuid()).execute();

        new RemoveMemberCommand(GROUPING_ADMINS, testPerson.getUsername()).execute();
        new RemoveMemberCommand(GROUPING_INCLUDE, testPerson.getUsername()).execute();
        new RemoveMemberCommand(GROUPING_EXCLUDE, testPerson.getUsername()).execute();
        new RemoveMemberCommand(GROUPING_OWNERS, testPerson.getUsername()).execute();
    }

    @Test
    public void addRemoveAdminTest() {
        String testUhUuid = testPerson.getUhUuid();

        // With uh number.
        assertFalse(memberService.isAdmin(testUhUuid));
        updateMemberService.addAdmin(ADMIN, testUhUuid);
        assertTrue(memberService.isAdmin(testUhUuid));
        updateMemberService.removeAdmin(ADMIN, testUhUuid);
        assertFalse(memberService.isAdmin(testUhUuid));

        String testUid = testPerson.getUsername();

        // With uh username.
        assertFalse(memberService.isAdmin(testUid));
        updateMemberService.addAdmin(ADMIN, testUid);
        assertTrue(memberService.isAdmin(testUid));
        updateMemberService.removeAdmin(ADMIN, testUid);
        assertFalse(memberService.isAdmin(testUid));

        try {
            updateMemberService.addAdmin(ADMIN, "bogus-admin-to-add");
            fail("Should throw an exception if an invalid adminToAdd is passed.");
        } catch (UhMemberNotFoundException e) {
            assertNull(e.getCause());
        }

        try {
            updateMemberService.removeAdmin(ADMIN, "bogus-admin-to-remove");
            fail("Should throw an exception if an invalid adminToRemove is passed.");
        } catch (UhMemberNotFoundException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void checkIfAdminUserTest() {
        try {
            updateMemberService.checkIfAdminUser(TEST_UH_NUMBERS.get(0));
            fail("Should throw an exception if identifier is not an admin.");
        } catch (AccessDeniedException e) {
            assertNull(e.getCause());
        }
        try {
            updateMemberService.checkIfAdminUser(TEST_UH_USERNAMES.get(0));
            fail("Should throw an exception if identifier is not an admin.");
        } catch (AccessDeniedException e) {
            assertNull(e.getCause());
        }

        try {
            updateMemberService.checkIfAdminUser(ADMIN);
        } catch (AccessDeniedException e) {
            fail("Should not throw exception if current user is admin.");
        }
    }

    @Test
    public void uidAddRemoveIncludeExcludeMembersTest() {
        for (String uid : TEST_UH_USERNAMES) {
            assertFalse(memberService.isMember(GROUPING_INCLUDE, uid));
            assertFalse(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.addIncludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertTrue(memberService.isMember(GROUPING_INCLUDE, uid));
            assertFalse(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.removeIncludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertFalse(memberService.isMember(GROUPING_INCLUDE, uid));
            assertFalse(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.addExcludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertFalse(memberService.isMember(GROUPING_INCLUDE, uid));
            assertTrue(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.removeExcludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertFalse(memberService.isMember(GROUPING_INCLUDE, uid));
            assertFalse(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.addIncludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        updateMemberService.addExcludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertFalse(memberService.isMember(GROUPING_INCLUDE, uid));
            assertTrue(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.addIncludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertTrue(memberService.isMember(GROUPING_INCLUDE, uid));
            assertFalse(memberService.isMember(GROUPING_EXCLUDE, uid));
        }

        updateMemberService.removeIncludeMembers(ADMIN, GROUPING, TEST_UH_USERNAMES);
    }

    @Test
    public void uidAddRemoveIncludeExcludeMemberTest() {
        String testUhUuid = testPerson.getUhUuid();
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.addIncludeMember(ADMIN, GROUPING, testUhUuid);
        assertTrue(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.removeIncludeMember(ADMIN, GROUPING, testUhUuid);
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.addExcludeMember(ADMIN, GROUPING, testUhUuid);
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertTrue(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.removeExcludeMember(ADMIN, GROUPING, testUhUuid);
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.addIncludeMember(ADMIN, GROUPING, testUhUuid);
        updateMemberService.addExcludeMember(ADMIN, GROUPING, testUhUuid);
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertTrue(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.addIncludeMember(ADMIN, GROUPING, testUhUuid);
        assertTrue(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.removeIncludeMember(ADMIN, GROUPING, testUhUuid);
    }

    @Test
    public void optTest() {
        String testUhUuid = testPerson.getUhUuid();
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));

        updateMemberService.optIn(ADMIN, GROUPING, testUhUuid);
        assertTrue(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.optOut(ADMIN, GROUPING, testUhUuid);
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertTrue(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        updateMemberService.optIn(ADMIN, GROUPING, testUhUuid);
        assertTrue(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_EXCLUDE, testUhUuid));

        removeGroupMember(GROUPING_INCLUDE, testUhUuid);
    }

    @Test
    public void removeFromGroupsTest() {
        String testUhUuid = testPerson.getUhUuid();

        updateMemberService.addOwnership(ADMIN, GROUPING, testUhUuid);
        updateMemberService.addIncludeMember(ADMIN, GROUPING, testUhUuid);
        assertTrue(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertTrue(memberService.isMember(GROUPING_OWNERS, testUhUuid));

        String[] array = { GROUPING_OWNERS, GROUPING_INCLUDE, GROUPING_EXCLUDE };
        List<String> groupPaths = Arrays.asList(array);

        updateMemberService.removeFromGroups(ADMIN, testUhUuid, groupPaths);
        assertFalse(memberService.isMember(GROUPING_INCLUDE, testUhUuid));
        assertFalse(memberService.isMember(GROUPING_OWNERS, testUhUuid));

        array = new String[] { GROUPING_OWNERS, GROUPING_INCLUDE, GROUPING_EXCLUDE, GROUPING };
        groupPaths = Arrays.asList(array);

        try {
            updateMemberService.removeFromGroups(ADMIN, testUhUuid, groupPaths);
            fail("Should throw an exception an invalid groupPath is in the group list");
        } catch (GcWebServiceError e) {
            assertEquals("404: Invalid group path.", e.getContainerResponseObject().toString());
        }
    }

    @Test
    public void resetGroupTest() {
        List<String> includes = TEST_UH_USERNAMES.subList(0, 2);
        List<String> excludes = TEST_UH_USERNAMES.subList(3, 5);
        updateMemberService.addIncludeMembers(ADMIN, GROUPING, includes);
        updateMemberService.addExcludeMembers(ADMIN, GROUPING, excludes);

        GroupingsReplaceGroupMembersResult resultInclude = updateMemberService.resetIncludeGroup(ADMIN, GROUPING);
        GroupingsReplaceGroupMembersResult resultExclude = updateMemberService.resetExcludeGroup(ADMIN, GROUPING);

        assertEquals(SUCCESS, resultInclude.getResultCode());
        assertEquals(SUCCESS, resultExclude.getResultCode());
        for (String str : excludes) {
            assertFalse(memberService.isMember(GROUPING_EXCLUDE, str));
        }
        for (String str : includes) {
            assertFalse(memberService.isMember(GROUPING_INCLUDE, str));
        }
    }

    @Test
    public void addRemoveOwnershipsTest() {
        updateMemberService.addOwnerships(ADMIN, GROUPING, TEST_UH_USERNAMES);
        for (String uid : TEST_UH_USERNAMES) {
            assertTrue(memberService.isMember(GROUPING_OWNERS, uid));
        }
        updateMemberService.removeOwnerships(ADMIN, GROUPING, TEST_UH_USERNAMES);

        for (String uid : TEST_UH_USERNAMES) {
            assertFalse(memberService.isMember(GROUPING_OWNERS, uid));
        }
    }

    @Test
    public void addRemoveOwnershipTest() {
        String testUhUuid = testPerson.getUhUuid();
        updateMemberService.addOwnership(ADMIN, GROUPING, testUhUuid);
        assertTrue(memberService.isMember(GROUPING_OWNERS, testUhUuid));
        updateMemberService.removeOwnership(ADMIN, GROUPING, testUhUuid);
        assertFalse(memberService.isMember(GROUPING_OWNERS, testUhUuid));
    }

    @Test
    public void checkIfOwnerOrAdminUserTest(){
        String testUhUuid = testPerson.getUhUuid();
        // Should not throw an exception if current user is an admin and an owner.
        try {
            updateMemberService.checkIfOwnerOrAdminUser(ADMIN, GROUPING);
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if current user is an admin and an owner.");
        }

        // Should not throw an exception if current user is an owner of grouping.
        addGroupMember(GROUPING_OWNERS, testUhUuid);
        try {
            updateMemberService.checkIfOwnerOrAdminUser(testUhUuid, GROUPING);
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if current user is an owner of grouping.");
        }

        // Should not throw an exception if current user is an owner of grouping and an admin.
        addGroupMember(GROUPING_ADMINS, testUhUuid);
        try {
            updateMemberService.checkIfOwnerOrAdminUser(testUhUuid, GROUPING);
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if current user is an owner of grouping and an admin.");
        }
        removeGroupMember(GROUPING_OWNERS, testUhUuid);

        // Should not throw an exception if current user an admin but not an owner of grouping.
        try {
            updateMemberService.checkIfOwnerOrAdminUser(testUhUuid, GROUPING);
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if current user an admin but not an owner of grouping.");
        }
        removeGroupMember(GROUPING_ADMINS, testUhUuid);

        // Should throw is not an admin or an owner of grouping.
        try {
            updateMemberService.checkIfOwnerOrAdminUser(testUhUuid, GROUPING);
            fail("Should throw an exception is not an admin or an owner of grouping.");
        } catch (AccessDeniedException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void checkIfSelfOptOrAdminTest() {
        try {
            updateMemberService.checkIfSelfOptOrAdmin(TEST_UH_NUMBERS.get(0), TEST_UH_NUMBERS.get(1));
            fail("Should throw an exception if currentUser is not admin and currentUser is not self opting.");
        } catch (AccessDeniedException e) {
            assertNull(e.getCause());
        }

        try {
            updateMemberService.checkIfSelfOptOrAdmin(ADMIN, TEST_UH_NUMBERS.get(0));
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if currentUser is admin but currentUser is not self opting.");
        }

        try {
            updateMemberService.checkIfSelfOptOrAdmin(ADMIN, ADMIN);
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if currentUser is admin and currentUser is self opting.");
        }

        try {
            updateMemberService.checkIfSelfOptOrAdmin(TEST_UH_NUMBERS.get(0), TEST_UH_NUMBERS.get(0));
        } catch (AccessDeniedException e) {
            fail("Should not throw an exception if currentUser is not admin but currentUser is self opting.");
        }

    }

    private void addGroupMember(String groupPath, String uhIdentifier) {
        new AddMemberCommand(groupPath, uhIdentifier).execute();
    }

    private void removeGroupMember(String groupPath, String uhIdentifier) {
        new RemoveMemberCommand(groupPath, uhIdentifier).execute();
    }

}
