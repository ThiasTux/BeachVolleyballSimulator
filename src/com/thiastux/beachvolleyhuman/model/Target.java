package com.thiastux.beachvolleyhuman.model;

import com.jme3.scene.shape.Torus;

/**
 * Created by ThiasTux.
 */
public class Target extends Torus {

    private int score;

    public Target(int score) {
        this.score = score;
    }

    public Target(int circleSamples, int radialSamples, float innerRadius, float outerRadius, int score) {
        super(circleSamples, radialSamples, innerRadius, outerRadius);
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
