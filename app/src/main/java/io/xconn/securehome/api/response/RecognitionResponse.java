package io.xconn.securehome.api.response;

public class RecognitionResponse {
    private boolean recognized;
    private String userName;
    private double confidence;
    private String message;

    // Constructors
    public RecognitionResponse() {}

    public RecognitionResponse(boolean recognized, String userName, double confidence, String message) {
        this.recognized = recognized;
        this.userName = userName;
        this.confidence = confidence;
        this.message = message;
    }

    // Getters and setters
    public boolean isRecognized() {
        return recognized;
    }

    public void setRecognized(boolean recognized) {
        this.recognized = recognized;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}