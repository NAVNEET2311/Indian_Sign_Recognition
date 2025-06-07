package com.example.mlrecognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MCQGameActivity extends AppCompatActivity {
    public double total_time = 0.0000; // Store the total time for all rounds
    private static final String CSV_FILE_NAME = "App_3_mcq.csv";
    private static final String GAMEPLAY_TIME_CSV_FILE_NAME = "ISL_gameplay_time.csv";
    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 2;

    private TextView statsTextView, resultText;
    private ImageView optionA, optionB, optionC, optionD;
    private Button backButton;

    private final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    private List<String> shuffledLetters;
    private String correctAnswer;
    private int score = 0;
    private int wrongAnswers = 0;
    private int currentLevel = 0;
    private static final int TOTAL_LEVELS = 26;

    private long questionStartTime;
    private long roundStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mcq_game);

        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        resultText = findViewById(R.id.resultText);
        statsTextView = findViewById(R.id.statsTextView);
        backButton = findViewById(R.id.backButton);

        requestStoragePermission();

        shuffleLetters();
        loadNewQuestion();

        optionA.setOnClickListener(view -> checkAnswer(optionA));
        optionB.setOnClickListener(view -> checkAnswer(optionB));
        optionC.setOnClickListener(view -> checkAnswer(optionC));
        optionD.setOnClickListener(view -> checkAnswer(optionD));

        backButton.setOnClickListener(view -> onBackPressed());
    }

    private void shuffleLetters() {
        shuffledLetters = new ArrayList<>();
        Collections.addAll(shuffledLetters, letters);
        //Collections.shuffle(shuffledLetters);
    }

    private void showGameOverDialog() {
        long roundEndTime = System.currentTimeMillis();
        double currentRoundTime = (roundEndTime - roundStartTime) / 1000.0;  // Convert to seconds with decimal precision
        total_time += currentRoundTime; // Add to total round time

        if (hasStoragePermission()) {
            writeRoundTimeToCSV(currentRoundTime);  // Write to ISL_results.csv
            writeTotalTimeForRoundsCSV(total_time);  // Write total round time
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over")
                .setMessage("Your final score: " + score)
                .setCancelable(false)
                .setPositiveButton("Back to Home", (dialog, id) -> {
                    Intent intent = new Intent(MCQGameActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
        builder.create().show();
    }

    private void writeTotalTimeForRoundsCSV(double totalTime) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(directory, "Total_Time_For_Rounds.csv");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

        try (FileWriter fileWriter = new FileWriter(file, true)) {
            if (!file.exists()) {
                fileWriter.append("TOTAL ROUND TIME (seconds),total_time\n");
            }
            fileWriter.append(String.format("%.2f", total_time)).append(",")
                    .append(timestamp).append("\n");
        } catch (IOException e) {
            Toast.makeText(this, "Error writing total round time to CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeRoundTimeToCSV(double currentRoundTime) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(directory, CSV_FILE_NAME);
        try (FileWriter fileWriter = new FileWriter(file, true)) {
            fileWriter.append("Round Time (seconds):").append(String.format("%.2f", total_time)).append("\n");
        } catch (IOException e) {
            Toast.makeText(this, "Error writing round time to CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadNewQuestion() {
        if (currentLevel >= TOTAL_LEVELS) {
            showGameOverDialog();
            return;
        }

        resultText.setVisibility(View.GONE);
        correctAnswer = shuffledLetters.get(currentLevel);
        TextView questionLetter = findViewById(R.id.questionLetter);
        questionLetter.setText(correctAnswer.toUpperCase());
        generateOptions();

        questionStartTime = System.currentTimeMillis();
        roundStartTime = questionStartTime; // Set the round start time at the beginning of the question
    }

    private void generateOptions() {
        List<String> options = new ArrayList<>();
        options.add(correctAnswer);
        while (options.size() < 4) {
            String wrongAnswer = letters[(int) (Math.random() * letters.length)];
            if (!options.contains(wrongAnswer)) {
                options.add(wrongAnswer);
            }
        }
        Collections.shuffle(options);
        setImageOption(optionA, options.get(0));
        setImageOption(optionB, options.get(1));
        setImageOption(optionC, options.get(2));
        setImageOption(optionD, options.get(3));
    }

    private void setImageOption(ImageView imageView, String letter) {
        int letterResId = getResources().getIdentifier(letter, "drawable", getPackageName());
        if (letterResId != 0) {
            imageView.setImageResource(letterResId);
            imageView.setTag(letter);
        } else {
            Toast.makeText(this, "Error: Image not found for " + letter, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAnswer(ImageView selectedOption) {
        String selectedAnswer = (String) selectedOption.getTag();
        double timeTaken = (System.currentTimeMillis() - questionStartTime) / 1000.0; // Convert to seconds with decimal precision
        total_time+=(timeTaken);
        correctAnswer= correctAnswer.toUpperCase();
        selectedAnswer = selectedAnswer.toUpperCase();
        if (hasStoragePermission()) {
            writeToCSV(correctAnswer, selectedAnswer, timeTaken);
        } else {
            requestStoragePermission();
        }

        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            resultText.setText("Correct!");
            resultText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            resultText.setVisibility(View.VISIBLE);
            updateStats();
            resultText.postDelayed(() -> {
                resultText.setVisibility(View.GONE);
                currentLevel++;
                loadNewQuestion();
            }, 500);
        } else {
            wrongAnswers++;
            resultText.setText("Wrong!");
            resultText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            resultText.setVisibility(View.VISIBLE);
            updateStats();
            resultText.postDelayed(() -> {
                resultText.setVisibility(View.GONE);
                currentLevel++;
                loadNewQuestion();
            }, 5000);
        }


    }
    private void updateStats() {
        String stats = "Q: " + (currentLevel + 2) ;//+ "  Correct: " + score + "  Wrong: " + wrongAnswers;
        statsTextView.setText(stats);
    }

    private void writeToCSV(String expectedAnswer, String userAnswer, double timeTaken) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(directory, CSV_FILE_NAME);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

        try (FileWriter fileWriter = new FileWriter(file, true)) {
            if (!file.exists()) {
                fileWriter.append("EXPECTED ANSWER,USER ANSWER,TIME TAKEN (seconds),TIMESTAMP\n");
            }
            fileWriter.append(expectedAnswer).append(",")
                    .append(userAnswer).append(",")
                    .append(String.format("%.2f", timeTaken)).append(",")
                    .append(timestamp).append("\n");
        } catch (IOException e) {
            Toast.makeText(this, "Error writing to CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
        }
    }

    private boolean hasStoragePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }
}
