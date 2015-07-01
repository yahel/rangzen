package org.denovogroup.rangzen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Config(manifest = "./apps/experimentalApp/AndroidManifest.xml",
        emulateSdk = 18,
        resourceDir = "../../ui/Rangzen/res/")
@RunWith(RobolectricTestRunner.class)
public class RangzemMessageStoreTest {
    /** The instance of MessageStore we're using for tests. */
    private RangzenMessageStore store;

    /** The app instance we're using to pass to MessageStore. */
    private SlidingPageIndicator activity;

    /** Test strings we're writing into the storage system. */
    private static final String TEST_MSG_1 = "message 1";
    private static final String TEST_MSG_2 = "message 2";
    private static final String TEST_MSG_3 = "message 3";

    private static final String TEST_MSG_INVALID = "invalid";

    private static final double TEST_PRIORITY_1 = 1.0;
    private static final double TEST_PRIORITY_2 = 0.2;
    private static final double TEST_PRIORITY_3 = 0.9;
    private static final double TEST_PRIORITY_4 = 0.8;
    private static final double TEST_PRIORITY_5 = 0.7;

    private static final double TEST_PRIORITY_INVALID = 1.1;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();
        store = new RangzenMessageStore(activity);
    }

    /**
     * Tests that we can store a message and retrieve its priority.
     */
    @Test
    public void storeMessageGetPriority() {
        assertNull(store.lookupByMessage(TEST_MSG_1));
        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_1);

        assertEquals(store.lookupByMessage(TEST_MSG_1).mPriority, TEST_PRIORITY_1, 0.1);
    }

    /**
     * Tests that we can store messages and get them in order.
     */
    @Test
    public void storeMessages() {
        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_1);
        store.insertMessage(TEST_MSG_2, TEST_PRIORITY_2);
        store.insertMessage(TEST_MSG_3, TEST_PRIORITY_3);

        final RangzenMessageStore.RangzenAppMessage appMessage1 = new RangzenMessageStore.RangzenAppMessage(TEST_MSG_1, TEST_PRIORITY_1);
        final RangzenMessageStore.RangzenAppMessage appMessage2 = new RangzenMessageStore.RangzenAppMessage(TEST_MSG_2, TEST_PRIORITY_2);
        final RangzenMessageStore.RangzenAppMessage appMessage3 = new RangzenMessageStore.RangzenAppMessage(TEST_MSG_3, TEST_PRIORITY_3);

        List<RangzenMessageStore.RangzenAppMessage> topk = null;
        try {
            topk = store.getKMessages(0);
            assertFalse("Should throw illegal argument execption!", true);
        } catch (IllegalArgumentException e) { }
        assertNull(topk);

        topk = store.getKMessages(1);
        assertEquals(1, topk.size());
        assertTrue(topk.contains(appMessage1));
        assertFalse(topk.contains(appMessage2));
        assertFalse(topk.contains(appMessage3));
        assertEquals(topk.get(0).mPriority, TEST_PRIORITY_1, 0.01);

        topk = store.getKMessages(2);
        assertEquals(2, topk.size());
        assertTrue(topk.contains(appMessage1));
        assertFalse(topk.contains(appMessage2));
        assertTrue(topk.contains(appMessage3));
        assertEquals(topk.get(0).mPriority, TEST_PRIORITY_1, 0.01);
        assertEquals(topk.get(1).mPriority, TEST_PRIORITY_3, 0.01);

        topk = store.getKMessages(3);
        assertEquals(3, topk.size());
        assertTrue(topk.contains(appMessage1));
        assertTrue(topk.contains(appMessage2));
        assertTrue(topk.contains(appMessage3));
        assertEquals(topk.get(0).mPriority, TEST_PRIORITY_1, 0.01);
        assertEquals(topk.get(1).mPriority, TEST_PRIORITY_3, 0.01);
        assertEquals(topk.get(2).mPriority, TEST_PRIORITY_2, 0.01);

        topk = store.getKMessages(4);
        assertEquals(topk.size(), 3);
    }

    /**
     * Regression test for the bug where getKMessages messages only returned one message
     * per unique priority score.
     */
    @Test
    public void regressionGetTopKPriorityScoreTest() {
        store.insertMessage("Test1", TEST_PRIORITY_1);
        store.insertMessage("Test2", TEST_PRIORITY_1);
        store.insertMessage("Test3", TEST_PRIORITY_1);
        store.insertMessage("Test4", TEST_PRIORITY_2);
        store.insertMessage("Test5", TEST_PRIORITY_3);
        store.insertMessage("Test6", TEST_PRIORITY_4);
        store.insertMessage("Test7s", TEST_PRIORITY_4);
        store.insertMessage("Test8", TEST_PRIORITY_4);
        store.insertMessage("test9", TEST_PRIORITY_4);
        store.insertMessage("Test10", TEST_PRIORITY_4);
        store.insertMessage("Test11", TEST_PRIORITY_5);
        store.insertMessage("Test12", TEST_PRIORITY_5);

        final List<RangzenMessageStore.RangzenAppMessage> kMessages = store.getKMessages(12);
        assertEquals(12, kMessages.size());
    }

    @Test
    public void testDeleteAllGetCount() {
        store.insertMessage("Test1", TEST_PRIORITY_1);
        store.insertMessage("Test2", TEST_PRIORITY_1);
        store.insertMessage("Test3", TEST_PRIORITY_1);
        store.insertMessage("Test4", TEST_PRIORITY_2);
        store.insertMessage("Test5", TEST_PRIORITY_3);
        store.insertMessage("Test6", TEST_PRIORITY_4);
        store.insertMessage("Test7s", TEST_PRIORITY_4);
        store.insertMessage("Test8", TEST_PRIORITY_4);
        store.insertMessage("test9", TEST_PRIORITY_4);
        store.insertMessage("Test10", TEST_PRIORITY_4);
        store.insertMessage("Test11", TEST_PRIORITY_5);
        store.insertMessage("Test12", TEST_PRIORITY_5);

        // Get right number of individual messages.
        assertEquals(12, store.getMessageCount());
        store.deleteAll();
        assertEquals(0, store.getMessageCount());
    }

    /**
     * Regression test for the bug where getKMessages returns too many messages because
     * it was stopping based on #bins instead of #messages.
     */
    @Test
    public void regressionGetTopKReturnsTooManyMessages() {
        store.insertMessage("Test1", TEST_PRIORITY_1);
        store.insertMessage("Test2", TEST_PRIORITY_1);
        store.insertMessage("Test3", TEST_PRIORITY_1);
        store.insertMessage("Test4", TEST_PRIORITY_2);
        store.insertMessage("Test5", TEST_PRIORITY_3);
        store.insertMessage("Test6", TEST_PRIORITY_4);
        store.insertMessage("Test7s", TEST_PRIORITY_4);
        store.insertMessage("Test8", TEST_PRIORITY_4);
        store.insertMessage("test9", TEST_PRIORITY_4);
        store.insertMessage("Test10", TEST_PRIORITY_4);
        store.insertMessage("Test11", TEST_PRIORITY_5);
        store.insertMessage("Test12", TEST_PRIORITY_5);

        // Get right number of individual messages.
        assertEquals(11, store.getKMessages(11).size());
        assertEquals(12, store.getKMessages(12).size());
    }

    @Test
    public void duplicateMessageAddTest() {
        int MORE_THAN_3 = 10;
        store.insertMessage("Test1", TEST_PRIORITY_1);
        store.insertMessage("Test1", TEST_PRIORITY_1);

        assertEquals(1, store.getKMessages(MORE_THAN_3).size());
        assertEquals(1, store.getKMessages(MORE_THAN_3).size());

        store.insertMessage("Test1", TEST_PRIORITY_2);

        assertEquals(1, store.getKMessages(MORE_THAN_3).size());
        assertEquals(1, store.getKMessages(MORE_THAN_3).size());
    }

    /**
     * Test the getPriority(String) method of MessageStore.
     */
    @Test
    public void getPriorityTest() {
        assertNull(store.lookupByMessage(TEST_MSG_1));
        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_1);
        assertEquals(TEST_PRIORITY_1, store.lookupByMessage(TEST_MSG_1).mPriority, 0.1);

        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_2);
        assertEquals(TEST_PRIORITY_2, store.lookupByMessage(TEST_MSG_1).mPriority, 0.1);
    }

    /**
     * Test the updatePriority(String, float) method of MessageStore.
     */
    @Test
    public void updatePriorityTest() {
        // No message in store and updating fails when that's the case.
        assertNull(store.lookupByMessage(TEST_MSG_1));
        assertFalse(store.updatePriority(TEST_MSG_1, TEST_PRIORITY_3));
        assertNull(store.lookupByMessage(TEST_MSG_1));

        // Add the message to the store.
        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_1);
        assertEquals(TEST_PRIORITY_1, store.lookupByMessage(TEST_MSG_1).mPriority, 0.1);

        // Update it and find its new priority is correct.
        assertTrue(store.updatePriority(TEST_MSG_1, TEST_PRIORITY_2));
        assertEquals(TEST_PRIORITY_2, store.lookupByMessage(TEST_MSG_1).mPriority, 0.1);
    }

    /**
     * Test the contains(String) method of MessageStore.
     */
    @Test
    public void containsMessageTest() {
        // Message not contained initially.
        assertFalse(store.lookupByMessage(TEST_MSG_1) != null);

        // Add it, now it's contained.
        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_1);
        assertTrue(store.lookupByMessage(TEST_MSG_1) != null);

        // Add it again, still contained.
        store.insertMessage(TEST_MSG_1, TEST_PRIORITY_2);
        assertTrue(store.lookupByMessage(TEST_MSG_1) != null);

        // Update its priority, still contained.
        store.updatePriority(TEST_MSG_1, TEST_PRIORITY_3);
        assertTrue(store.lookupByMessage(TEST_MSG_1) != null);

        // the message is no longer contained.
        store.deleteMessage(TEST_MSG_1);
        assertFalse(store.lookupByMessage(TEST_MSG_1) != null);
    }

    /**
     * Utility method for testing check priority.
     */
    private boolean isValidPriority(double priority) {
        return RangzenMessageStore.priorityAcceptable(priority);
    }

    /**
     * Test that priorityAcceptable bounds the right numbers.
     */
    @Test
    public void checkPriorityTest() {
        assertTrue(isValidPriority(1.0));
        assertTrue(isValidPriority(1.2));
        assertTrue(isValidPriority(1.2398023948234092384));
        assertTrue(isValidPriority(0.5));
        assertTrue(isValidPriority(0.0));
        assertTrue(isValidPriority(0.000000000001));
        assertFalse(isValidPriority(1000.0));
        assertFalse(isValidPriority(-1.0));
        assertFalse(isValidPriority(-2.0));
        assertFalse(isValidPriority(-0.00000001));
        assertFalse(isValidPriority(10.0));
        assertFalse(isValidPriority(2.000001));

    }
}
