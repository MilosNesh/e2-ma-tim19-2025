package com.example.habitgame.model;

import com.google.common.collect.Lists;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccessToken {
    private static final String firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken() {
        try {
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"habitgame-3b883\",\n" +
                    "  \"private_key_id\": \"9cc0bb684aec4d2675ccaea347bd3a649adb8b82\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCfcWdmcl78ydo6\\nuv7KvLIyfgY9OdV2Q3pPB/O6w7q+1tdvp0P4roHFgbU9iFGFHEIcUcEJdSyPcY4O\\n2RfZIS7PzSI/RpnLWoG78Q44/z42UrVNW8p4uinYtGZCdbioMxJzemloD/HYUgFy\\nZUMN0Qw2cJRFf/7BuHvGL3h6qr8JL4B9T0v9cz1Yh3tGKQG0VETyU+rNqZdK0EuK\\nLk5PydQHHP3CKLE24BnNclLrXvtCtSO/ahNZkWVmPhoEaELH825pPhtSCDQhytBT\\ncpVsCaRPiFL5pkqAjimBVHYFMSIP+OpJrkZHGQ4E3+RzdvoL24weL8l2of7+mmju\\n9fA9u/ORAgMBAAECggEAC0kA7TG+6qREWZgS8OlUcQm1uhdp9hAOXAHndMKGCRwm\\nfTkA8Pz0AzVcLUXk5Dek78FzGO7l16TNTdx8iUIloLBnokQmXkvyhwmHrM3XGsv1\\nmGs87Fjb3aJeIwnXRzNvNMR3KbYIM3wRbWYArh2L4F4NRpKJxu8edp91RvRnJ1iM\\nWYeWC3J1PNgzV4CD+g04iNU87tEnb8KQ6YLkWXv5LTscp4vY5kHjnlALkEHGqOLG\\n4OGi3zIKtp5h0Z6f5nA8oa4Dv5zjo8uuT3jyfNjdrdy7RsoFu2Ohd33P4KL4oPzE\\nrsaC8JuzJDXy1lJNBMRf2f8+ej+bMlQDSIx6BNP0aQKBgQDV6LRduFKeyxD1EMjy\\n0shTXesyWn6Z7aJZkpQJ1e/cVL6p5WtCA5CaD1MmyzzdnOCNE3mlxDPDoDgY7Jcy\\n8IhDYjg22jMbCYzvSpMCJiyLRrTbV7AY1oaendrN9QezSNVIDinyUnZTCVmtq0SO\\nnSolsIa8HINm3GWwvzNcXzQMqQKBgQC+0RFqu4/3Xn5tOXJFhyvJSfjTDD4zvI8v\\naKIT1XtBN7WUmMutEzdVSpa+4+kVM60Lhm5CHifnXxrct3XOYJGmx2//L6wiTtOP\\nFa09CbrS+Tnx+KIJb7roNoMUj2hsRqbQn4Wj/lBHRTlR0QbEa6gWXQsTYb6m0URW\\nsTRr4cnYqQKBgQCWEc4IOjpsgRaCd6YjBVHZevh4uRg8sYTGxpjpVZgTwbiJxRQ8\\nmfemvb2eZ9NpvpejIdikPUP7qLnycnmZGTrf21aI1QgpncesUWtVhKaXh6F2iMxY\\ngJitSx2ohJFEbIKFLLqs8MFmfBbYSmGslAJvsqqz6mUdkt4c4+cKlRgVEQKBgBgG\\nBFZWtOJk8C/aKqvcP8QA3yXbBb3HwJMsbjNnDjSAygiiUApDQBUp5d60nPTBY8ju\\nrNUALO/xRlWBd4B7IftIYq5TFy3elL4P8Zacsfu7yvLO5b+gZHy12DzvbSssnfXo\\nD5A9BWhjq7rEQUDbePcP0cQn6zPfZuYvOzVV4O65AoGAXFfORDNURezglJWfe3tG\\nK4vaBxOke3SSaYe5pwtQon4Lf0jf4Fh0OiDO4jf3sYeSBdc2QI8lgqxx2K8KLInc\\nNwrPwrnKoX4cOZ0FZuDwgJsXNeLFKulvtXkAndvmgMfNs/GTGwmBDrYvjmpUC7N6\\n3k3MS6Swm6OZm4xGZwywGTw=\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-fbsvc@habitgame-3b883.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"112059502435600924486\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40habitgame-3b883.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";

            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).
                    createScoped(Lists.newArrayList(firebaseMessagingScope));

            googleCredentials.refresh();

            return googleCredentials.getAccessToken().getTokenValue();
        }catch (IOException e){
            return null;
        }
    }

}
