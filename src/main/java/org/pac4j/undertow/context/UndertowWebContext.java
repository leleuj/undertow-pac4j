package org.pac4j.undertow.context;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.util.CommonHelper;

/**
 * The webcontext implementation for Undertow.
 *
 * @author Jerome Leleu
 * @author Michael Remond
 * @author Igor Lobanov
 * @since 1.0.0
 */
public class UndertowWebContext implements WebContext {

    private final HttpServerExchange exchange;

    public UndertowWebContext(final HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    public HttpServerExchange getExchange() {
        return exchange;
    }

    @Override
    public Optional<String> getRequestParameter(final String name) {
        Deque<String> param = exchange.getQueryParameters().get(name);
        if (param != null) {
            return Optional.ofNullable(param.peek());
        } else {
            FormData data = exchange.getAttachment(FormDataParser.FORM_DATA);
            if (data != null && data.get(name) != null) {
                return Optional.ofNullable(data.get(name).peek().getValue());
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        Map<String, String[]> map = new HashMap<>();
        for (Entry<String, Deque<String>> entry : params.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));
        }
        FormData data = exchange.getAttachment(FormDataParser.FORM_DATA);
        if (data != null) {
            for (String key : data) {
                map.put(key, data.get(key).stream().map(f -> f.getValue()).collect(Collectors.toList()).toArray(new String[data.get(key).size()]));
            }
        }
        return map;
    }

    @Override
    public Optional<String> getRequestHeader(final String name) {
        return Optional.ofNullable(exchange.getRequestHeaders().get(name, 0));
    }

    @Override
    public String getRequestMethod() {
        return exchange.getRequestMethod().toString();
    }

    @Override
    public Optional<String> getResponseHeader(final String name) {
        return Optional.ofNullable(exchange.getResponseHeaders().getFirst(name));
    }

    @Override
    public void setResponseHeader(final String name, final String value) {
        exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    }

    @Override
    public String getServerName() {
        return exchange.getHostName();
    }

    @Override
    public int getServerPort() {
        return exchange.getHostPort();
    }

    @Override
    public String getScheme() {
        return exchange.getRequestScheme();
    }

    @Override
    public String getFullRequestURL() {
        String full = exchange.getRequestURL();
        if (CommonHelper.isNotBlank(exchange.getQueryString())) {
            full = full + "?" + exchange.getQueryString();
        }
        return full;
    }

    @Override
    public String getRemoteAddr() {
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    @Override
    public void addResponseCookie(final Cookie cookie) {
        final CookieImpl newCookie = new CookieImpl(cookie.getName(), cookie.getValue());
        newCookie.setComment(cookie.getComment());
        newCookie.setDomain(cookie.getDomain());
        newCookie.setPath(cookie.getPath());
        newCookie.setMaxAge(cookie.getMaxAge() < 0 ? null : cookie.getMaxAge());
        newCookie.setSecure(cookie.isSecure());
        newCookie.setHttpOnly(cookie.isHttpOnly());
        exchange.setResponseCookie(newCookie);
    }

    @Override
    public void setRequestAttribute(final String name, final Object value) {
        RequestAttributesMap.getOrInitialize(exchange).put(name, value);
    }

    @Override
    public String getPath() {
        return exchange.getRequestPath();
    }

    @Override
    public void setResponseContentType(final String content) {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, content);
    }

    @Override
    public Collection<Cookie> getRequestCookies() {
        final Collection<io.undertow.server.handlers.Cookie> uCookies = exchange.getRequestCookies().values();
        final List<Cookie> cookies = new ArrayList<>(uCookies.size());
        for (final io.undertow.server.handlers.Cookie uCookie : uCookies) {
            final Cookie cookie = new Cookie(uCookie.getName(), uCookie.getValue());
            cookie.setComment(uCookie.getComment());
            cookie.setDomain(uCookie.getDomain());
            cookie.setPath(uCookie.getPath());
            cookie.setMaxAge(uCookie.getMaxAge() == null ? -1 : uCookie.getMaxAge());
            cookie.setSecure(uCookie.isSecure());
            cookie.setHttpOnly(uCookie.isHttpOnly());
            cookies.add(cookie);
        }
        return cookies;
    }

    @Override
    public Optional<Object> getRequestAttribute(final String name) {
        return Optional.ofNullable(RequestAttributesMap.getOrInitialize(exchange).get(name));
    }

    @Override
    public boolean isSecure() {
        return exchange.isSecure();
    }
}
