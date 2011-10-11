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
package com.gitblit;

import com.gitblit.models.UserModel;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import org.eclipse.jgit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrowdUserService implements IUserService {
    private final Logger logger = LoggerFactory.getLogger(CrowdUserService.class);
    private IUserService wrapped;
    private FileSettings settings;
    private String url;
    private String crowdUsername;
    private String crowdPassword;

    @Override
	public void setup(IStoredSettings settings) throws IOException {
        Properties properties = settings.read();

        File usersProperties = new File(settings.getString(Keys.crowd.users_properties, null));
        wrapped = new FileUserService(usersProperties);

        url = settings.getString(Keys.crowd.url, null);
        crowdUsername = settings.getString(Keys.crowd.username, null);
        crowdPassword = settings.getString(Keys.crowd.password, null);

        if(url == null || crowdUsername == null || crowdPassword == null) {
            throw new IOException("Missing required properties: " + Keys.crowd.url + ", " + Keys.crowd.username + ", " + Keys.crowd.password);
        }

        logger.info("Crowd URL: " + url);
    }

    @Override
	public boolean supportsCookies() {
        return wrapped.supportsCookies();
    }

    @Override
	public char[] getCookie(UserModel model) {
        return wrapped.getCookie(model);
    }

    @Override
	public UserModel authenticate(char[] cookie) {
        return wrapped.authenticate(cookie);
    }

    @Override
	public UserModel authenticate(String username, char[] password) {
        UserModel userModel = wrapped.getUserModel(username);
        if(userModel == null) {
            System.out.println("authenticate: Could not find user in file store");
            return null;
        }

        String authorization = crowdUsername + ":" + crowdPassword;

        HttpURLConnection c;
        try {
            URL url = new URL(this.url + "/rest/usermanagement/1/authentication?username=" + username);
            c = (HttpURLConnection)url.openConnection();
            c.setRequestProperty("Content-Type", "application/xml");
            c.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(authorization.getBytes("ascii")));
            c.setDoOutput(true);
            OutputStream os = c.getOutputStream();
            os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><password><value>".getBytes("utf-8"));
            // TODO: This is bad and defeats the purpose with using char arrays for containing passwords.
            os.write(new String(password).getBytes("utf-8"));
            os.write("</value></password>".getBytes("utf-8"));
            os.flush();
            int responseCode = c.getResponseCode();

            return responseCode == 200 ? userModel : null;
        }
        catch(Exception e) {
            logger.info("Error while accessing crowd", e);
            return null;
        }
        finally {
            if(c != null) {
                try {
                    c.close();
                }
                catch(Exception ignore) {
                }
            }
        }
    }

    @Override
	public UserModel getUserModel(String username) {
        return wrapped.getUserModel(username);
    }

    @Override
	public boolean updateUserModel(UserModel model) {
        return wrapped.updateUserModel(model);
    }

    @Override
	public boolean updateUserModel(String username, UserModel model) {
	    return wrapped.updateUserModel(username, model);
    }

    @Override
	public boolean deleteUserModel(UserModel model) {
	    return wrapped.deleteUserModel(model);
    }

    @Override
	public boolean deleteUser(String username) {
        return wrapped.deleteUser(username);
    }

    @Override
	public List<String> getAllUsernames() {
	    return wrapped.getAllUsernames();
    }

    @Override
	public List<String> getUsernamesForRepositoryRole(String role) {
	    return wrapped.getUsernamesForRepositoryRole(role);
    }

    @Override
	public boolean setUsernamesForRepositoryRole(String role, List<String> usernames) {
        return wrapped.setUsernamesForRepositoryRole(role, usernames);
    }

    @Override
	public boolean renameRepositoryRole(String oldRole, String newRole) {
        return wrapped.renameRepositoryRole(oldRole, newRole);
    }

    @Override
	public boolean deleteRepositoryRole(String role) {
	    return wrapped.deleteRepositoryRole(role);
    }

    @Override
	public String toString() {
        return wrapped.toString();
    }
}
