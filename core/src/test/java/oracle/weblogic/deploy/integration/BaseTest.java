// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.weblogic.deploy.integration;

import oracle.weblogic.deploy.integration.utils.ExecCommand;
import oracle.weblogic.deploy.integration.utils.ExecResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class BaseTest {

    protected static final Logger logger = Logger.getLogger(ITWdt.class.getName());
    protected static final String PS = File.pathSeparator;
    protected static final String FS = File.separator;
    private static final String SAMPLE_ARCHIVE_FILE = "archive.zip";
    private static final String WDT_ZIPFILE = "weblogic-deploy.zip";
    private static final String WDT_HOME_DIR = "weblogic-deploy";
    protected static final String SAMPLE_MODEL_FILE_PREFIX = "simple-topology";
    protected static final String SAMPLE_VARIABLE_FILE = "domain.properties";
    private static int maxIterations = 50;
    private static int waitTime = 5;
    private static String projectRoot = "";
    protected static String mwhome_12213;
    protected static String fmwhome_12213;
    protected static String createDomainScript = "";
    protected static String discoverDomainScript = "";
    protected static String updateDomainScript = "";
    protected static String deployAppScript = "";
    protected static String encryptModelScript = "";
    protected static String validateModelScript = "";
    protected static String domainParent12213 = "";
    protected static String fmwDomainParent12213 = "";
    protected static final String ORACLE_DB_IMG = "phx.ocir.io/weblogick8s/database/enterprise";
    protected static final String ORACLE_DB_IMG_TAG = "12.2.0.1-slim";
    private static final String DB_CONTAINER_NAME = "InfraDB";
    private static final String OCIR_SERVER = "phx.ocir.io";

    protected static void initialize() throws Exception {

        logger.info("Initializing the tests ...");
        projectRoot = System.getProperty("user.dir");
        logger.info("DEBUG: projectRoot=" + projectRoot);

        mwhome_12213 = System.getProperty("MW_HOME");
        fmwhome_12213 = System.getProperty("FMW_HOME");

        createDomainScript = getWDTScriptsHome() + FS + "createDomain.sh";
        discoverDomainScript = getWDTScriptsHome() + FS + "discoverDomain.sh";
        updateDomainScript = getWDTScriptsHome() + FS + "updateDomain.sh";
        deployAppScript = getWDTScriptsHome() + FS + "deployApps.sh";
        encryptModelScript = getWDTScriptsHome() + FS + "encryptModel.sh";
        validateModelScript = getWDTScriptsHome() + FS + "validateModel.sh";
        domainParent12213 = mwhome_12213 + FS + "user_projects" + FS + "domains";
        fmwDomainParent12213 = fmwhome_12213 + FS + "user_projects" + FS + "domains";
    }

    protected static void setup() throws Exception {

        logger.info("Setting up the test environment ...");
        logger.info("building WDT sample archive file");
        buildSampleArchive();

        // unzip weblogic-deploy-tooling/installer/target/weblogic-deploy.zip
        String cmd = "/bin/unzip -o " + getInstallerTargetDir() + FS + WDT_ZIPFILE;
        executeNoVerify(cmd);

    }

    protected static void cleanup() throws Exception {
        logger.info("cleaning up the test environment ...");

        // remove WDT script home directory
        String cmd = "/bin/rm -rf " + getProjectRoot() + FS + WDT_HOME_DIR;
        executeNoVerify(cmd);
    }

    protected static String getProjectRoot() {
        return projectRoot;
    }

    protected static void pullOracleDBDockerImage() throws Exception {
        logger.info("Pulling Oracle DB image from OCR ...");

        String ocir_user = System.getProperty("OCIR_USER");
        String ocir_pass = System.getProperty("OCIR_PASS");
        logger.info("DEBUG: ocir_user=" + ocir_user);
        logger.info("DEBUG: ocir_pass=" + ocir_pass);
        if(ocir_user == null || ocir_pass == null) {
            throw new Exception("Please set -Docir_user and -Docir_pass in mvn.config to pull DB " +
                    "image " + ORACLE_DB_IMG + ":" + ORACLE_DB_IMG_TAG);
        }

        pullDockerImage(OCIR_SERVER, ocir_user, ocir_pass, ORACLE_DB_IMG, ORACLE_DB_IMG_TAG);
    }

    private static void pullDockerImage(String repoServer, String username, String password,
                                        String imagename, String imagetag) throws Exception {

        String cmd = "docker login " + repoServer + " -u " + username + " -p " + password;
        logger.info("executing command: " + cmd);
        ExecCommand.exec(cmd, true);
        cmd = "docker pull " + imagename + ":" + imagetag;
        logger.info("executing command: " + cmd);
        ExecCommand.exec(cmd, true);

        // verify the docker image is pulled
        ExecResult result = ExecCommand.exec("docker images | grep " + imagename  + " | grep " +
                imagetag + "| wc -l");
        String resultString = result.stdout();
        if(Integer.parseInt(resultString.trim()) != 1) {
            throw new Exception("docker image " + imagename + ":" + imagetag + " is not pulled as expected."
                    + " Expected 1 image, found " + resultString);
        }
    }

    protected static String getWDTScriptsHome() {
        return getProjectRoot() + FS + WDT_HOME_DIR + FS + "bin";
    }

    protected static void executeNoVerify(String command) throws Exception {
        logger.info("executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        logger.info("DEBUG: result.stderr=" + result.stderr());
        logger.info("DEBUG: result.stdout=" + result.stdout());
    }

    protected void verifyResult(ExecResult result, String matchString) throws Exception {
        if(result.exitValue() != 0 || !result.stdout().contains(matchString)) {
            logger.info("DEBUG: result.stdout=" + result.stdout());
            throw new Exception("result stdout does not contains the expected string: " + matchString);
        }
    }

    protected static void verifyExitValue(ExecResult result, String command) throws Exception {
        if(result.exitValue() != 0) {
            logger.info(result.stderr());
            throw new Exception("executing the following command failed: " + command);
        }
    }

    protected void verifyErrorMsg(ExecResult result, String errorMsg) throws Exception {
        if(result.exitValue() == 0 || !result.stderr().contains(errorMsg)) {
            logger.info("DEBUG: result stderr: " + result.stderr());
            throw new Exception("test result does not contains expected error msg: " + errorMsg);
        }
    }

    protected void verifyModelFile(String modelFile) throws Exception {
        String cmd = "/bin/ls " + modelFile + " | wc -l";
        logger.info("executing command: " + cmd);
        ExecResult result = ExecCommand.exec(cmd);
        if(Integer.parseInt(result.stdout().trim()) != 1) {
            throw new Exception("no model file is not created as expected");
        }
    }

    protected void logTestBegin(String testMethodName) throws Exception {
        logger.info("=======================================");
        logger.info("BEGIN test " + testMethodName + " ...");
    }

    protected void logTestEnd(String testMethodName) throws Exception {
        logger.info("SUCCESS - " + testMethodName);
        logger.info("=======================================");
    }

    protected static String getResourcePath() {
        return getProjectRoot() + FS + "src" + FS + "test" + FS + "resources";
    }

    protected static ExecResult buildSampleArchive() throws Exception {
        logger.info("Building WDT archive ...");
        String command = "sh " + getResourcePath() + FS + "build-archive.sh";
        return executeAndVerify(command, true);
    }

    protected static String getSampleArchiveFile() throws Exception {
        return getResourcePath() + FS + SAMPLE_ARCHIVE_FILE;
    }

    protected static String getSampleModelFile(String suffix) throws Exception {
        return getResourcePath() + FS + SAMPLE_MODEL_FILE_PREFIX + suffix + ".yaml";
    }

    protected static String getInstallerTargetDir() throws Exception {
        return getProjectRoot() + FS + ".." + FS + "installer" + FS + "target";
    }

    protected static String getSampleVariableFile() throws Exception {
        return getResourcePath() + FS + SAMPLE_VARIABLE_FILE;
    }

    protected static void createDBContainer() throws Exception {
        logger.info("Creating an Oracle db docker container ...");
        String command = "docker rm -f " + DB_CONTAINER_NAME;
        ExecCommand.exec(command);
        command = "docker run -d --name " + DB_CONTAINER_NAME + " -p 1521:1521 -p 5500:5500 --env=\"DB_PDB=InfraPDB1\"" +
                " --env=\"DB_DOMAIN=us.oracle.com\" --env=\"DB_BUNDLE=basic\" " + ORACLE_DB_IMG + ":" +
                ORACLE_DB_IMG_TAG;
        ExecCommand.exec(command);

        // wait for the db is ready
        command = "docker ps | grep " + DB_CONTAINER_NAME;
        checkCmdInLoop(command, "healthy");
    }

    protected static void replaceStringInFile(String filename, String originalString, String newString)
            throws Exception {
        Path path = Paths.get(filename);

        String content = new String(Files.readAllBytes(path));
        content = content.replaceAll(originalString, newString);
        Files.write(path, content.getBytes());
    }

    private static ExecResult executeAndVerify(String command, boolean isRedirectToOut) throws Exception {
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command, isRedirectToOut);
        verifyExitValue(result, command);
        logger.info(result.stdout());
        return result;
    }

    private static void checkCmdInLoop(String cmd, String matchStr)
            throws Exception {
        int i = 0;
        while (i < maxIterations) {
            ExecResult result = ExecCommand.exec(cmd);

            // pod might not have been created or if created loop till condition
            if (result.exitValue() != 0
                    || (result.exitValue() == 0 && !result.stdout().contains(matchStr))) {
                logger.info("Output for " + cmd + "\n" + result.stdout() + "\n " + result.stderr());
                // check for last iteration
                if (i == (maxIterations - 1)) {
                    throw new RuntimeException(
                            "FAILURE: " + cmd + " does not return the expected string " + matchStr + ", exiting!");
                }
                logger.info(
                        "Waiting for the expected String " + matchStr
                                + ": Ite ["
                                + i
                                + "/"
                                + maxIterations
                                + "], sleeping "
                                + waitTime
                                + " seconds more");

                Thread.sleep(waitTime * 1000);
                i++;
            } else {
                logger.info("get the expected String " + matchStr);
                break;
            }
        }
    }
}
