package io.netty.handler.codec.http;

/**
 * An HTTP request.
 *
 * <h3>Accessing Query Parameters and Cookie</h3>
 * <p>
 * Unlike the Servlet API, a query string is constructed and decomposed by
 * {@link QueryStringEncoder} and {@link QueryStringDecoder}.
 *
 * {@link io.netty.handler.codec.http.cookie.Cookie} support is also provided
 * separately via {@link io.netty.handler.codec.http.cookie.ServerCookieDecoder},
 * {@link io.netty.handler.codec.http.cookie.ClientCookieDecoder},
 * {@link io.netty.handler.codec.http.cookie.ServerCookieEncoder},
 * and {@link io.netty.handler.codec.http.cookie.ClientCookieEncoder}.
 *
 * @see HttpResponse
 * @see io.netty.handler.codec.http.cookie.ServerCookieDecoder
 * @see io.netty.handler.codec.http.cookie.ClientCookieDecoder
 * @see io.netty.handler.codec.http.cookie.ServerCookieEncoder
 * @see io.netty.handler.codec.http.cookie.ClientCookieEncoder
 */
public interface HttpRequest extends HttpMessage {

    /**
     * @deprecated Use {@link #method()} instead.
     */
    @Deprecated
    HttpMethod getMethod();

    /**
     * Returns the {@link HttpMethod} of this {@link HttpRequest}.
     *
     * @return The {@link HttpMethod} of this {@link HttpRequest}
     */
    HttpMethod method();

    /**
     * Set the {@link HttpMethod} of this {@link HttpRequest}.
     */
    HttpRequest setMethod(HttpMethod method);

    /**
     * @deprecated Use {@link #uri()} instead.
     */
    @Deprecated
    String getUri();

    /**
     * Returns the requested URI (or alternatively, path)
     *
     * @return The URI being requested
     */
    String uri();

    /**
     *  Set the requested URI (or alternatively, path)
     */
    HttpRequest setUri(String uri);

    @Override
    HttpRequest setProtocolVersion(HttpVersion version);
}
