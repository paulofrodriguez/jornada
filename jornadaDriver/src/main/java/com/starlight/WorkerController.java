package com.starlight;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/worker")
public class WorkerController {

    private String currentStatus = "No status set";

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    public String getStatus() {
        return "Current Status: " + currentStatus;
    }

    @POST
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setStatus(WorkerStatus workerStatus) {

        String responseMessage = String.format(
                "Status updated for worker %s (ID: %s) to: %s at %s",
                workerStatus.getWorkerName(),
                workerStatus.getWorkerId(),
                workerStatus.getStatus(),
                workerStatus.getTimestamp()
        );

        return Response.ok(responseMessage).build();
    }

    @POST
    @Path("/checkin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setCheckin(WorkerStatus workerStatus) {

        String responseMessage = String.format(
                "Status updated for worker %s (ID: %s) to: %s at %s",
                workerStatus.getWorkerName(),
                workerStatus.getWorkerId(),
                workerStatus.getStatus(),
                workerStatus.getTimestamp()
        );

        return Response.ok(responseMessage).build();
    }
}