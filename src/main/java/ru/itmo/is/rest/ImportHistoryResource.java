package ru.itmo.is.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.itmo.is.auth.Account;
import ru.itmo.is.auth.AuthFilter;
import ru.itmo.is.dto.ImportOperationDto;
import ru.itmo.is.exception.ConflictException;
import ru.itmo.is.exception.FieldException;
import ru.itmo.is.importfile.ImportFileReader;
import ru.itmo.is.service.ImportHistoryService;
import ru.itmo.is.service.ImportService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/imports")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class ImportHistoryResource {

    private static final Logger LOG = Logger.getLogger(ImportHistoryResource.class.getName());

    @Inject
    ImportHistoryService service;

    @Inject
    ImportService importer;

    @GET
    public List<ImportOperationDto> list(@Context HttpServletRequest request) {
        return service.list(AuthFilter.account(request));
    }

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response upload(InputStream file, @Context HttpServletRequest request) {
        Account account = AuthFilter.account(request);
        if (account == null) {
            throw new WebApplicationException(401);
        }
        try {
            ImportOperationDto result = importer.save(account, ImportFileReader.read(file));
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (FieldException | ConflictException e) {
            service.recordFailure(account);
            throw e;
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Не удалось выполнить импорт", e);
            service.recordFailure(account);
            return Response.serverError().entity(Map.of("message", "Не удалось выполнить импорт")).build();
        }
    }
}
