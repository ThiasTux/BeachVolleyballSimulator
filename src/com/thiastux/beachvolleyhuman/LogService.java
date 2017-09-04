package com.thiastux.beachvolleyhuman;

import com.thiastux.beachvolleyhuman.model.Score;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThiasTux.
 */
public class LogService {

    //Saving scores
    private String filePath = "./scores.csv";
    private File scoreFile;

    public LogService() {

        try {
            scoreFile = new File(filePath);
            if (!scoreFile.exists())
                scoreFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveScores(List<Score> scores) {
        scores.sort(Score.comparator);
        try (BufferedWriter writer = Files.newBufferedWriter(scoreFile.toPath())) {
            for (Score score : scores) {
                writer.write(score.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Score> readScoresFromFile() {
        List<Score> scores = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(scoreFile.toPath(), Charset.forName("UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] scoreString = line.split(",");
                scores.add(new Score(scoreString[0].trim(),
                        Integer.parseInt(scoreString[1].trim()),
                        Integer.parseInt(scoreString[2].trim()),
                        Integer.parseInt(scoreString[3].trim())));
            }
            return scores;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scores;
    }
}
