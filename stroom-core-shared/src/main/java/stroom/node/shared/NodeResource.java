/*
 * Copyright 2017 Crown Copyright
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

package stroom.node.shared;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.fusesource.restygwt.client.DirectRestService;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "node - /v1")
@Path(NodeResource.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface NodeResource extends RestResource, DirectRestService {
    String BASE_PATH = "/node" + ResourcePaths.V1;
    String PING_PATH_PART = "/ping";
    String INFO_PATH_PART = "/info";
    String PRIORITY_PATH_PART = "/priority";
    String ENABLED_PATH_PART = "/enabled";
    String NODE_NAME_PATH_PARAM = "/{nodeName}";

    @GET
    @Path(INFO_PATH_PART + NODE_NAME_PATH_PARAM)
    @ApiOperation(
            value = "Gets detailed information about a node",
            response = Long.class)
    ClusterNodeInfo info(@PathParam("nodeName") String nodeName);

    @GET
    @Path("/all")
    @ApiOperation(
            value = "Lists all nodes",
            response = List.class)
    List<String> listAllNodes();

    @GET
    @Path("/enabled")
    @ApiOperation(
            value = "Lists enabled nodes",
            response = List.class)
    List<String> listEnabledNodes();

    @GET
    @ApiOperation(
            value = "Lists nodes",
            response = FetchNodeStatusResponse.class)
    FetchNodeStatusResponse find();

    @GET
    @Path(PING_PATH_PART + NODE_NAME_PATH_PARAM)
    @ApiOperation(
            value = "Gets a ping time for a node",
            response = Long.class)
    Long ping(@PathParam("nodeName") String nodeName);

    @PUT
    @Path(PRIORITY_PATH_PART + NODE_NAME_PATH_PARAM)
    @ApiOperation(value = "Sets the priority of a node")
    void setPriority(@PathParam("nodeName") String nodeName, Integer priority);

    @PUT
    @Path(ENABLED_PATH_PART + NODE_NAME_PATH_PARAM)
    @ApiOperation(value = "Sets whether a node is enabled")
    void setEnabled(@PathParam("nodeName") String nodeName, Boolean enabled);
}