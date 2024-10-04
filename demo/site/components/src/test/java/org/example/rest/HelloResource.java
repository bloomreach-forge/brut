package org.example.rest;

import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("/hello/")
public class HelloResource extends BaseRestResource {

    @GET
    @Path("/{user}")
    public Response hello(@Context HttpServletRequest request, @PathParam("user") String user) {
        return Response.ok().entity("Hello, World! " + user).build();
    }

    @GET
    @Path("/mount/{mountParamName}")
    public String mountParam(@PathParam("mountParamName") String mountParamName) {
        Mount mount = RequestContextProvider.get().getResolvedMount().getMount();
        String paramValue = mount.getParameter(mountParamName);
        return !Strings.isNullOrEmpty(paramValue) ? paramValue : "";
    }
}
