package io.jans.configapi.rest.resource;

import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.OXAUTH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = configurationService.find();
        return Response.ok(appConfiguration).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws Exception {
        Conf conf = configurationService.findConf();
        AppConfiguration appConfiguration = Jackson.applyPatch(requestString, conf.getDynamic());
        conf.setDynamic(appConfiguration);

        configurationService.merge(conf);
        return Response.ok(appConfiguration).build();
    }
}
