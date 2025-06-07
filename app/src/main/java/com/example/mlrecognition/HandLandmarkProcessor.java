package com.example.mlrecognition;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import android.os.Environment;
import android.util.Log;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HandLandmarkProcessor {

    private static final String CSV_FILE_NAME = "App_3_landmarks.csv";

    public static List<Float> preProcessLandmark(HandLandmarkerResult result) {
        try {
            List<List<NormalizedLandmark>> bothHandLandmarks = result.landmarks();

            if (result == null || bothHandLandmarks == null || bothHandLandmarks.size() == 0 || bothHandLandmarks.size() > 2) {
                // Invalid input: no hands or more than two hands detected
                return null;
            }

            if (bothHandLandmarks.size() == 1) { // One hand detected
                List<Float> handLandmarks = new ArrayList<>();
                // Convert to relative coordinates
                float baseX = bothHandLandmarks.get(0).get(0).x();
                float baseY = bothHandLandmarks.get(0).get(0).y();

                for (int i = 0; i < 42; i++) {
                    handLandmarks.add(0.0f); // Initialize with default values
                }

                for (NormalizedLandmark point : bothHandLandmarks.get(0)) {
                    handLandmarks.add(point.y() - baseY);
                    handLandmarks.add(point.x() - baseX);
                }

                // Normalize landmarks
                float maxElement = -1;
                for (float key : handLandmarks) {
                    maxElement = max(maxElement, abs(key));
                }

                // Avoid division by zero
                if (maxElement != 0) {
                    for (int i = 0; i < handLandmarks.size(); i++) {
                        handLandmarks.set(i, handLandmarks.get(i) / maxElement);
                    }
                }
                saveToCSV(handLandmarks);
                return handLandmarks;

            } else if (bothHandLandmarks.size() == 2) {
                List<NormalizedLandmark> leftHand;
                List<NormalizedLandmark> rightHand;

                if (Objects.equals(result.handednesses().get(0).get(0).categoryName(), "Left")) {
                    leftHand = bothHandLandmarks.get(0);
                    rightHand = bothHandLandmarks.get(1);
                } else {
                    leftHand = bothHandLandmarks.get(1);
                    rightHand = bothHandLandmarks.get(0);
                }

                List<Float> handLandmarks = new ArrayList<>();

                float baseX = (leftHand.get(0).x() + rightHand.get(0).x()) / 2.0f;
                float baseY = (leftHand.get(0).y() + rightHand.get(0).y()) / 2.0f;

                for (NormalizedLandmark point : leftHand) {
                    handLandmarks.add(point.y() - baseY);
                    handLandmarks.add(point.x() - baseX);
                }

                for (NormalizedLandmark point : rightHand) {
                    handLandmarks.add(point.y() - baseY);
                    handLandmarks.add(point.x() - baseX);
                }

                // Normalize landmarks
                float maxElement = -1;
                for (float key : handLandmarks) {
                    maxElement = max(maxElement, abs(key));
                }

                // Avoid division by zero
                if (maxElement != 0) {
                    for (int i = 0; i < handLandmarks.size(); i++) {
                        handLandmarks.set(i, handLandmarks.get(i) / maxElement);
                    }
                }
                saveToCSV(handLandmarks);
                return handLandmarks;
            }
        } catch (Exception e) {
            Log.e("HandLandmarkProcessor", "Error processing landmarks", e);
            return new ArrayList<>(84); // Return default list on error
        }
        saveToCSV(new ArrayList<>(84));
        return new ArrayList<>(84);
    }

    private static void saveToCSV(List<Float> handLandmarks) {
        try {
            File csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), CSV_FILE_NAME);
            FileWriter writer = new FileWriter(csvFile, true);  // Append mode

            for (int i = 0; i < handLandmarks.size(); i++) {
                writer.append(String.valueOf(handLandmarks.get(i)));
                if (i < handLandmarks.size() - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CSV_ERROR", "Error writing hand landmarks to CSV: " + e.getMessage());
        }
    }
}
