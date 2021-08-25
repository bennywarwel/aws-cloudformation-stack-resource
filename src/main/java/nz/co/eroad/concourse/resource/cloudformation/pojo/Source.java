package nz.co.eroad.concourse.resource.cloudformation.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
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
    private AwsBasicCredentials credentials;
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

        if (credentials != null && !StringUtils.isEmpty(assumeRoleArn)) {
            System.out.printf("Config credential is %s, %s", credentials.accessKeyId(), credentials.secretAccessKey());
            System.out.printf("assume role is %s", assumeRoleArn);

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .durationSeconds(12 * 60 * 60) // 12 hours
                    .roleArn(assumeRoleArn)
                    .roleSessionName(String.format("%s-%s", assumeRoleArn, Instant.now()))
                    .build();

            StsClient sts = StsClient.builder().credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .httpClientBuilder(UrlConnectionHttpClient.builder()).build();
            Credentials assumeRoleCredential = sts.assumeRole(assumeRoleRequest).credentials();

            System.out.printf("Assume role credential is %s, %s",
                    assumeRoleCredential.accessKeyId(),
                    assumeRoleCredential.secretAccessKey());

            //Quick and dirty solution, just override the credential
            this.credentials = AwsBasicCredentials.create(
                    assumeRoleCredential.accessKeyId(),
                    assumeRoleCredential.secretAccessKey());
        }

        this.notificationArns = notificationArns == null ? Collections.emptyList() : notificationArns;
    }

    public String getName() {
        return name;
    }

    public Region getRegion() {
        return region;
    }

    public AwsBasicCredentials getCredentials() {
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
