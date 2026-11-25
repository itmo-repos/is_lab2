package ru.itmo.is.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(value = "/api/*", asyncSupported = true)
public class AuthFilter implements Filter {

    static final String ACCOUNT = Account.class.getName();

    public static Account account(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Account) session.getAttribute(ACCOUNT);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse result = (HttpServletResponse) response;
        result.setHeader("Cache-Control", "no-store");

        String method = http.getMethod();
        boolean read = method.equals("GET") || method.equals("HEAD");
        // A cross-origin form cannot send this header. The API does not allow CORS.
        if (!read && !"1".equals(http.getHeader("X-Lab-Request"))) {
            reject(result, 403, "Запрос отклонён");
            return;
        }

        String path = http.getRequestURI().substring(http.getContextPath().length());
        boolean publicPost = method.equals("POST")
                && (path.equals("/api/auth/login") || path.equals("/api/auth/register"));
        if (!publicPost && account(http) == null) {
            reject(result, 401, "Войдите в систему");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
