
/*
 *
 *   Copyright 2017 Crown Copyright
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package stroom.authentication.resources.user.v1;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import event.logging.Event;
import event.logging.MultiObject;
import event.logging.ObjectOutcome;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.jooq.Record;
import org.jooq.Record13;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.authentication.clients.AuthorisationService;
import stroom.authentication.clients.UserServiceClient;
import stroom.authentication.impl.db.UserDao;
import stroom.authentication.service.eventlogging.StroomEventLoggingService;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.util.shared.RestResource;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static stroom.auth.db.Tables.USERS;

@Singleton
@Path("/user/v1")
@Produces({"application/json"})
@Api(description = "Stroom User API", tags = {"User"})
public final class UserResource implements RestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);

    private AuthorisationService authorisationService;
    private UserService userService;
    private stroom.security.impl.UserService securityUserService;
    private UserDao userDao;
    private StroomEventLoggingService stroomEventLoggingService;
    private SecurityContext securityContext;

    @Inject
    public UserResource(
            @NotNull final AuthorisationService authorisationService,
            final UserService userService,
            final stroom.security.impl.UserService securityUserService,
            final UserDao userDao,
            final StroomEventLoggingService stroomEventLoggingService,
            final SecurityContext securityContext) {
        super();
        this.authorisationService = authorisationService;
        this.userService = userService;
        this.securityUserService = securityUserService;
        this.userDao = userDao;
        this.stroomEventLoggingService = stroomEventLoggingService;
        this.securityContext = securityContext;
    }


    @ApiOperation(
            value = "Get all users.",
            response = String.class,
            tags = {"User"})
    @GET
    @Path("/")
    @Timed
    @NotNull
    public final Response getAll(@Context @NotNull HttpServletRequest httpServletRequest ) {
        String usersAsJson = userService.getAllAsJson();
        return Response.status(Response.Status.OK).entity(usersAsJson).build();
    }

    @ApiOperation(
            value = "Create a user.",
            response = Integer.class,
            tags = {"User"})
    @POST
    @Path("/")
    @Timed
    @NotNull
    public final Response createUser(
            @Context @NotNull HttpServletRequest httpServletRequest,
            @Context @NotNull DSLContext database,
            @ApiParam("user") @NotNull User user) {
        int newUserId = userService.create(user);
        return Response.status(Response.Status.OK).entity(newUserId).build();
    }

    @ApiOperation(
            value = "Search for a user by email.",
            response = String.class,
            tags = {"User"})
    @GET
    @Path("search")
    @Timed
    @NotNull
    public final Response searchUsers(
            @Context @NotNull HttpServletRequest httpServletRequest,
            @Context @NotNull DSLContext database,
            @QueryParam("email") String email) {

        return securityContext.secureResult(PermissionNames.MANAGE_USERS_PERMISSION, () -> {
            // Get the users
            Result<Record13<Integer, String, String, String, String, String, Integer, Integer, Timestamp, Timestamp, String, Timestamp, String>> foundUserRecord = database
                    .select(USERS.ID,
                            USERS.EMAIL,
                            USERS.FIRST_NAME,
                            USERS.LAST_NAME,
                            USERS.COMMENTS,
                            USERS.STATE,
                            USERS.LOGIN_FAILURES,
                            USERS.LOGIN_COUNT,
                            USERS.LAST_LOGIN,
                            USERS.UPDATED_ON,
                            USERS.UPDATED_BY_USER,
                            USERS.CREATED_ON,
                            USERS.CREATED_BY_USER)
                    .from(USERS)
                    .where(new Condition[]{USERS.EMAIL.contains(email)})
                    .fetch();

            Response response;
            if (foundUserRecord == null) {
                response = Response.status(Response.Status.NOT_FOUND).build();
                return response;
            } else {
                String users = foundUserRecord.formatJSON((new JSONFormat())
                        .header(false)
                        .recordFormat(JSONFormat.RecordFormat.OBJECT));

                event.logging.User user = new event.logging.User();
                user.setId(email);
                ObjectOutcome objectOutcome = new ObjectOutcome();
                objectOutcome.getObjects().add(user);
                stroomEventLoggingService.view(
                        "SearchUser",
                        httpServletRequest,
                        securityContext.getUserId(),
                        objectOutcome,
                        "Search for a user.");

                response = Response.status(Response.Status.OK).entity(users).build();
                return response;
            }
        });
    }

    @ApiOperation(
            value = "Get a user by ID.",
            response = String.class,
            tags = {"User"})
    @GET
    @Path("{id}")
    @Timed
    @NotNull
    public final Response getUser(
            @Context @NotNull HttpServletRequest httpServletRequest,
            @PathParam("id") int userId) {
        final String loggedInUser = securityContext.getUserId();

        Optional<User> optionalUser = userDao.get(userId);
        Response response;
        if (optionalUser.isEmpty()) {
            response = Response.status(Response.Status.NOT_FOUND).build();
            return response;
        } else {
            User foundUser = optionalUser.get();
            // We only need to check auth permissions if the user is trying to access a different user.
            final boolean isUserAccessingThemselves = loggedInUser.equals(foundUser.getEmail());
            boolean canManageUsers = securityContext.hasAppPermission(PermissionNames.MANAGE_USERS_PERMISSION);
            if (!isUserAccessingThemselves && !canManageUsers) {
                return Response.status(Response.Status.UNAUTHORIZED).entity(UserServiceClient.UNAUTHORISED_USER_MESSAGE).build();
            }

            event.logging.User user = new event.logging.User();
            user.setId(foundUser.getEmail());
            ObjectOutcome objectOutcome = new ObjectOutcome();
            objectOutcome.getObjects().add(user);
            stroomEventLoggingService.view(
                    "GetUser",
                    httpServletRequest,
                    loggedInUser,
                    objectOutcome,
                    "Read a specific user.");

            response = Response.status(Response.Status.OK).entity(foundUser).build();
            return response;
        }
    }

    @ApiOperation(
            value = "Update a user.",
            response = String.class,
            tags = {"User"})
    @PUT
    @Path("{id}")
    @Timed
    @NotNull
    public final Response updateUser(
            @Context @NotNull HttpServletRequest httpServletRequest,
            @ApiParam("user") @NotNull User user,
            @PathParam("id") int userId) {
        return securityContext.secureResult(PermissionNames.MANAGE_USERS_PERMISSION, () -> {
            final String loggedInUser = securityContext.getUserId();

            Optional<User> optionalUser = userDao.get(userId);
            Response response;
            if (optionalUser.isEmpty()) {
                response = Response.status(Response.Status.NOT_FOUND).build();
                return response;
            }

            User foundUser = optionalUser.get();

            // TODO: auth-into-stroom migration: change the Stroom user
            boolean isEnabled = user.getState().equals("enabled");
            stroom.security.shared.User userToUpdate = securityUserService.getUserByName(foundUser.getEmail());
            userToUpdate.setEnabled(isEnabled);
            securityUserService.update(userToUpdate);

            user.setUpdatedByUser(loggedInUser);
            user.setUpdatedOn(LocalDateTime.now().toString());
            user.setId(userId);
            userDao.update(user);
//            UsersRecord updatedUsersRecord = UserMapper.updateUserRecordWithUser(user, usersRecord);
//            database
//                    .update((Table) USERS)
//                    .set(updatedUsersRecord)
//                    .where(new Condition[]{USERS.ID.eq(userId)}).execute();

            event.logging.User eventUser = new event.logging.User();
            eventUser.setId(Integer.valueOf(userId).toString());
            MultiObject afterMultiObject = new MultiObject();
            afterMultiObject.getObjects().add(eventUser);
            Event.EventDetail.Update update = new Event.EventDetail.Update();
            update.setAfter(afterMultiObject);
            stroomEventLoggingService.update(
                    "UpdateUser",
                    httpServletRequest,
                    loggedInUser,
                    update,
                    "Toggle whether a token is enabled or not.");

            response = Response.status(Response.Status.NO_CONTENT).build();
            return response;
        });
    }

    @ApiOperation(
            value = "Delete a user by ID.",
            response = String.class,
            tags = {"User"})
    @DELETE
    @Path("{id}")
    @Timed
    @NotNull
    public final Response deleteUser(
            @Context @NotNull HttpServletRequest httpServletRequest,
            @PathParam("id") int userId) {

        return securityContext.secureResult(PermissionNames.MANAGE_USERS_PERMISSION, () -> {
            userDao.delete(userId);

            event.logging.User user = new event.logging.User();
            user.setId(Integer.valueOf(userId).toString());
            ObjectOutcome objectOutcome = new ObjectOutcome();
            objectOutcome.getObjects().add(user);
            stroomEventLoggingService.delete(
                    "DeleteUser",
                    httpServletRequest,
                    securityContext.getUserId(),
                    objectOutcome,
                    "Delete a user by ID");

            Response response = Response.status(Response.Status.NO_CONTENT).build();
            return response;
        });
    }
}

