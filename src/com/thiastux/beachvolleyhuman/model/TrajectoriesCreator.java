package com.thiastux.beachvolleyhuman.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThiasTux.
 */
public class TrajectoriesCreator {

    private static TrajectoriesCreator defaultTrajectories = null;
    private List<Trajectory> trajectories = new ArrayList<>();

    private TrajectoriesCreator() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./trajectories.csv"));
            String line = reader.readLine();
            while (line != null) {
                String[] data = line.split(",");
                double x = Double.parseDouble(data[0].trim());
                double y = Double.parseDouble(data[1].trim());
                double z = Double.parseDouble(data[2].trim());
                int mult = Integer.parseInt(data[3].trim());
                trajectories.add(new Trajectory(x, y, z, mult));
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public TrajectoriesCreator getDefaultTrajectories() {
        if (defaultTrajectories == null) {
            defaultTrajectories = new TrajectoriesCreator();
        }
        return defaultTrajectories;
    }

    public List<Trajectory> getTrajectories() {
        return trajectories;
    }
}
