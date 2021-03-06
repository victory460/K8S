package com.wenge.tbase.cicd.jenkins.template;


import com.wenge.tbase.cicd.entity.CicdRepos;
import com.wenge.tbase.cicd.entity.param.ImageBuildParam;
import com.wenge.tbase.cicd.entity.param.ImageUploadParam;
import com.wenge.tbase.cicd.entity.param.PackageCommonParam;
import com.wenge.tbase.cicd.entity.param.PackageParam;
import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: JenkinsTemplate
 * @Description: JenkinsTemplate
 * @Author: Wang XingPeng
 * @Date: 2020/12/2 10:54
 */
public class JenkinsTemplate {

    /**
     * 获取模板xml
     *
     * @param script
     * @param token
     * @return
     */
    public static String getJenkinsTemplateXml(String script, String token) {
        String xml = "<flow-definition plugin=\"workflow-job@2.40\">\n" +
                "  <actions>\n" +
                "    <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin=\"pipeline-model-definition@1.7.2\"/>\n" +
                "    <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin=\"pipeline-model-definition@1.7.2\">\n" +
                "      <jobProperties/>\n" +
                "      <triggers/>\n" +
                "      <parameters/>\n" +
                "      <options/>\n" +
                "    </org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction>\n" +
                "  </actions>\n" +
                "  <description></description>\n" +
                "  <keepDependencies>false</keepDependencies>\n" +
                "  <properties>\n" +
                "    <com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty plugin=\"gitlab-plugin@1.5.13\">\n" +
                "      <gitLabConnection></gitLabConnection>\n" +
                "    </com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty>\n" +
                "  </properties>\n" +
                "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps@2.86\">\n" +
                "    <script>" + script + "</script>\n" +
                "    <sandbox>true</sandbox>\n" +
                "  </definition>\n" +
                "  <triggers/>\n" +
                "  <authToken>" + token + "</authToken>\n" +
                "  <disabled>false</disabled>\n" +
                "</flow-definition>";
        return xml;
    }

    /**
     * 获取创建模块xml
     *
     * @param description
     * @param token
     * @return
     */
    public static String getBulidJenkinsTemplateXml(String description, String token) {
        String xml = "<flow-definition plugin=\"workflow-job@2.32\">\n" +
                "<description>" + description + "</description>\n" +
                "<definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps@2.85\">\n" +
                "<script> node(){echo 'hello wenge!'} </script>\n" +
                "<sandbox>true</sandbox>\n" +
                "</definition>\n" +
                "<authToken>" + token + "</authToken>" +
                "</flow-definition>";
        return xml;
    }

    /**
     * 获取代码检出阶段内容
     *
     * @param branch
     * @param url
     * @param auth
     * @return
     */
    public static String getCodePullStage(String branch, String url, String auth) {
        String stage = "\tstage('拉取代码') {\n" +
                "\t\tcheckout([$class: 'GitSCM', branches: [[name: '*/" + branch + "']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '" + auth + "', url: '" + url + "']]])\n" +
                "\t} \n";
        return stage;
    }

    /**
     * 获取代码检测阶段内容
     *
     * @param address
     * @return
     */
    public static String getCodeCheckStage(String address) {
        String stage = "\tstage('检查代码') {\n" +
                "\t\tdef scannerHome = tool 'sonar-scanner'\n" +
                "\t\twithSonarQubeEnv('sonar') {\n" +
                "\t\t\tsh \"\"\"\n" +
                "\t\t\t\tcd " + address + "\n" +
                "\t\t\t\t${scannerHome}/bin/sonar-scanner\n" +
                "\t\t\t\t\"\"\"\n" +
                "\t\t\t\techo \"完成代码审查\"\n" +
                "\t\t}\n" +
                "\t}\n";
        return stage;
    }

    /**
     * 获取编译打包阶段内容
     *
     * @param param
     * @return
     */
    public static String getPackageCommonStage(PackageCommonParam param) {
        //后端
        String stage = "\tstage('" + param.getStageName() + "') {\n" +
                "\t\tsh \"mvn -f " + param.getProjectName() + " clean install\"\n" +
                "\t}\n";
        //前端
        return stage;
    }

    /**
     * 获取编译打包阶段内容
     *
     * @param param
     * @return
     */
    public static String getPackageStage(PackageParam param) {
        String stage = null;
        //后端
        if (param.getPackageType() == 1) {
            stage = "\tstage('" + param.getStageName() + "') {\n" +
                    "\t\tsh \"mvn -f " + param.getProjectName() + " clean package\"\n" +
                    "\t}\n";
        }
        //前端
        return stage;
    }

    /**
     * 获取镜像构建阶段内容
     *
     * @param param
     * @return
     */
    public static String getImageBuildStage(ImageBuildParam param) {
        StringBuffer stage = new StringBuffer();
        stage.append("\tstage('" + param.getStageName() + "') {\n")
                .append("\t\tsh \"docker build --build-arg JAR_FILE=")
                .append(param.getProjectName())
                .append(" -t ").append(param.getImageName()).append(":").append(param.getImageTag());
        if (StringUtils.isNotEmpty(param.getDockerfileAddress())) {
            stage.append(" -f ").append(param.getDockerfileAddress());
        } else {
            stage.append(" -f ").append("${env.WORKSPACE}/").append(param.getProjectName()).append("/Dockerfile");
        }
        stage.append(" .\"\n");
        stage.append("\t}\n");
        return stage.toString();
    }

    /**
     * 获取镜像上传阶段内容
     *
     * @param repos
     * @param param
     * @return
     */
    public static String getImageUploadStage(CicdRepos repos, ImageUploadParam param, String harborUrl) {
        StringBuffer stage = new StringBuffer();
        String imageName = param.getImageName() + ":" + param.getImageTag();
        String harborImageName = harborUrl + "/" + repos.getProjectName() + "/" + imageName;
        stage.append("\tstage('" + param.getStageName() + "') {\n");
        stage.append("\t\tsh \"docker tag " + imageName + " " + harborUrl + "/" + repos.getProjectName() + "/" + imageName + " \"\n");
        stage.append("\t\twithCredentials([usernamePassword(credentialsId: \"" + repos.getCredentialId() + "\", passwordVariable: 'password', usernameVariable: 'username')]) {\n" +
                "\t\t\tsh \"docker login -u ${username} -p ${password} " + harborUrl + "\"\n" +
                "\t\t\tsh \"docker push " + harborImageName + "\"\n" +
                "\t\t}\n");
        stage.append("\t\tsh \"docker rmi -f " + imageName + "\"\n");
        stage.append("\t\tsh \"docker rmi -f " + harborImageName + "\"\n");
        stage.append("\t}\n");
        return stage.toString();
    }
}
