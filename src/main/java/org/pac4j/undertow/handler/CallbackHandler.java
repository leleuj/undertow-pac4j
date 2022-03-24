package org.pac4j.undertow.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.FindBest;
import org.pac4j.undertow.context.UndertowContextFactory;
import org.pac4j.undertow.context.UndertowSessionStore;
import org.pac4j.undertow.http.UndertowHttpActionAdapter;

/**
 * <p>This filter finishes the login process for an indirect client.</p>
 *
 * @author Jerome Leleu
 * @author Michaël Remond
 * @since 1.0.0
 */
public class CallbackHandler implements HttpHandler {

    private CallbackLogic callbackLogic;

    private Config config;

    private String defaultUrl;

    private Boolean renewSession;

    private String defaultClient;

    protected CallbackHandler(final Config config, final String defaultUrl)  {
        this.config = config;
        this.defaultUrl = defaultUrl;
    }

    public static HttpHandler build(final Config config) {
        return build(config, null);
    }

    public static HttpHandler build(final Config config, final String defaultUrl) {
        return build(config, defaultUrl, null);
    }

    public static HttpHandler build(final Config config, final String defaultUrl, final CallbackLogic callbackLogic) {
        final FormParserFactory factory = FormParserFactory.builder().addParser(new FormEncodedDataDefinition()).build();
        final EagerFormParsingHandler formHandler = new EagerFormParsingHandler(factory);
        final CallbackHandler callbackHandler = new CallbackHandler(config, defaultUrl);
        if (callbackLogic != null) {
            callbackHandler.setCallbackLogic(callbackLogic);
        }
        formHandler.setNext(callbackHandler);
        return new BlockingHandler(formHandler);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        final SessionStore bestSessionStore = FindBest.sessionStore(null, config, new UndertowSessionStore(exchange));
        final HttpActionAdapter bestAdapter = FindBest.httpActionAdapter(null, config, UndertowHttpActionAdapter.INSTANCE);
        final CallbackLogic bestLogic = FindBest.callbackLogic(callbackLogic, config, DefaultCallbackLogic.INSTANCE);

        final WebContext context = FindBest.webContextFactory(null, config, UndertowContextFactory.INSTANCE).newContext(exchange);
        bestLogic.perform(context, bestSessionStore, config, bestAdapter, this.defaultUrl, this.renewSession, this.defaultClient);
    }

    protected CallbackLogic getCallbackLogic() {
        return callbackLogic;
    }

    protected void setCallbackLogic(final CallbackLogic callbackLogic) {
        this.callbackLogic = callbackLogic;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(final String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    public Boolean getRenewSession() {
        return renewSession;
    }

    public void setRenewSession(final Boolean renewSession) {
        this.renewSession = renewSession;
    }

    public String getDefaultClient() {
        return defaultClient;
    }

    public void setDefaultClient(final String defaultClient) {
        this.defaultClient = defaultClient;
    }
}
