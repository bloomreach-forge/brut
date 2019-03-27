package org.bloomreach.forge.brut.components.mock;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.util.PathEncoder;

import static org.apache.commons.lang.CharEncoding.UTF_8;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hippoecm.hst.util.PathUtils.FULLY_QUALIFIED_URL_PREFIXES;

public class MockHstLink extends org.hippoecm.hst.mock.core.linking.MockHstLink {

    private static final String SLASH = "/";
    private final String baseUrl;


    public MockHstLink(String baseUrl) {
        this(baseUrl, EMPTY);
    }

    public MockHstLink(String baseUrl, String path) {
        this(baseUrl, path, null);
    }

    public MockHstLink(String baseUrl, String path, String subpath) {
        super(path);
        setSubPath(subpath);
        this.baseUrl = baseUrl.endsWith(SLASH) ? baseUrl : baseUrl + SLASH;
    }

    @Override
    public String toUrlForm(HstRequestContext requestContext, boolean fullyQualified) {
        String result = handleFullyQualifiedPathCase();
        if (result == null) {
            result = (fullyQualified ? baseUrl : SLASH) + getNormalizedPath();
            if (StringUtils.isNotBlank(getSubPath())) {
                result = result + "./" + normalize(getSubPath());
            }
        }
        return result;
    }

    private String getNormalizedPath() {
        return normalize(getPath() != null ? getPath() : EMPTY);
    }

    private String handleFullyQualifiedPathCase() {
        try {
            String result = null;
            for (String prefix : FULLY_QUALIFIED_URL_PREFIXES) {
                if (getPath() != null && getPath().startsWith(prefix)) {
                    result = PathEncoder.encode(getPath(), UTF_8, FULLY_QUALIFIED_URL_PREFIXES);
                }
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new HstComponentException(e);
        }
    }

    private String normalize(String path) {
        String result = path;
        if (StringUtils.isNotBlank(path)) {
            int endIndex = path.endsWith(SLASH) ? path.length() - 1 : path.length();
            int beginIndex = path.startsWith(SLASH) ? 1 : 0;
            result = path.substring(beginIndex, Math.max(beginIndex, endIndex));
        }
        return result;
    }
}
