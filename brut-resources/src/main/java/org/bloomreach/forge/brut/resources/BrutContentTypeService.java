package org.bloomreach.forge.brut.resources;

import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypes;

import javax.jcr.RepositoryException;

//Content type service is not supported in requestcontext. Dynamic beans are not supported
public class BrutContentTypeService implements ContentTypeService {

    @Override
    public EffectiveNodeTypes getEffectiveNodeTypes() throws RepositoryException {
        return null;
    }

    @Override
    public ContentTypes getContentTypes() throws RepositoryException {
        return null;
    }
}