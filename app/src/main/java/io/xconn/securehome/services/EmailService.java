package io.xconn.securehome.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


/**
 * Service for sending emails via SMTP
 */
public class EmailService {
    private static final String TAG = "EmailService";

    // Email configuration - Move to secure storage or environment variables
    // For development: use BuildConfig fields defined in build.gradle
    private static final String EMAIL_USERNAME = "muteeb285@gmail.com";
    private static final String EMAIL_PASSWORD = "kexi fbky edvc gkyz"; // IMPORTANT: This is an app password for Gmail.
    private static final String EMAIL_HOST = "smtp.gmail.com";
    private static final String EMAIL_PORT = "465"; // Use "587" for TLS

    private final Context context;
    private final Executor executor;

    /**
     * Constructor
     * @param context Application context
     */
    public EmailService(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Send a registration request notification email to admins
     * @param adminEmail Admin email address
     * @param userName Name of the requesting user
     * @param userEmail Email of the requesting user
     */
    public void sendRegistrationRequestToAdmin(String adminEmail, String userName, String userEmail) {
        executor.execute(() -> {
            try {
                // Configure email properties
                Properties props = getEmailProperties();

                // Create a session with sender credentials
                Session session = createEmailSession(props);

                // Create email message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_USERNAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminEmail));
                message.setSubject("SecureHome: New Registration Request");

                // Create message body
                String emailBody =
                        "<html><body>" +
                                "<h2>New User Registration Request</h2>" +
                                "<p>A new user has requested to join the SecureHome system:</p>" +
                                "<p><b>Name:</b> " + userName + "</p>" +
                                "<p><b>Email:</b> " + userEmail + "</p>" +
                                "<p>Please log in to the admin dashboard to approve or reject this request.</p>" +
                                "<p>Thank you,<br>SecureHome Team</p>" +
                                "</body></html>";

                // Set up the HTML email part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(emailBody, "text/html; charset=utf-8");

                // Create a multipart message
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // Set content
                message.setContent(multipart);

                // Send the message
                Transport.send(message);

                Log.d(TAG, "Registration request email sent to admin: " + adminEmail);
                showToast("Email notification sent to administrator");
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send registration request email", e);
                showToast("Failed to send email notification: " + e.getMessage());
            }
        });
    }

    /**
     * Send an approval notification email to the user
     * @param userEmail Email of the approved user
     * @param userName Name of the approved user
     */
    public void sendApprovalNotificationToUser(String userEmail, String userName) {
        executor.execute(() -> {
            try {
                // Configure email properties
                Properties props = getEmailProperties();

                // Create a session with sender credentials
                Session session = createEmailSession(props);

                // Create email message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_USERNAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
                message.setSubject("SecureHome: Account Approved");

                // Create message body
                String emailBody =
                        "<html><body>" +
                                "<h2>Your Account Has Been Approved</h2>" +
                                "<p>Dear " + userName + ",</p>" +
                                "<p>We're pleased to inform you that your SecureHome account has been approved.</p>" +
                                "<p>You can now log in to the application and access all features of the system.</p>" +
                                "<p>Thank you for choosing SecureHome!</p>" +
                                "<p>Best regards,<br>SecureHome Team</p>" +
                                "</body></html>";

                // Set up the HTML email part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(emailBody, "text/html; charset=utf-8");

                // Create a multipart message
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // Set content
                message.setContent(multipart);

                // Send the message
                Transport.send(message);

                Log.d(TAG, "Approval notification email sent to: " + userEmail);
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send approval notification email", e);
            }
        });
    }

    /**
     * Send a rejection notification email to the user
     * @param userEmail Email of the rejected user
     * @param userName Name of the rejected user
     * @param reason Optional reason for rejection
     */
    public void sendRejectionNotificationToUser(String userEmail, String userName, String reason) {
        executor.execute(() -> {
            try {
                // Configure email properties
                Properties props = getEmailProperties();

                // Create a session with sender credentials
                Session session = createEmailSession(props);

                // Create email message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_USERNAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
                message.setSubject("SecureHome: Account Registration Status");

                // Create message body
                String reasonText = (reason != null && !reason.isEmpty()) ? "<p>Reason: " + reason + "</p>" : "";

                String emailBody =
                        "<html><body>" +
                                "<h2>Account Registration Update</h2>" +
                                "<p>Dear " + userName + ",</p>" +
                                "<p>We regret to inform you that your registration request for SecureHome has not been approved at this time.</p>" +
                                reasonText +
                                "<p>If you believe this is in error or have any questions, please contact support.</p>" +
                                "<p>Best regards,<br>SecureHome Team</p>" +
                                "</body></html>";

                // Set up the HTML email part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(emailBody, "text/html; charset=utf-8");

                // Create a multipart message
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // Set content
                message.setContent(multipart);

                // Send the message
                Transport.send(message);

                Log.d(TAG, "Rejection notification email sent to: " + userEmail);
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send rejection notification email", e);
            }
        });
    }

    /**
     * Send an emergency alert notification email to admins
     * @param adminEmail Admin email address
     * @param userName Name of the user reporting the emergency
     * @param userEmail Email of the user reporting the emergency
     * @param alertTitle Title of the emergency alert
     * @param alertDescription Description of the emergency
     */
    public void sendEmergencyAlertToAdmin(String adminEmail, String userName, String userEmail, String alertTitle, String alertDescription) {
        // Input validation
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            Log.e(TAG, "Cannot send emergency alert: Admin email is null or empty");
            showToast("Error: Admin email not specified");
            return;
        }

        // Log the attempt and parameters
        Log.d(TAG, "Emergency alert email - Starting to send to: " + adminEmail);
        Log.d(TAG, "Emergency alert params - Title: " + alertTitle + ", From user: " + userName);

        executor.execute(() -> {
            try {
                // Configure email properties
                Properties props = getEmailProperties();
                Log.d(TAG, "Emergency alert - Email properties configured");

                // Create a session with sender credentials
                Session session = createEmailSession(props);
                Log.d(TAG, "Emergency alert - Email session created");

                // Create email message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_USERNAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminEmail));
                message.setSubject("SecureHome: EMERGENCY ALERT - " + alertTitle);
                Log.d(TAG, "Emergency alert - Message headers set");

                // Create message body with simpler HTML (in case that's causing issues)
                String emailBody =
                        "<html><body style='font-family: Arial, sans-serif;'>" +
                                "<h2 style='color: #D32F2F;'>EMERGENCY ALERT</h2>" +
                                "<p>An emergency alert has been triggered in the SecureHome system:</p>" +
                                "<hr>" +
                                "<p><b>User Reporting:</b> " + userName + "</p>" +
                                "<p><b>User Email:</b> " + userEmail + "</p>" +
                                "<hr>" +
                                "<p><b>Alert Title:</b> " + alertTitle + "</p>" +
                                "<p><b>Description:</b></p>" +
                                "<p style='padding: 10px; border: 1px solid #FFCDD2; background-color: #FFF3F3;'>"
                                + (alertDescription != null ? alertDescription.replace("\n", "<br>") : "No description provided") + "</p>" +
                                "<hr>" +
                                "<p>Please take appropriate action immediately.</p>" +
                                "<p>Regards,<br>SecureHome System</p>" +
                                "</body></html>";

                // Set up the HTML email part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(emailBody, "text/html; charset=utf-8");
                Log.d(TAG, "Emergency alert - Email body created");

                // Create a multipart message
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // Set content
                message.setContent(multipart);
                Log.d(TAG, "Emergency alert - Message content set");

                // Send the message
                Log.d(TAG, "Emergency alert - About to call Transport.send()");
                Transport.send(message);

                Log.d(TAG, "Emergency alert email sent successfully to admin: " + adminEmail + " regarding: " + alertTitle);
                showToast("Emergency alert email sent to administrator.");
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send emergency alert email: " + e.getMessage(), e);
                showToast("Failed to send emergency alert: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending emergency alert: " + e.getMessage(), e);
                showToast("Unexpected error sending alert: " + e.getMessage());
            }
        });
    }


    /**
     * Configure email server properties
     */
    private Properties getEmailProperties() {
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", EMAIL_HOST);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", EMAIL_PORT);
        props.put("mail.smtp.socketFactory.port", EMAIL_PORT); //SSL Port
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); //SSL Factory Class
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        // For TLS, you would use port 587 and different properties:
        // props.put("mail.smtp.starttls.enable", "true");
        // props.put("mail.smtp.port", "587"); // TLS Port
        // And remove the socketFactory properties if using TLS

        return props;
    }

    /**
     * Create email session with authentication
     */
    private Session createEmailSession(Properties props) {
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
            }
        });
    }

    /**
     * Show a toast on the UI thread
     */
    private void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}