package com.WAT.airbnb.rest;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.rest.entities.TestEntity;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Path("entities")
public class EntityServer {
//TODO: Add authentication to the upload service
    @GET
    @Path("/getentities")
    public Response getEntity() {
        try {
            DataSource.getInstance().printCon();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }
}
