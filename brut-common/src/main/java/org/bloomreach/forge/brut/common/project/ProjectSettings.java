package org.bloomreach.forge.brut.common.project;

public class ProjectSettings {

    private final String hstRoot;
    private final String projectNamespace;
    private final String selectedBeansPackage;
    private final String selectedRestPackage;
    private final String selectedComponentsPackage;
    private final String selectedProjectPackage;
    private final String repositoryDataModule;
    private final String applicationSubModule;
    private final String developmentSubModule;
    private final String siteModule;
    private final String webfilesSubModule;

    public ProjectSettings(String hstRoot,
                           String projectNamespace,
                           String selectedBeansPackage,
                           String selectedRestPackage,
                           String selectedComponentsPackage,
                           String selectedProjectPackage,
                           String repositoryDataModule,
                           String applicationSubModule,
                           String developmentSubModule,
                           String siteModule,
                           String webfilesSubModule) {
        this.hstRoot = hstRoot;
        this.projectNamespace = projectNamespace;
        this.selectedBeansPackage = selectedBeansPackage;
        this.selectedRestPackage = selectedRestPackage;
        this.selectedComponentsPackage = selectedComponentsPackage;
        this.selectedProjectPackage = selectedProjectPackage;
        this.repositoryDataModule = repositoryDataModule;
        this.applicationSubModule = applicationSubModule;
        this.developmentSubModule = developmentSubModule;
        this.siteModule = siteModule;
        this.webfilesSubModule = webfilesSubModule;
    }

    public String getHstRoot() {
        return hstRoot;
    }

    public String getProjectNamespace() {
        return projectNamespace;
    }

    public String getSelectedBeansPackage() {
        return selectedBeansPackage;
    }

    public String getSelectedRestPackage() {
        return selectedRestPackage;
    }

    public String getSelectedComponentsPackage() {
        return selectedComponentsPackage;
    }

    public String getSelectedProjectPackage() {
        return selectedProjectPackage;
    }

    public String getRepositoryDataModule() {
        return repositoryDataModule;
    }

    public String getApplicationSubModule() {
        return applicationSubModule;
    }

    public String getDevelopmentSubModule() {
        return developmentSubModule;
    }

    public String getSiteModule() {
        return siteModule;
    }

    public String getWebfilesSubModule() {
        return webfilesSubModule;
    }
}
