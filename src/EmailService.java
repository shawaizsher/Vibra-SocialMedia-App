import com.sendgrid.*;

import java.io.IOException;

public class EmailService {

    private static final String API_KEY = "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";

    public static void sendVerificationEmail(String toEmail, String verificationCode) {
        Email from = new Email("vibra.socialmedia.app@gmail.com");
        String subject = "Your Verification Code";

        Email to = new Email(toEmail);

        String contentText = "Your verification code is: " + verificationCode +
                "\n\nPlease enter this code in the app to complete registration.";

        Content content = new Content("text/plain", contentText);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(API_KEY);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println("Email sent successfully: " + response.getStatusCode());
        } catch (IOException ex) {
            System.out.println("Error sending email: " + ex.getMessage());
        }
    }

    public static void sendAccountDetailsEmail(String toEmail, String emailContent) throws IOException {
        Email from = new Email("vibra.socialmedia.app@gmail.com");
        String subject = "Your VIBRA Account Details";

        Email to = new Email(toEmail);

        Content content = new Content("text/plain", emailContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(API_KEY);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);

        System.out.println("Account details email sent successfully: " + response.getStatusCode());
    }

}
