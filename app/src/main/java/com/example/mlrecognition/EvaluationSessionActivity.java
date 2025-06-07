package com.example.mlrecognition;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.Manifest;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EvaluationSessionActivity extends Activity {

    private ImageView questionImage;
    private TextView questionText, scoreTextView;
    private Button captureButton, backButton, homeButton;

    private static final String CSV_FILE_NAME = "App_3_evaluation.csv";
    private Interpreter tflite;
    private final String[] words = {
            "cat","dog","gift","mango","tomato","lion","nose","pineapple",
            "watermelon" , "fan","key","orange","rainbow","ant","ice"
            ,"queen","umbrella" , "ball" , "star" , "van","xmas","zebra"
    };

    private ArrayList<String> remainingWords;
    private String currentAnswer;
    private char missingLetter;
    private int currentRound = 1;
    private int correctAnswers = 0;
    private HandLandmarker handLandmarker;
    private int wrongAnswers = 0;

    private static final int TOTAL_ROUNDS = 20;
    private static final int VIDEO_CAPTURE_REQUEST = 101;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Matrix matrix_val = null;
    private boolean landmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluation_session);

        questionImage = findViewById(R.id.questionImage);
        questionText = findViewById(R.id.questionText);
        scoreTextView = findViewById(R.id.scoreTextView);
        //userNameTextView = findViewById(R.id.userNameTextView); // Assuming userName is displayed
        captureButton = findViewById(R.id.captureButton);
        backButton = findViewById(R.id.backButton);
        homeButton = findViewById(R.id.homeButton);

        // Retrieve the user name from the previous activity

        remainingWords = new ArrayList<>();
        Collections.addAll(remainingWords, words);
        //Collections.shuffle(remainingWords);

        if (hasCameraPermission()) {
            loadModel();
            initializeHandLandmarker(this);
            loadNextQuestion();
        } else {
            requestCameraPermission();
        }

        captureButton.setOnClickListener(v -> startVideoCapture());
        backButton.setOnClickListener(v -> finish());
        homeButton.setOnClickListener(v -> navigateToHome());
    }

    private void initializeHandLandmarker(Context context)
    {
        try {
            // Set base options
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task") // Ensure this file exists in assets
                    .build();

            // Configure hand landmarker options
            HandLandmarker.HandLandmarkerOptions options =
                    HandLandmarker.HandLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setMinHandDetectionConfidence(0.3f) // Adjust confidence as per your requirements
                            .setMinTrackingConfidence(0.3f)
                            .setMinHandPresenceConfidence(0.3f)
                            .setNumHands(2) // Max hands to detect
                            .setRunningMode(RunningMode.IMAGE) // Change to VIDEO or LIVE_STREAM if needed
                            .build();

            // Create the hand landmarker instance
            handLandmarker = HandLandmarker.createFromOptions(context, options);
            Log.d("HandLandmarker", "Hand Landmarker initialized successfully.");
        } catch (Exception e) {
            Log.e("HandLandmarker", "Error initializing Hand Landmarker", e);
        }
    }

    private void navigateToHome() {
        Intent homeIntent = new Intent(EvaluationSessionActivity.this, MainActivity.class);
        startActivity(homeIntent);
        finish();
    }

    // Load the TensorFlow Lite model
    private void loadModel() {
        try {
            tflite = new Interpreter(loadModelFile());
            Toast.makeText(this, "Model loaded successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error loading model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadNextQuestion() {
        if (currentRound > TOTAL_ROUNDS) {
            showFinalResult();
            return;
        }

        currentAnswer = remainingWords.remove(0);
        missingLetter = currentAnswer.charAt(0);
        String displayedText = currentAnswer.replaceFirst(String.valueOf(missingLetter), "_");

        int resId = getResources().getIdentifier(currentAnswer, "drawable", getPackageName());
        if (resId != 0) {
            questionImage.setImageResource(resId);
        }

        questionText.setText(displayedText);
        scoreTextView.setText("Round: " + currentRound + " | Correct: " + correctAnswers + " | Wrong: " + wrongAnswers);
    }

    private char selectRandomLetter(String word) {
        Random random = new Random();
        return word.charAt(random.nextInt(word.length()));
    }

    private void startVideoCapture() {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 2); // 2 seconds video
        if (videoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(videoIntent, VIDEO_CAPTURE_REQUEST);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            processVideo(videoUri);
        }
    }

    private void processVideo(Uri videoUri) {
        new Handler().postDelayed(() -> {
            List<Character> predictedLetters = new ArrayList<>();
            long startTime = System.currentTimeMillis();  // Start time for the round

            // Use MediaMetadataRetriever to get video duration
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, videoUri);

            // Get the video duration in milliseconds
            String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long videoDuration = Long.parseLong(durationString);

            // Get the last frame of the 2-second video (at 1999 milliseconds)
            long lastFrameTime = 1900;

            Bitmap frame = retriever.getFrameAtTime(lastFrameTime);
            frame = rotateImage(frame); // Rotate to align with expected input format

            if (frame != null) {
                List<Float> frameInputList = classifyImage(frame); // Extract features
                char predictedLetter = runMLModel(convertFloatArrayListToByteBuffer((ArrayList<Float>) frameInputList)); // Get the letter
                predictedLetters.add(predictedLetter); // Store the prediction
                frame.recycle(); // Free memory after processing
            }


            // Find the most frequent predicted letter
            char mostFrequentLetter = getMostFrequentLetter(predictedLetters);

            // Calculate time taken for prediction
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;  // in milliseconds

            // Update UI and check correctness
            if (mostFrequentLetter == missingLetter) {
                correctAnswers++;
                showMessage("CORRECT! The answer was : " + missingLetter);
            } else {
                wrongAnswers++;
                showMessage("WRONG! Correct answer was: " + missingLetter + "\n" + "Your Prediction was : " + mostFrequentLetter);
            }

            // Append the result to the CSV file
            appendResultToCSV(mostFrequentLetter, missingLetter, timeTaken);

            // Move to the next round
            currentRound++;
            loadNextQuestion();
        }, 2000); // 2s delay after video capture for processing
    }


    private void appendResultToCSV(char predictedLetter, char correctLetter, long timeTaken) {
        try {
            File csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), CSV_FILE_NAME);
            FileWriter writer = new FileWriter(csvFile, true);  // Append mode
            String dateTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
            writer.append(correctLetter)
                    .append(',')
                    .append(predictedLetter)
                    .append(',')
                    .append(String.valueOf(timeTaken))
                    .append(',')
                    .append(dateTime)
                    .append('\n');
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error writing to CSV file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }





    public Bitmap rotateImage(Bitmap image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            Log.e("rotateImage", "Invalid image: Null or zero size");
            return null;
        }

        try {
            // Ensure correct format for MediaPipe (ARGB_8888)
            Bitmap compatibleBitmap = image.copy(Bitmap.Config.ARGB_8888, true);

            // Create a new rotation matrix
            Matrix matrix = new Matrix();
            matrix.postRotate(270);

            // Rotate and return a new Bitmap
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    compatibleBitmap, 0, 0,
                    image.getWidth(),
                    image.getHeight(),
                    matrix, true
            );

            return rotatedBitmap;

        } catch (Exception e) {
            Log.e("rotateImage", "Error rotating image", e);
            return null;
        }
    }

    public List<Float> classifyImage(Bitmap image) {
        if (handLandmarker == null) {
            initializeHandLandmarker(this);
        }

        try {
            // Convert Bitmap to MPImage
            MPImage mpImage = new BitmapImageBuilder(image).build();

            // Detect hands
            HandLandmarkerResult result = handLandmarker.detect(mpImage);
            result.handednesses().get(0).get(0).categoryName();
            if (result != null && result.landmarks().size() > 0) {
                // Example: Process the first detected hand's landmarks
                if(result.landmarks().size()>2){
                    Log.d("MAIN ACTIV","More Hands");
                }
                return HandLandmarkProcessor.preProcessLandmark(result);

            } }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private char runMLModel(ByteBuffer inputBuffer) {
        try {
            // Create the input tensor of shape [1, 84]
            TensorBuffer inputFeature = TensorBuffer.createFixedSize(new int[]{1, 84}, DataType.FLOAT32);
            inputFeature.loadBuffer(inputBuffer);

            // Create the output tensor of shape [1, 26]
            TensorBuffer outputFeature = TensorBuffer.createFixedSize(new int[]{1, 26}, DataType.FLOAT32);

            // Run model inference
            tflite.run(inputFeature.getBuffer(), outputFeature.getBuffer());

            // Get output as a float array
            float[] output = outputFeature.getFloatArray();

            // Find the index with the highest probability
            int predictedIndex = 0;
            for (int i = 1; i < output.length; i++) {
                if (output[i] > output[predictedIndex]) {
                    predictedIndex = i;
                }
            }

            // Return the predicted letter
            return (char) ('a' + predictedIndex);

        } catch (Exception e) {
            Toast.makeText(this, "Prediction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Model Error", "Error during prediction", e);
            return ' ';  // Return blank if an error occurs
        }
    }

    public ByteBuffer convertFloatArrayListToByteBuffer(ArrayList<Float> floatList) {
        if (floatList == null || floatList.size() != 84) {
            throw new IllegalArgumentException("Input list must have exactly 84 float values.");
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(84 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        for (Float value : floatList) {
            byteBuffer.putFloat(value);
        }

        byteBuffer.rewind();  // Reset the buffer position to 0

        return byteBuffer;
    }

    private char getMostFrequentLetter(List<Character> predictedLetters) {
        int[] frequency = new int[26]; // 26 letters in the alphabet

        // Count the frequency of each predicted letter
        for (char predictedLetter : predictedLetters) {
            if (predictedLetter != ' ') { // Ignore invalid predictions
                frequency[predictedLetter - 'a']++;
            }
        }

        // Find the letter with the highest frequency
        int maxFrequency = -1;
        char mostFrequentLetter = ' ';

        for (int i = 0; i < 26; i++) {
            if (frequency[i] > maxFrequency) {
                maxFrequency = frequency[i];
                mostFrequentLetter = (char) ('a' + i);
            }
        }

        return mostFrequentLetter;
    }

    private void showMessage(String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Result")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showFinalResult() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Final Score: " + correctAnswers + " / " + TOTAL_ROUNDS)
                .setPositiveButton("Home", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to load the model and next question
                loadModel();
                loadNextQuestion();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

}