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

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.gitblit.crowd.CrowdClient;
import com.gitblit.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrowdUserService implements IUserService {
    private final Logger logger = LoggerFactory.getLogger(CrowdUserService.class);
    private IUserService wrapped;
    private CrowdClient client;

	public void setup(IStoredSettings settings) throws IOException {
        File usersProperties = new File(settings.getString(Keys.crowd.users_properties, null));
        wrapped = new FileUserService(usersProperties);

        String url = settings.getString(Keys.crowd.url, null);

        try {
            client = new CrowdClient(logger,
                    url,
                    settings.getString(Keys.crowd.applicationUsername, null),
                    settings.getString(Keys.crowd.applicationPassword, null),
                    settings.getString(Keys.crowd.group, null));
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }

        logger.info("Crowd URL: " + url);
    }

	public boolean supportsCookies() {
        return wrapped.supportsCookies();
    }

	public char[] getCookie(UserModel model) {
        return wrapped.getCookie(model);
    }

	public UserModel authenticate(char[] cookie) {
        return wrapped.authenticate(cookie);
    }

	public UserModel authenticate(String username, char[] password) {
        return client.authenticate(username, password) ? getUserModel(username, true) : null;
    }

	public UserModel getUserModel(String username) {
        return getUserModel(username, false);
    }

	public boolean updateUserModel(UserModel model) {
        return wrapped.updateUserModel(model);
    }

	public boolean updateUserModel(String username, UserModel model) {
	    return wrapped.updateUserModel(username, model);
    }

	public boolean deleteUserModel(UserModel model) {
	    return wrapped.deleteUserModel(model);
    }

	public boolean deleteUser(String username) {
        return wrapped.deleteUser(username);
    }

	public List<String> getAllUsernames() {
	    return client.allUsersInGroup();
    }

	public List<String> getUsernamesForRepositoryRole(String role) {
	    return wrapped.getUsernamesForRepositoryRole(role);
    }

	public boolean setUsernamesForRepositoryRole(String role, List<String> usernames) {
        return wrapped.setUsernamesForRepositoryRole(role, usernames);
    }

	public boolean renameRepositoryRole(String oldRole, String newRole) {
        return wrapped.renameRepositoryRole(oldRole, newRole);
    }

	public boolean deleteRepositoryRole(String role) {
	    return wrapped.deleteRepositoryRole(role);
    }

    @Override
	public String toString() {
        return "Crowd User Service with file store.";
    }

	public UserModel getUserModel(String username, boolean createIfMissing) {
        UserModel userModel = wrapped.getUserModel(username);
        if(userModel == null && createIfMissing) {
            logger.info("Creating user: " + username);
            userModel = new UserModel(username);
            userModel.password = "";
            wrapped.updateUserModel(userModel);
        }
        return userModel;
    }
}
