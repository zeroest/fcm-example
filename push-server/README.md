# FCM

## Reference

[Code Reference](https://github.com/imaginalis/spring-boot-fcm-push-notifications)

[[Doc] FCM Server](https://firebase.google.com/docs/cloud-messaging/server)

[[Doc] FCM message spec](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)

[[MVN] Firebase Admin](https://mvnrepository.com/artifact/com.google.firebase/firebase-admin)

## Token Logic

[oauth playground](https://developers.google.com/oauthplayground/)

FcmInitializer

```kotlin
FirebaseOptions.builder()
    .setCredentials(GoogleCredentials.fromStream(ClassPathResource(serviceAccountPath!!).inputStream))
    .build()

this.credentialsSupplier = Suppliers
    .ofInstance(checkNotNull(credentials).createScoped(FIREBASE_SCOPES));
```

FcmService

```kotlin
FirebaseMessaging
    .getInstance()
    .sendAsync(message)
    .get()
```

FirebaseMessaging

```java
private CallableOperation < String, FirebaseMessagingException > sendOp(
    final Message message, final boolean dryRun
) {
    checkNotNull(message, "message must not be null");
    final FirebaseMessagingClient messagingClient = getMessagingClient();
    return new CallableOperation < String, FirebaseMessagingException>() {
    @Override
    protected String execute() throws FirebaseMessagingException {
        return messagingClient.send(message, dryRun);
    }
};
}
```

FirebaseMessagingClientImpl

```java
  private String sendSingleRequest(
      Message message, boolean dryRun) throws FirebaseMessagingException {
    HttpRequestInfo request =
        HttpRequestInfo.buildJsonPostRequest(
            fcmSendUrl, message.wrapForTransport(dryRun))
            .addAllHeaders(COMMON_HEADERS);
    MessagingServiceResponse parsed = httpClient.sendAndParse(
        request, MessagingServiceResponse.class);
    return parsed.getMessageId();
  }
```

ErrorHandlingHttpClient

```java
  public <V> V sendAndParse(HttpRequestInfo requestInfo, Class<V> responseType) throws T {
    IncomingHttpResponse response = send(requestInfo);
    return parse(response, responseType);
  }
  
```

```java
  public IncomingHttpResponse send(HttpRequestInfo requestInfo) throws T {
    HttpRequest request = createHttpRequest(requestInfo);

    HttpResponse response = null;
    try {
      response = request.execute();
      // Read and buffer the content. Otherwise if a parse error occurs later,
      // we lose the content stream.
      String content = null;
      InputStream stream = response.getContent();
      if (stream != null) {
        // Stream is null when the response body is empty (e.g. 204 No Content responses).
        content = CharStreams.toString(new InputStreamReader(stream, response.getContentCharset()));
      }

      return new IncomingHttpResponse(response, content);
    } catch (HttpResponseException e) {
      throw errorHandler.handleHttpResponseException(e, new IncomingHttpResponse(e, request));
    } catch (IOException e) {
      throw errorHandler.handleIOException(e);
    } finally {
      ApiClientUtils.disconnectQuietly(response);
    }
  }
```

```java
  private HttpRequest createHttpRequest(HttpRequestInfo requestInfo) throws T {
    try {
      return requestInfo.newHttpRequest(requestFactory, jsonFactory)
          .setResponseInterceptor(interceptor);
    } catch (IOException e) {
      // Handle request initialization errors (credential loading and other config errors)
      throw errorHandler.handleIOException(e);
    }
  }
```

HttpRequestInfo

```java
  HttpRequest newHttpRequest(
      HttpRequestFactory factory, JsonFactory jsonFactory) throws IOException {
    HttpRequest request;
    HttpContent httpContent = getContent(jsonFactory);
    if (factory.getTransport().supportsMethod(method)) {
      request = factory.buildRequest(method, url, httpContent);
    } else {
      // Some HttpTransport implementations (notably NetHttpTransport) don't support new methods
      // like PATCH. We try to emulate such requests over POST by setting the method override
      // header, which is recognized by most Google backend APIs.
      request = factory.buildPostRequest(url, httpContent);
      request.getHeaders().set("X-HTTP-Method-Override", method);
    }

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      request.getHeaders().set(entry.getKey(), entry.getValue());
    }

    return request;
  }
```

HttpRequestFactory

```java
  public HttpRequest buildRequest(String requestMethod, GenericUrl url, HttpContent content)
      throws IOException {
    HttpRequest request = transport.buildRequest();
    if (url != null) {
      request.setUrl(url);
    }
    if (initializer != null) {
      initializer.initialize(request);
    }
    request.setRequestMethod(requestMethod);
    if (content != null) {
      request.setContent(content);
    }
    return request;
  }
```

FirebaseRequestInitializer

```java
public final class FirebaseRequestInitializer implements HttpRequestInitializer {

  private final List<HttpRequestInitializer> initializers;

  public FirebaseRequestInitializer(FirebaseApp app) {
    this(app, null);
  }

  public FirebaseRequestInitializer(FirebaseApp app, @Nullable RetryConfig retryConfig) {
    ImmutableList.Builder<HttpRequestInitializer> initializers =
        ImmutableList.<HttpRequestInitializer>builder()
            .add(new HttpCredentialsAdapter(ImplFirebaseTrampolines.getCredentials(app)))
            .add(new TimeoutInitializer(app.getOptions()));
    if (retryConfig != null) {
      initializers.add(new RetryInitializer(retryConfig));
    }
    this.initializers = initializers.build();
  }

  @Override
  public void initialize(HttpRequest request) throws IOException {
    for (HttpRequestInitializer initializer : initializers) {
      initializer.initialize(request);
    }
  }

  private static class TimeoutInitializer implements HttpRequestInitializer {

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    TimeoutInitializer(FirebaseOptions options) {
      this.connectTimeoutMillis = options.getConnectTimeout();
      this.readTimeoutMillis = options.getReadTimeout();
    }

    @Override
    public void initialize(HttpRequest request) {
      request.setConnectTimeout(connectTimeoutMillis);
      request.setReadTimeout(readTimeoutMillis);
    }
  }
}
```

HttpCredentialsAdapter

```java
  @Override
  public void initialize(HttpRequest request) throws IOException {
    request.setUnsuccessfulResponseHandler(this);

    if (!credentials.hasRequestMetadata()) {
      return;
    }
    HttpHeaders requestHeaders = request.getHeaders();
    URI uri = null;
    if (request.getUrl() != null) {
      uri = request.getUrl().toURI();
    }
    Map<String, List<String>> credentialHeaders = credentials.getRequestMetadata(uri);
    if (credentialHeaders == null) {
      return;
    }
    for (Map.Entry<String, List<String>> entry : credentialHeaders.entrySet()) {
      String headerName = entry.getKey();
      List<String> requestValues = new ArrayList<>();
      requestValues.addAll(entry.getValue());
      requestHeaders.put(headerName, requestValues);
    }
  }
```

ServiceAccountCredentials

```java
  @Override
  public Map<String, List<String>> getRequestMetadata(URI uri) throws IOException {
    if (scopes.isEmpty() && defaultScopes.isEmpty() && uri == null) {
      throw new IOException(
          "Scopes and uri are not configured for service account. Either pass uri"
              + " to getRequestMetadata to use self signed JWT, or specify the scopes"
              + " by calling createScoped or passing scopes to constructor.");
    }
    if (jwtCredentials != null && uri != null) {
      return jwtCredentials.getRequestMetadata(uri);
    } else {
      return super.getRequestMetadata(uri);
    }
  }
```

OAuth2Credentials

```java
  @Override
  public Map<String, List<String>> getRequestMetadata(URI uri) throws IOException {
    return unwrapDirectFuture(asyncFetch(MoreExecutors.directExecutor())).requestMetadata;
  }
```

```java
  private ListenableFuture<OAuthValue> asyncFetch(Executor executor) {
    AsyncRefreshResult refreshResult = null;

    // fast and common path: skip the lock if the token is fresh
    // The inherent race condition here is a non-issue: even if the value gets replaced after the
    // state check, the new token will still be fresh.
    if (getState() == CacheState.FRESH) {
      return Futures.immediateFuture(value);
    }

    // Schedule a refresh as necessary
    synchronized (lock) {
      if (getState() != CacheState.FRESH) {
        refreshResult = getOrCreateRefreshTask();
      }
    }
    // Execute the refresh if necessary. This should be done outside of the lock to avoid blocking
    // metadata requests during a stale refresh.
    if (refreshResult != null) {
      refreshResult.executeIfNew(executor);
    }

    synchronized (lock) {
      // Immediately resolve the token token if its not expired, or wait for the refresh task to
      // complete
      if (getState() != CacheState.EXPIRED) {
        return Futures.immediateFuture(value);
      } else if (refreshResult != null) {
        return refreshResult.task;
      } else {
        // Should never happen
        return Futures.immediateFailedFuture(
            new IllegalStateException("Credentials expired, but there is no task to refresh"));
      }
    }
  }
```

```java
  private AsyncRefreshResult getOrCreateRefreshTask() {
    synchronized (lock) {
      if (refreshTask != null) {
        return new AsyncRefreshResult(refreshTask, false);
      }

      final ListenableFutureTask<OAuthValue> task =
          ListenableFutureTask.create(
              new Callable<OAuthValue>() {
                @Override
                public OAuthValue call() throws Exception {
                  return OAuthValue.create(refreshAccessToken(), getAdditionalHeaders());
                }
              });

      task.addListener(
          new Runnable() {
            @Override
            public void run() {
              finishRefreshAsync(task);
            }
          },
          MoreExecutors.directExecutor());

      refreshTask = task;

      return new AsyncRefreshResult(refreshTask, true);
    }
  }
```

ServiceAccountCredentials

```java
  @Override
  public AccessToken refreshAccessToken() throws IOException {
    JsonFactory jsonFactory = OAuth2Utils.JSON_FACTORY;
    long currentTime = clock.currentTimeMillis();
    String assertion = createAssertion(jsonFactory, currentTime, tokenServerUri.toString());

    GenericData tokenRequest = new GenericData();
    tokenRequest.set("grant_type", GRANT_TYPE);
    tokenRequest.set("assertion", assertion);
    UrlEncodedContent content = new UrlEncodedContent(tokenRequest);

    HttpRequestFactory requestFactory = transportFactory.create().createRequestFactory();
    HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(tokenServerUri), content);
    request.setParser(new JsonObjectParser(jsonFactory));

    request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(new ExponentialBackOff()));
    request.setUnsuccessfulResponseHandler(
        new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff())
            .setBackOffRequired(
                new BackOffRequired() {
                  public boolean isRequired(HttpResponse response) {
                    int code = response.getStatusCode();
                    return (
                    // Server error --- includes timeout errors, which use 500 instead of 408
                    code / 100 == 5
                        // Forbidden error --- for historical reasons, used for rate_limit_exceeded
                        // errors instead of 429, but there currently seems no robust automatic way
                        // to
                        // distinguish these cases: see
                        // https://github.com/google/google-api-java-client/issues/662
                        || code == 403);
                  }
                }));

    HttpResponse response;
    try {
      response = request.execute();
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Error getting access token for service account: %s, iss: %s",
              e.getMessage(), getIssuer()),
          e);
    }

    GenericData responseData = response.parseAs(GenericData.class);
    String accessToken =
        OAuth2Utils.validateString(responseData, "access_token", PARSE_ERROR_PREFIX);
    int expiresInSeconds =
        OAuth2Utils.validateInt32(responseData, "expires_in", PARSE_ERROR_PREFIX);
    long expiresAtMilliseconds = clock.currentTimeMillis() + expiresInSeconds * 1000L;
    return new AccessToken(accessToken, new Date(expiresAtMilliseconds));
  }
```
