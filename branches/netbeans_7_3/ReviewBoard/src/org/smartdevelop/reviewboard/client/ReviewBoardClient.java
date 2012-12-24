/*
 * Created on: Mar 4, 2012
 */
package org.smartdevelop.reviewboard.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.smartdevelop.reviewboard.client.response.*;
import org.smartdevelop.reviewboard.options.RBConfigurationPanel;

/**
 * Review Board API Client.
 *
 * @author Pragalathan M
 */
public class ReviewBoardClient {

    LogFactoryImpl factoryImpl;
    private DefaultHttpClient httpClient;
    private ObjectMapper mapper = new ObjectMapper();
    private Session session;
    boolean authenticated;
    public static final ReviewBoardClient INSTANCE = new ReviewBoardClient();
    private String baseUrl;

    private ReviewBoardClient() {
        httpClient = new DefaultHttpClient();
    }

    /**
     * Authenticates the user {@code username}.
     *
     * @param url the URL of the review board server.
     * @param username the username to authenticate.
     * @param password the password for the user.
     * @return true if the user is authenticated successfully, false otherwise.
     * @throws IOException
     * @throws ParseException
     */
    public boolean authenticate(String url, String username, String password) throws IOException, ParseException {
        baseUrl = url;
        HttpGet get = new HttpGet(url + "/api/session");
        String auth = Base64.encodeBase64URLSafeString((username + ":" + password).getBytes());
        get.setHeader("Authorization", "Basic " + auth);
        HttpResponse response = httpClient.execute(get);
        HttpEntity entity = response.getEntity();
        LoginResponse loginResponse = mapper.readValue(entity.getContent(), LoginResponse.class);
        session = loginResponse.getSession();
        authenticated = session.isAuthenticated();
        if (!authenticated) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(loginResponse.getFailedFields()));
        }
        return authenticated;
    }

    /**
     * Returns all the repository in the review board server.
     *
     * @return the map of repositoryName-{@code Repository}
     * @throws IOException
     */
    public Map<String, Repository> getRepositories() throws IOException, ParseException {
        if (baseUrl == null) {
            Preferences node = Preferences.userNodeForPackage(RBConfigurationPanel.class);
            baseUrl = node.get("reviewboard.url", null);
            if (baseUrl == null) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Please configure Review Board in Tools->Options->ReviewBoard before creating any reviews", NotifyDescriptor.ERROR_MESSAGE));
            }
        }
        if (!authenticated) {
            Preferences node = Preferences.userNodeForPackage(RBConfigurationPanel.class);
            authenticate(node.get("reviewboard.url", ""), node.get("username", ""), node.get("password", ""));
        }
        Map<String, Repository> map = new HashMap<String, Repository>();
        if (authenticated) {
            HttpGet get = new HttpGet(baseUrl + "/api/repositories/");
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            RepositoryResponse repositoryResponse = mapper.readValue(entity.getContent(), RepositoryResponse.class);
            for (Repository repository : repositoryResponse.getRepositories()) {
                map.put(repository.getPath(), repository);
            }
        }
        return map;
    }

    /**
     * Creates a new review request.
     *
     * @param repository the repository to which the new request belongs.
     * @param diffContent the file diff to upload
     * @param updateFields the fields to update in the review request
     * @return the {@link ReviewRequest} object representing the newly created.
     * request.
     * @throws IOException
     * @throws ParseException
     */
    public ReviewRequest createRequest(Repository repository, String diffContent, Map<String, String> updateFields) throws IOException, ParseException {
        ReviewRequest reviewRequest = createRequest(repository.getPath());
        if (reviewRequest == null) {
            return null;
        }
        boolean successful = uploadDiff(reviewRequest, diffContent);
        if (!successful) {
            return reviewRequest;
        }
        updateField(reviewRequest, updateFields);
        return reviewRequest;
    }

    /**
     * Creates a new review request.
     *
     * @param repositoryUrl the repository URL to the new request belongs.
     * @return the {@link ReviewRequest} object representing the newly created.
     * @throws IOException
     * @throws ParseException
     */
    public ReviewRequest createRequest(String repositoryUrl) throws IOException, ParseException {
        if (!authenticated) {
            Preferences node = Preferences.userNodeForPackage(RBConfigurationPanel.class);
            authenticate(node.get("reviewboard.url", ""), node.get("username", ""), node.get("password", ""));
        }
        if (authenticated) {
            HttpPost post = new HttpPost(baseUrl + "/api/review-requests/");
            List<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new BasicNameValuePair("repository", repositoryUrl));
            post.setEntity(new UrlEncodedFormEntity(data));
            HttpResponse response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            ReviewRequestResponse reviewRequestResponse = mapper.readValue(entity.getContent(), ReviewRequestResponse.class);
            return reviewRequestResponse.getReviewRequest();
        }

        return null;
    }

    /**
     * Updates the {@code reviewRequest}.
     *
     * @param reviewRequest the review request to be updated.
     * @param fields the fields of the {@code reviewRequest} to be updated.
     * @return true if the operation is successful, false otherwise
     * @throws IOException
     */
    public boolean updateField(ReviewRequest reviewRequest, Map<String, String> fields) throws IOException {
        HttpPut put = new HttpPut(reviewRequest.getLinks().get("draft").getHref());
        MultipartEntity mp = new MultipartEntity();
        for (String fieldName : fields.keySet()) {
            mp.addPart(fieldName, new StringBody(fields.get(fieldName), "text/plain", Charset.forName("UTF-8")));
        }
        put.setEntity(mp);
        put.setHeader(mp.getContentType());

        HttpResponse response = httpClient.execute(put);
        HttpEntity entity = response.getEntity();
        DraftResponse draftResponse = mapper.readValue(entity.getContent(), DraftResponse.class);
        if (!draftResponse.isSuccessful()) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(draftResponse.getFailedFields()));
        }
        return draftResponse.isSuccessful();
    }

    /**
     * Uploads the file diff content against the {@code reviewRequest}
     *
     * @param reviewRequest the review request to which diff should be uploaded.
     * @param diff the diff content
     * @return true if the operation is successful, false otherwise
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public boolean uploadDiff(ReviewRequest reviewRequest, String diff) throws UnsupportedEncodingException, IOException {
        HttpPost post = new HttpPost(reviewRequest.getLinks().get("diffs").getHref());
        MultipartEntity mp = new MultipartEntity();
        mp.addPart("basedir", new StringBody("/", "text/plain", Charset.forName("UTF-8")));
        FormBodyPart pathBodyPart = new FormBodyPart("path", new ByteArrayBody(diff.getBytes(Charset.forName("UTF-8")), "text/plain; charset=UTF-8", "diff"));
        mp.addPart(pathBodyPart);
        post.setEntity(mp);
        post.setHeader(mp.getContentType());

        HttpResponse response = httpClient.execute(post);
        HttpEntity entity = response.getEntity();
        UploadDiffResponse uploadDiffResponse = mapper.readValue(entity.getContent(), UploadDiffResponse.class);
        if (!uploadDiffResponse.isSuccessful()) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(uploadDiffResponse.getErr()));
        }
        return uploadDiffResponse.isSuccessful();
    }

    /**
     * Publishes a review request.
     *
     * @param reviewRequest the review request to be published.
     * @return true if the operation is successful, false otherwise
     * @throws IOException
     */
    public boolean publish(ReviewRequest reviewRequest) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        map.put("public", "true");
        return updateField(reviewRequest, map);
    }
}
