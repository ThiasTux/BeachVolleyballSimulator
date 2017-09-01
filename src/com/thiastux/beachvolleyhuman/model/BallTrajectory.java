package com.thiastux.beachvolleyhuman.model;

import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThiasTux.
 */
public class BallTrajectory {

    private List<Vector3f> trajectories = new ArrayList<>();
    private static BallTrajectory defaultTrajectories = null;

    public BallTrajectory() {
        trajectories.add(new Vector3f());
    }

    static public BallTrajectory getDefaultTrajectories() {
        if (defaultTrajectories == null) {
            defaultTrajectories = new BallTrajectory();
        }
        return defaultTrajectories;
    }

}
