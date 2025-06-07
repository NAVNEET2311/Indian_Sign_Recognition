package com.example.mlrecognition;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    private Button practiceSessionButton;
    private Button trainingSessionButton;
    private Button evaluationSessionButton; // New Button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        practiceSessionButton = findViewById(R.id.practiceSessionButton);
        trainingSessionButton = findViewById(R.id.trainingSessionButton);
        evaluationSessionButton = findViewById(R.id.evaluationSessionButton); // Initialize new button

        //gestureRecognizerButton.setOnClickListener(new View.OnClickListener() {
         //   @Override
        //    public void onClick(View view) {
         //       // Open Gesture Recognizer Activity
         //       Intent intent = new Intent(MainActivity.this, GestureRecognitionActivity.class);
         //       startActivity(intent);
         //   }
        //});

        practiceSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open MCQ Game Activity
                Intent intent = new Intent(MainActivity.this, MCQGameActivity.class);
                startActivity(intent);
            }
        });

        trainingSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open Training Session Activity
                Intent intent = new Intent(MainActivity.this, TrainingSessionActivity.class);
                startActivity(intent);
            }
        });

        evaluationSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open Evaluation Session Activity
                Intent intent = new Intent(MainActivity.this, EvaluationSessionActivity.class);
                startActivity(intent);
            }
        });
    }
}
