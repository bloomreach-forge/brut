package client.packagename.rest;

import client.packagename.model.ListItemPagination;
import client.packagename.model.NewsDocument;
import client.packagename.model.NewsItemRep;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;
import org.onehippo.cms7.essentials.components.rest.ctx.DefaultRestContext;
import org.onehippo.cms7.essentials.components.rest.ctx.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;


@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Path("/news/")
public class NewsResource extends BaseRestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsResource.class);

    @GET
    @Path("/")
    public Pageable<NewsItemRep> allNews(@Context HttpServletRequest request) {
        return findListItems(new DefaultRestContext(this, request));
    }


    private Pageable<NewsItemRep> findListItems(RestContext context) {
        ListItemPagination<NewsItemRep> pageable = new ListItemPagination<>();
        HstQuery query = createQuery(context, NewsDocument.class, Subtypes.EXCLUDE);
        try {
            HstQueryResult result = query.execute();

            pageable.setTotal(result.getTotalSize());
            pageable.setPageSize(context.getPageSize());
            pageable.setPageNumber(context.getPage());

            HippoBeanIterator it = result.getHippoBeans();
            it.forEachRemaining(hippoBean -> pageable.addItem(new NewsItemRep().represent((NewsDocument) hippoBean)));
        } catch (QueryException e) {
            LOGGER.warn(e.getLocalizedMessage());
        }
        return pageable;
    }
}
