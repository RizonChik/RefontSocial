package ru.rizonchik.refontsocial.storage.model;

import java.util.UUID;

public final class PlayerRep {

    private final UUID uuid;
    private final String name;

    private final int likes;
    private final int dislikes;
    private final int votes;
    private final double score;

    public PlayerRep(UUID uuid, String name, int likes, int dislikes, int votes, double score) {
        this.uuid = uuid;
        this.name = name;
        this.likes = likes;
        this.dislikes = dislikes;
        this.votes = votes;
        this.score = score;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getLikes() {
        return likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public int getVotes() {
        return votes;
    }

    public double getScore() {
        return score;
    }
}