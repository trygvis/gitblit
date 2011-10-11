/*
 * Copyright 2011 Trygve Laugstøl <trygvis@inamo.no>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.crowd;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.xpath.*;

import com.gitblit.Keys;
import org.eclipse.jgit.util.Base64;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * TODO: Read the configuration properties on each request.
 */
public class CrowdClient {
    private final Logger logger;
    private final String url;
    private final String applicationUsername;
    private final String applicationPassword;
    private final String group;
    private final XPathExpression xPathExpression;

    public CrowdClient(Logger logger, String url, String applicationUsername, String applicationPassword, String group) throws IOException, XPathExpressionException {
        this.logger = logger;
        this.url = url;
        this.applicationUsername = applicationUsername;
        this.applicationPassword = applicationPassword;
        this.group = group;

        if(url == null || applicationUsername == null || applicationPassword == null || group == null) {
            throw new IOException("Missing required properties: " + Keys.crowd.url + ", " + Keys.crowd.applicationUsername + ", " + Keys.crowd.applicationPassword + ", " + Keys.crowd.group);
        }

        XPath xPath = XPathFactory.newInstance().newXPath();
        xPathExpression = xPath.compile("//*[@name]");
    }

    public boolean authenticate(String username, char[] password) {
        try {
            HttpURLConnection c = openConnection("/rest/usermanagement/1/authentication?username=" + username);
            c.setRequestProperty("Content-Type", "application/xml");
            c.setDoOutput(true);
            OutputStream os = c.getOutputStream();
            os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><password><value>".getBytes("utf-8"));
            // TODO: This is bad and defeats the purpose with using char arrays for containing passwords.
            os.write(new String(password).getBytes("utf-8"));
            os.write("</value></password>".getBytes("utf-8"));
            os.flush();
            connect(c);
            int responseCode = c.getResponseCode();

            return responseCode == 200;
        }
        catch(Exception e) {
            logger.info("Error while accessing crowd", e);
            return false;
        }
    }

    public List<String> allUsersInGroup() {
        logger.info("allUsersInGroup");
        try {
            HttpURLConnection c = openConnection("/rest/usermanagement/1/group/user/direct?groupname=" + URLEncoder.encode(group, "utf-8"));
            c.setRequestProperty("Content-Type", "application/xml");
            c.setDoOutput(false);
            c.setDoInput(true);
            connect(c);
            int responseCode = c.getResponseCode();

            if(responseCode != 200) {
                logger.info("Crowd returned " + responseCode);
                return Collections.emptyList();
            }

            NodeList users = (NodeList) xPathExpression.evaluate(new InputSource(c.getInputStream()), XPathConstants.NODESET);

            List<String> list = new ArrayList<String>(users.getLength());
            for(int i = 0; i < users.getLength(); i++) {
                Node user = users.item(i);
                String name = user.getAttributes().getNamedItem("name").getTextContent();
                list.add(name);
            }

            logger.debug("Loaded " + users.getLength() + " users.");
//            logger.info("Users " + list.toString());

            return list;
        }
        catch(Exception e) {
            logger.info("Error while accessing crowd", e);
            return Collections.emptyList();
        }
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        URL url = new URL(this.url + path);

        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        String authorization = applicationUsername + ":" + applicationPassword;
        c.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(authorization.getBytes("ascii")));
        return c;
    }

    private void connect(HttpURLConnection c) throws IOException {
        try {
            logger.info(c.getRequestMethod() + " " + c.getURL().toExternalForm());
            c.connect();
            logger.info(c.getResponseCode() + " " + c.getResponseMessage());
        }
        catch(SSLHandshakeException e) {
            logger.info("Unable to establish SSL connection to the Crowd server. Is the certificate trusted or has it changed?");
            throw e;
        }
    }
}
