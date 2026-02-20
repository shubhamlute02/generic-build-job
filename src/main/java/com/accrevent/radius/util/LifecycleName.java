package com.accrevent.radius.util;

import java.util.Arrays;
import java.util.List;

public class LifecycleName {

    public static final String IN_WORK = "In Work";
    public static final String COMPLETED = "Completed";

    public static final String BACKLOG = "Backlog";
    public static final String PERPARATION = "Preparation";
    public static final String ACTIVE = "Active";
    public static final String CLOSING = "Closing";
    public static final String ON_HOLD = "OnHold";
    public static final String CANCELLED = "Cancelled";

    public static final String IDENTIFIED = "Identified";
    public static final String RESEARCH = "Research";
    public static final String PROSPECTING = "Prospecting";
    public static final String DISQUALIFIED = "Disqualified";
    public static final String OPPORTUNITY_CREATED = "Opportunity Created";

    public static final String DISCOVERY = "Discovery";
    public static final String PROPOSAL = "Proposal";
    public static final String CUSTOMER_EVALUATING = "Customer Evaluating";
    public static final String CLOSED_WON = "Closed Won";
    public static final String CLOSED_LOST = "Closed Lost";
    public static final String NEGOTIATING = "Negotiating";

    public static final String NOT_STARTED = "Not Started";
    public static final String IN_PROGRESS = "In Progress";

    public static final String INTRO = "Intro";
    public static final String FOLLOW_UP_1 = "Follow Up 1";
    public static final String FOLLOW_UP_2 = "Follow Up 2";
    public static final String FOLLOW_UP_3 = "Follow Up 3";
    public static final String CLOSURE = "Closure";
    public static final String MEETING = "Meeting";

    public static final String CONNECT = "Connect";
    public static final String WAITING_FOR_ACCEPTANCE = "Waiting For Acceptance";
    public static final String ACCEPTED = "Accepted";



    public static final String CALLING = "Calling";
    public static final String STOPPED = "Stopped";

    public static final String NOT_REACHABLE = "Not Reachable";
    public static final String INCORRECT_NUMBER = "Incorrect Number";
    public static final String DIDNOT_ANSWER = "Did Not Answer";
    public static final String NOT_INTERESTED = "Not Interested";
    public static final String CALL_BACK_LATER = "Call Back Later";

    public static final String PLANNING = "Planning";
    public static final String IDEATION_AND_REASEARCH = "Ideation & Research";
    public static final String CREATION = "Creation";
    public static final String REVIEW = "Review";
    public static final String APPROVED = "Approved";



    public static List<String> getEmailOutreachTaskLifecycleNames() {
        return Arrays.asList(
                NOT_STARTED,
                INTRO,
                FOLLOW_UP_1,
                FOLLOW_UP_2,
                FOLLOW_UP_3,
                CLOSURE,
                MEETING,
                COMPLETED
        );
    }

    public static List<String> getTaskLifecycleNames() {
        return Arrays.asList(
                NOT_STARTED,
                IN_WORK,
                COMPLETED
        );
    }

    public static List<String> getCampaignLifecycleNames() {
        return Arrays.asList(
                BACKLOG,
                PERPARATION,
                ACTIVE,
                CLOSING,
                ON_HOLD,
                CANCELLED,
                COMPLETED     //instead of complete
        );
    }

    public static List<String> getOutreachCampaignLifecycleNames() {
        return Arrays.asList(
                NOT_STARTED,
                IN_PROGRESS,
                COMPLETED //instead of Complete
        );
    }

    public static List<String> getLeadLifecycleNames() {
        return Arrays.asList(
                IDENTIFIED,
                RESEARCH,
                PROSPECTING,
                DISQUALIFIED,
                OPPORTUNITY_CREATED
        );
    }

    public static List<String> getOpportunityLifecycleNames() {
        return Arrays.asList(
                DISCOVERY,
                PROPOSAL,
                CUSTOMER_EVALUATING,
                NEGOTIATING,
                CLOSED_WON,
                CLOSED_LOST
        );
    }

    public static List<String> getLinkedInOutreachTaskLifecycleNames() {
        return Arrays.asList(
                NOT_STARTED,
                CONNECT,
                WAITING_FOR_ACCEPTANCE,
                INTRO,
                FOLLOW_UP_1,
                FOLLOW_UP_2,
                FOLLOW_UP_3,
                CLOSURE,
                MEETING,
                COMPLETED
        );
    }

    public static List<String> getPhoneOutreachTaskLifecycleNames() {
        return Arrays.asList(
                NOT_STARTED,
                CALLING,
                STOPPED,
                MEETING,
                COMPLETED
        );
    }

    public static List<String> getPromotionAutomationPhoneOutreachTaskLifecycleNames() {
        return Arrays.asList(
                NOT_REACHABLE,
                INCORRECT_NUMBER,
                DIDNOT_ANSWER,
                NOT_INTERESTED,
                CALL_BACK_LATER,
                MEETING
        );
    }

    public static List<String> getVersionLifecycleNames() {
        return Arrays.asList(
               PLANNING,
                IDEATION_AND_REASEARCH,
                CREATION,
                REVIEW,
                APPROVED
        );
    }

}
