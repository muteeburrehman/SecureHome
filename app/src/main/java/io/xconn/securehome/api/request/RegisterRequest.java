package io.xconn.securehome.api.request;

public class RegisterRequest {
    private String full_name;
    private String email;
    private String phone_number;
    private String password;

    public RegisterRequest(String full_name, String email, String phone_number, String password) {
        this.full_name = full_name;
        this.email = email;
        this.phone_number = phone_number;
        this.password = password;
    }

    // Getters and setters
    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone_number() { return phone_number; }
    public void setPhone_number(String phone_number) { this.phone_number = phone_number; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}