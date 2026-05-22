package com.github.b3kt.presentation.rest.pazaauto;

import java.time.LocalDateTime;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbSpkService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@RequestScoped
@Path("/api/pazaauto/spk")
@RequiredArgsConstructor
public class TbSpkResource extends AbstractCrudResource<TbSpkEntity, Long> {

    final TbSpkService service;

    @Override
    protected AbstractCrudService<TbSpkEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "SPK";
    }

    @GET
    @Path("/by-no-spk/{noSpk}")
    @WithSpan("find-spk-by-no-spk")
    public Response findByNoSpk(@PathParam("noSpk") @SpanAttribute("spk.no-spk") String noSpk) {
        return Response.ok(ApiResponse.success(service.findByNoSpk(noSpk))).build();
    }

    @GET
    @Path("/unprocessed")
    @WithSpan("get-unprocessed-spk")
    public Response getUnprocessedSpk() {
        return Response.ok(ApiResponse.success(service.findUnprocessedSpk())).build();
    }

    @Override
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbSpkEntity entity = getService().findById(parseId(id));
        return Response.ok(ApiResponse.success(entity)).build();
    }

    @GET
    @Path("/get-next-spk-number")
    @WithSpan("get-next-spk-number")
    public Response getNextSpk() {
        String nextSpkNumber = service.generateNextSpkNumber(SPK_DATE_FORMATTER.format(LocalDateTime.now()));
        return Response.ok(ApiResponse.success(nextSpkNumber)).build();
    }

    @POST
    @Override
    public Response create(TbSpkEntity entity) {
        service.enrich(entity);
        TbSpkEntity created = getService().create(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " created", created)).build();
    }

    @Override
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, TbSpkEntity entity) {
        service.enrich(entity);
        TbSpkEntity updated = getService().update(parseId(id), entity);

        if (updated == null) {
            return Response.ok(ApiResponse.error(getEntityName() + " not found")).build();
        }
        return Response.ok(ApiResponse.success(getEntityName() + " updated", updated)).build();
    }

    @Override
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        TbSpkEntity cancelled = service.cancelSpk(parseId(id));
        if (cancelled == null) {
            return Response.ok(ApiResponse.error(getEntityName() + " not found")).build();
        }
        return Response.ok(ApiResponse.success(getEntityName() + " cancelled", cancelled)).build();
    }

    @DELETE
    @Path("/delete-by-no-spk/{noSpk}")
    public Response deleteByNoSpk(@PathParam("noSpk") String noSpk) {
        service.deleteByNoSpk(noSpk);
        return Response.ok(ApiResponse.success(getEntityName() + " deleted permanently")).build();
    }
}
