package org.bloomreach.forge.brut.resources;

import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        //utility
    }

    public static ModuleDefinition loadAddonModule(String addonModuleresourcePath) {
        try {
            URL addonUrl = Utils.class.getClassLoader().getResource(addonModuleresourcePath);
            if (addonUrl == null) {
                throw new IOException("Error while loading the pagemodel addon module");
            }
            return loadModuleDefinition(addonUrl);
        } catch (IOException | JAXBException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static ModuleDefinition loadModuleDefinition(URL url) throws JAXBException, IOException {
        ModuleDefinition moduleDefinition;

        JAXBContext jc = JAXBContext.newInstance(ModuleDefinition.class);
        Unmarshaller um = jc.createUnmarshaller();

        moduleDefinition = (ModuleDefinition) um.unmarshal(url.openStream());
        return moduleDefinition;
    }
}
