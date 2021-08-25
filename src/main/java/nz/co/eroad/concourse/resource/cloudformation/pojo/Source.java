package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class Source {

    private final String name;
    private final Region region;
    private AwsCredentials credentials;
    private final List<String> notificationArns;

    @JsonCreator
    public Source(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "region", required = true) String region,
            @JsonProperty("access_key") String awsAccessKey,
            @JsonProperty("secret_key") String awsSecretKey,
            @JsonProperty("notification_arns") List<String> notificationArns,
            @JsonProperty("sts-role-arn") String assumeRoleArn) {
        this.name = name;
        this.region = parseRegion(region);


        if (awsAccessKey != null && awsSecretKey != null) {
            this.credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
        } else if ((awsAccessKey == null) != (awsSecretKey == null)) {
            throw new IllegalArgumentException("Both access_key and secret_key must be defined or not defined together!");
        } else {
            this.credentials = null;
        }

        AwsCredentialsProvider provider;
        if (credentials == null) {
            provider = DefaultCredentialsProvider.builder().build();
        } else {
            System.out.println("AWS secret is configured, hence create static chain out of it");
            provider = StaticCredentialsProvider.create(credentials);
        }

        if (!StringUtils.isEmpty(assumeRoleArn)) {

            System.out.printf("assume role is %s", assumeRoleArn);

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .durationSeconds(60 * 60) // 1 hour
                    .roleArn(assumeRoleArn)
                    .roleSessionName("ConcourseAssumeRoleSession")
                    .build();

            StsClient sts = StsClient.builder().credentialsProvider(provider)
                    .httpClientBuilder(UrlConnectionHttpClient.builder()).build();
            Credentials assumeRoleCredential = sts.assumeRole(assumeRoleRequest).credentials();

            System.out.printf("Assume role credential is %s, %s",
                    assumeRoleCredential.accessKeyId(),
                    assumeRoleCredential.secretAccessKey());

            //Quick and dirty solution, just override the credential
            this.credentials = AwsSessionCredentials.create(
                    assumeRoleCredential.accessKeyId(),
                    assumeRoleCredential.secretAccessKey(),
                    assumeRoleCredential.sessionToken());
        }

        this.notificationArns = notificationArns == null ? Collections.emptyList() : notificationArns;
    }

    public String getName() {
        return name;
    }

    public Region getRegion() {
        return region;
    }

    public AwsCredentials getCredentials() {
        return credentials;
    }

    public List<String> getNotificationArns() {
        return notificationArns;
    }

    private static Region parseRegion(String region) {
        Optional<Region> any = Region.regions().stream()
                .filter(validRegion -> validRegion.id().equals(region)).findAny();
        if (any.isEmpty()) {
            throw new IllegalArgumentException("Could not find region with id " + region);
        }
        return any.get();
    }

}
