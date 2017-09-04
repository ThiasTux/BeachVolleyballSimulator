package com.thiastux.beachvolleyhuman.model;

import java.util.Comparator;

/**
 * Created by ThiasTux.
 */
public class Score {

    private String playerName;
    private int score;
    private int numShots;
    private int maxNumShots;
    public static Comparator<Score> comparator = new Comparator<Score>() {
        @Override
        public int compare(Score score, Score t1) {
            return t1.getScore() - score.getScore();
        }
    };

    public Score(String playerName, int score, int numShots, int maxNumShots) {
        this.playerName = playerName;
        this.score = score;
        this.numShots = numShots;
        this.maxNumShots = maxNumShots;
    }

    public String getPlayerName() {

        return playerName;
    }

    public int getScore() {
        return score;
    }

    public Comparator<Score> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return String.format("%s, %d, %d, %d", playerName, score, numShots, maxNumShots);
    }
}
