package com.example.servlet;

import java.io.IOException;

import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.MyRed5ProPlugin;
import com.red5pro.plugin.Red5ProPlugin;
import com.red5pro.server.stream.Red5ProConnManager;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Base servlet for commonality between WHIP and WHEP.
 *
 * @author Paul Gregoire
 */
public abstract class MyServlet extends HttpServlet {

    private static final long serialVersionUID = 4384924788536621579L;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // plugin reference
    protected static MyRed5ProPlugin plugin;

    // connection manager reference
    protected static Red5ProConnManager connectionManager = (Red5ProConnManager) Red5ProConnManager.getInstance();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // won't even get here if the plugin doesnt exist unless somehow it doesnt start
        plugin = ((MyRed5ProPlugin) PluginRegistry.getPlugin(MyRed5ProPlugin.NAME));
        // look up <context-param> entries for configuring credentials
        ServletContext context = config.getServletContext();
        if (context != null) {
            // do context param lookups here
        }
        logger.debug("init completed - config: {} context: {}", config, context);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.trace("service <<");
        if (logger.isDebugEnabled()) {
            debugDump(request, response);
        }
        // ensure the server is ready
        if (Red5ProPlugin.isReady()) {
            // service your requests here
            logger.trace("service >>");
            super.service(request, response);
        } else {
            logger.debug("Red5 Pro isn't ready yet");
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Service not ready"); // 412
        }
    }

    /**
     * Debugging info for request/response.
     *
     * @param request
     * @param response
     */
    protected void debugDump(HttpServletRequest request, HttpServletResponse response) {
        if (logger.isDebugEnabled()) {
            logger.debug("==========>>>DUMP START<<<==========");
            logger.debug("Protocol: {} request id: {}", request.getProtocol(), request.getProtocolRequestId());
            logger.debug("Request URI: {} URL: {}", request.getRequestURI(), request.getRequestURL());
            logger.debug("Request id: {} requested session id: {}", request.getRequestId(), request.getRequestedSessionId());
            logger.debug("Method: {}", request.getMethod());
            logger.debug("Context type: {} path: {} servlet path: {}", request.getContentType(), request.getContextPath(),
                    request.getServletPath());
            logger.debug("Character encoding: {}", request.getCharacterEncoding());
            logger.debug("Local address: {} name: {} port: {}", request.getLocalAddr(), request.getLocalName(), request.getLocalPort());
            logger.debug("Remote address: {} host: {} port: {}", request.getRemoteAddr(), request.getRemoteHost(), request.getRemotePort());
            logger.debug("Remote user: {}", request.getRemoteUser());
            logger.debug("Server name: {} port: {}", request.getServerName(), request.getServerPort());
            logger.debug("Path info: {}", request.getPathInfo());
            logger.debug("Path translated: {}", request.getPathTranslated());
            logger.debug("Query string: {}", request.getQueryString());
            logger.debug("Auth type: {}", request.getAuthType());
            logger.debug("User principal: {}", request.getUserPrincipal());
            // seems to be per browser without a forced refresh
            logger.debug("Session id: {}", request.getSession().getId());
            logger.debug("Session created: {}", request.getSession().getCreationTime());
            logger.debug("Session last accessed: {}", request.getSession().getLastAccessedTime());
            logger.debug("Session max inactive: {}", request.getSession().getMaxInactiveInterval());
            // new session detection doesnt appear to work with Firefox
            logger.debug("Session is new: {}", request.getSession().isNew());
            // we're not using session attributes att, but we could
            request.getSession().getAttributeNames().asIterator().forEachRemaining(attrName -> {
                logger.debug("Session attribute {}: {}", attrName, request.getSession().getAttribute(attrName));
            });
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    logger.debug("Cookie: {} value: {}", cookie.getName(), cookie.getValue());
                    cookie.getAttributes().forEach((key, value) -> {
                        logger.debug("Cookie attribute: {} value: {}", key, value);
                    });
                }
            }
            // dump the headers at trace level to keep the debug cleaner
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                logger.trace("Header {}: {}", headerName, request.getHeader(headerName));
            });
            request.getParameterNames().asIterator().forEachRemaining(paramName -> {
                logger.trace("Parameter {}: {}", paramName, request.getParameter(paramName));
            });
            request.getAttributeNames().asIterator().forEachRemaining(attrName -> {
                logger.trace("Attribute {}: {}", attrName, request.getAttribute(attrName));
            });
            logger.debug("==========>>>DUMP END<<<==========");
        }
    }

}
