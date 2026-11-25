package ru.itmo.is.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import ru.itmo.is.auth.AuthFilter;
import ru.itmo.is.dto.ImportOperationDto;
import ru.itmo.is.service.ImportHistoryService;

import java.util.List;

@Path("/imports")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class ImportHistoryResource {

    @Inject
    ImportHistoryService service;

    @GET
    public List<ImportOperationDto> list(@Context HttpServletRequest request) {
        return service.list(AuthFilter.account(request));
    }
}
