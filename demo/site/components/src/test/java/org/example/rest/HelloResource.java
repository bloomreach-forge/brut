package org.example.rest;

import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
