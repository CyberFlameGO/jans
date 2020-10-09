package io.jans.configapi.rest.resource;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;
import io.jans.orm.PersistenceEntryManager;
import io.jans.oxauth.service.common.ApplicationFactory;
import io.jans.service.cache.*;
import org.oxauth.persistence.model.configuration.GluuConfiguration;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.function.Function;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.CACHE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CacheConfigurationResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceManager;

    private CacheConfiguration loadCacheConfiguration() {
        return configurationService.findGluuConfiguration().getCacheConfiguration();
    }

    private CacheConfiguration mergeModifiedCache(Function<CacheConfiguration, CacheConfiguration> function) {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        final CacheConfiguration modifiedCache = function.apply(gluuConfiguration.getCacheConfiguration());
        gluuConfiguration.setCacheConfiguration(modifiedCache);

        persistenceManager.get().merge(gluuConfiguration);
        return modifiedCache;
    }

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getCacheConfiguration() {
        return Response.ok(loadCacheConfiguration()).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response patchCacheConfiguration(@NotNull String requestString) {
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            try {
                return Jackson.applyPatch(requestString, cache);
            } catch (IOException | JsonPatchException e) {
                throw new RuntimeException("Unable to apply patch.", e);
            }
        });
        return Response.ok(modifiedCache).build();
    }

    @GET
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getRedisConfiguration() {
        return Response.ok(loadCacheConfiguration().getRedisConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateRedisConfiguration(@NotNull RedisConfiguration redisConfiguration) {
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setRedisConfiguration(redisConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getRedisConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.IN_MEMORY)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getInMemoryConfiguration() {
        return Response.ok(loadCacheConfiguration().getInMemoryConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.IN_MEMORY)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateInMemoryConfiguration(@NotNull InMemoryConfiguration inMemoryConfiguration) {
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setInMemoryConfiguration(inMemoryConfiguration);
            return cache;
        });

        return Response.ok(modifiedCache.getInMemoryConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getNativePersistenceConfiguration() {
        return Response.ok(loadCacheConfiguration().getNativePersistenceConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateNativePersistenceConfiguration(@NotNull NativePersistenceConfiguration nativePersistenceConfiguration) {
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setNativePersistenceConfiguration(nativePersistenceConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getNativePersistenceConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.MEMCACHED)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getMemcachedConfiguration() {
        return Response.ok(loadCacheConfiguration().getMemcachedConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.MEMCACHED)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateMemcachedConfiguration(@NotNull MemcachedConfiguration memcachedConfiguration) {
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setMemcachedConfiguration(memcachedConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getMemcachedConfiguration()).build();
    }

}
