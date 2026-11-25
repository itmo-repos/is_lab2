package ru.itmo.is.auth;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Locale;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    public record Credentials(String username, String password) {}

    @Inject
    AccountService accounts;

    @Context
    HttpServletRequest request;

    @GET
    @Path("/me")
    public Account me() {
        return AuthFilter.account(request);
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(Credentials credentials) {
        String username = username(credentials);
        if (!username.matches("[a-z0-9_.-]{3,32}")) {
            return error(400, "Логин должен содержать от 3 до 32 латинских букв, цифр или символов _ . -");
        }
        if (!validPassword(credentials)) {
            return error(400, "Пароль должен содержать от 8 до 128 символов");
        }
        if (!accounts.register(username, credentials.password())) {
            return error(409, "Логин уже занят");
        }
        return Response.status(201).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(Credentials credentials) {
        String username = username(credentials);
        if (!username.matches("[a-z0-9_.-]{3,32}") || !validPassword(credentials)) {
            return error(401, "Неверный логин или пароль");
        }

        Account account = accounts.authenticate(username, credentials.password());
        if (account == null) {
            return error(401, "Неверный логин или пароль");
        }
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        request.getSession(true).setAttribute(AuthFilter.ACCOUNT, account);
        return Response.ok(account).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Response.noContent().build();
    }

    private String username(Credentials credentials) {
        return credentials == null || credentials.username() == null
                ? "" : credentials.username().strip().toLowerCase(Locale.ROOT);
    }

    private boolean validPassword(Credentials credentials) {
        if (credentials == null || credentials.password() == null) {
            return false;
        }
        int length = credentials.password().length();
        return length >= 8 && length <= 128;
    }

    private Response error(int status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }
}
