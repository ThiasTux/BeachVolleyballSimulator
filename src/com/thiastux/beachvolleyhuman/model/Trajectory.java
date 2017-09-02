package com.thiastux.beachvolleyhuman.model;

import com.jme3.math.Vector3f;

/**
 * Created by ThiasTux.
 */
public class Trajectory {

    private Vector3f direction;
    private int multiplier;

    public Trajectory(Vector3f direction, int multiplier) {
        this.direction = direction;
        this.multiplier = multiplier;
    }

    public Trajectory(double x, double y, double z, int multiplier) {
        this.direction = new Vector3f((float) x, (float) y, (float) z);
        this.multiplier = multiplier;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public int getMultiplier() {
        return multiplier;
    }
}
