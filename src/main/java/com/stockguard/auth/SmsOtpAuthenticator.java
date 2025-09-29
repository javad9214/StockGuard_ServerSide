package com.stockguard.auth;



import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.kavenegar.sdk.KavenegarApi;
import com.kavenegar.sdk.excepctions.ApiException;
import com.kavenegar.sdk.excepctions.HttpException;
import jakarta.ws.rs.core.Response;


import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Map;

public class SmsOtpAuthenticator implements Authenticator {
    private static final String NOTE_HASH = "otp_hash";
    private static final String NOTE_DEADLINE = "otp_deadline";
    private static final String NOTE_SENT = "otp_sent_at";

    private final SecureRandom rnd = new SecureRandom();

    private String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            Formatter fmt = new Formatter();
            for (byte b : dig) fmt.format("%02x", b);
            String hex = fmt.toString();
            fmt.close();
            return hex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateCode(int length) {
        int min = (int) Math.pow(10, length-1);
        int max = (int) Math.pow(10, length) - 1;
        int code = min + rnd.nextInt(max - min + 1);
        return String.valueOf(code);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String alreadySent = authSession.getAuthNote(NOTE_SENT);

        // Get config (factory should provide default keys like apiKey, sender, ttlSeconds, codeLength)
        AuthenticatorConfigModel cfg = context.getAuthenticatorConfig();
        Map<String, String> cfgMap = cfg != null ? cfg.getConfig() : null;
        String apiKey = cfgMap != null ? cfgMap.get("apiKey") : null;
        String sender = cfgMap != null ? cfgMap.get("sender") : "";
        int ttlSeconds = cfgMap != null && cfgMap.get("ttlSeconds") != null ? Integer.parseInt(cfgMap.get("ttlSeconds")) : 120;
        int codeLength = cfgMap != null && cfgMap.get("codeLength") != null ? Integer.parseInt(cfgMap.get("codeLength")) : 6;

        UserModel user = context.getUser();
        if (user == null) {
            // This authenticator requires a user (set requiresUser=true in factory)
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        // phone attribute name — adapt if you store user phone under a different attribute
        String phone = user.getFirstAttribute("phone");
        if (phone == null || phone.isEmpty()) {
            // no number -> show a helpful error or route to a required action to add phone
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, context.form()
                    .setError("You don't have a phone number configured.")
                    .createForm("otp-no-phone.ftl"));
            return;
        }

        // If not yet sent (or expired), generate + send
        if (alreadySent == null) {
            String code = generateCode(codeLength);
            String hashed = sha256Hex(code);
            long deadline = System.currentTimeMillis() + ttlSeconds * 1000L;

            // store hashed + expiry in auth session notes
            authSession.setAuthNote(NOTE_HASH, hashed);
            authSession.setAuthNote(NOTE_DEADLINE, Long.toString(deadline));
            authSession.setAuthNote(NOTE_SENT, Long.toString(System.currentTimeMillis()));

            // send via Kavenegar (SDK usage from README) - adapt for actual SDK methods if needed
            try {
                KavenegarApi api = new KavenegarApi(apiKey);
                // Compose message; you may want to use templates/configurable message.
                String message = String.format("Your verification code is: %s", code);
                api.send(sender, phone, message); // method per SDK README
            } catch (HttpException | ApiException e) {
                context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                        context.form().setError("Failed to send SMS: " + e.getMessage()).createForm("otp-error.ftl"));
                return;
            } catch (Exception e) {
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
                return;
            }
        }

        // render the OTP input form
        LoginFormsProvider form = context.form();
        // mask phone for display
        String mask = phone.length() > 6 ? phone.substring(0, 3) + "*****" + phone.substring(phone.length()-2) : phone;
        form.setAttribute("phone_mask", mask);
        Response challenge = form.createForm("otp-form.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String provided = context.getHttpRequest().getDecodedFormParameters().getFirst("otp");
        String storedHash = authSession.getAuthNote(NOTE_HASH);
        String deadlineStr = authSession.getAuthNote(NOTE_DEADLINE);

        if (provided == null || storedHash == null || deadlineStr == null) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setError("Invalid or expired code").createForm("otp-form.ftl"));
            return;
        }

        long deadline = Long.parseLong(deadlineStr);
        if (System.currentTimeMillis() > deadline) {
            // expired -> clear notes and resend next authenticate
            authSession.removeAuthNote(NOTE_HASH);
            authSession.removeAuthNote(NOTE_DEADLINE);
            authSession.removeAuthNote(NOTE_SENT);
            context.attempted(); // restart step: next authenticate will re-send
            return;
        }

        String providedHash = sha256Hex(provided);
        if (providedHash.equals(storedHash)) {
            // OK
            // clear notes
            authSession.removeAuthNote(NOTE_HASH);
            authSession.removeAuthNote(NOTE_DEADLINE);
            authSession.removeAuthNote(NOTE_SENT);
            context.success();
        } else {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setError("Wrong code").createForm("otp-form.ftl"));
        }
    }

    @Override public boolean requiresUser() { return true; }
    @Override public boolean configuredFor(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) {
        // you might check if user has phone attribute set
        return user != null && user.getFirstAttribute("phone") != null;
    }
    @Override public void setRequiredActions(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) { }
    @Override public void close() { }
}

