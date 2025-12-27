package ru.rizonchik.refontsocial.storage.model;

public final class VoteLogEntry {
    private final long timeMillis;
    private final int value; // 1 like, 0 dislike
    private final String reason; // tag key or display
    private final String voterName; // optional

    public VoteLogEntry(long timeMillis, int value, String reason, String voterName) {
        this.timeMillis = timeMillis;
        this.value = value;
        this.reason = reason;
        this.voterName = voterName;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public int getValue() {
        return value;
    }

    public String getReason() {
        return reason;
    }

    public String getVoterName() {
        return voterName;
    }
}