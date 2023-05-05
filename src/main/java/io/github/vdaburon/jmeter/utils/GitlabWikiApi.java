package io.github.vdaburon.jmeter.utils;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.WikiAttachment;
import org.gitlab4j.api.models.WikiPage;

import java.io.File;
import java.util.List;

public class GitlabWikiApi {

    private String gitLabUrl;
    private String projectId;
    private String accessToken;

    private GitLabApi gitLabApi = null;

    public GitlabWikiApi(String gitLabUrl, String projectId, String accessToken) {
        this.gitLabUrl = gitLabUrl;
        this.projectId = projectId;
        this.accessToken = accessToken;
    }

    public GitLabApi initConnexion() {
        // Create a GitLabApi instance to communicate with your GitLab server
        gitLabApi = new GitLabApi(gitLabUrl, accessToken);
        return gitLabApi;
    }

    public GitLabApi getGitLabApi() {
        if (gitLabApi == null) {
            gitLabApi = initConnexion();
        }
        return gitLabApi;
    }
    public WikiPage createWikiPage(String titlePage, String content) throws GitLabApiException {
        WikiPage newWikiPage = gitLabApi.getWikisApi().createPage(projectId, titlePage,content);
        return newWikiPage;
    }

    public WikiPage updateWikiPage(String slug, String titlePage, String content) throws GitLabApiException {
        WikiPage newWikiPage = gitLabApi.getWikisApi().updatePage(projectId, slug,titlePage, content);
        return newWikiPage;
    }
    public List<WikiPage> getWikiPagesFromProject(boolean withContent) throws GitLabApiException {
        // Get a list of pages in project wiki
        List<WikiPage> wikiPages = gitLabApi.getWikisApi().getPages(projectId, withContent);
        return wikiPages;
    }

    public void deleteWikiPage(String slug) throws GitLabApiException {
        gitLabApi.getWikisApi().deletePage(projectId, slug);
    }

    public WikiAttachment uploadAttachment(File fileToUpload) throws GitLabApiException {
        WikiAttachment attach = gitLabApi.getWikisApi().uploadAttachment(projectId, fileToUpload);
        return attach;
    }

    public List<Project> getListProjectsByUserId(String userId) throws GitLabApiException {
        ProjectFilter filter = new ProjectFilter();
        List<Project> projects = gitLabApi.getProjectApi().getUserProjects(userId,filter);
        return projects;
    }

}
