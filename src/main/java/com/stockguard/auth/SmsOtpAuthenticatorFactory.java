package com.stockguard.auth;


import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.Config;
import java.util.List;

public class SmsOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "sms-otp-authenticator";

    // Configuration property keys
    private static final String CONFIG_API_KEY = "apiKey";
    private static final String CONFIG_SENDER = "sender";
    private static final String CONFIG_TTL_SECONDS = "ttlSeconds";
    private static final String CONFIG_CODE_LENGTH = "codeLength";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "SMS OTP (Kavenegar)";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public String getHelpText() {
        return "Validates a one-time password sent via SMS using Kavenegar API.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SmsOtpAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // Initialize global configuration if needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post-initialization logic if needed
    }

    @Override
    public void close() {
        // Cleanup resources if needed
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_API_KEY)
                .label("Kavenegar API Key")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Enter your Kavenegar API key from panel.kavenegar.com")
                .add()
                .property()
                .name(CONFIG_SENDER)
                .label("SMS Sender")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Sender line number (e.g., 10004346 or your custom line)")
                .defaultValue("10004346")
                .add()
                .property()
                .name(CONFIG_TTL_SECONDS)
                .label("OTP TTL (seconds)")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("How long the OTP code remains valid")
                .defaultValue("120")
                .add()
                .property()
                .name(CONFIG_CODE_LENGTH)
                .label("OTP Code Length")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Number of digits in the OTP code")
                .defaultValue("6")
                .add()
                .build();
    }
}