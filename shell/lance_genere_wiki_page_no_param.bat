set JAVA_HOME=C:\Java\jdk1.8.0_191
SET GITLAB_URL=https://mysgitlab.com
set PROJECT_ID=140009
set ACCESS_TOKEN=WetokenE7
set DIR_WITH_FILE=C:/result
set IMAGE_WIDTH=950
rem set PAGE_TITLE="load test 05_03_2023 13h25m12s"
%JAVA_HOME%\bin\java.exe -jar ../target/create-gitlab-wiki-page-for-files-in-directory-1.1-jar-with-dependencies.jar -gitlabUrl %GITLAB_URL% -projectId %PROJECT_ID% -accessToken %ACCESS_TOKEN% -dirWithFile %DIR_WITH_FILE% -imageWidth %IMAGE_WIDTH%
