package org.rmt2.rest.media;

import java.math.BigInteger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.rmt2.jaxb.MimeContentType;
import org.rmt2.jaxb.MultimediaRequest;
import org.rmt2.jaxb.MultimediaResponse;
import org.rmt2.jaxb.ObjectFactory;
import org.rmt2.rest.RMT2BaseRestResouce;

import com.api.messaging.MessageRoutingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/media/document/{transaction}")
public class DocumentMediaResource extends RMT2BaseRestResouce {
    private static final Logger LOGGER = Logger.getLogger(DocumentMediaResource.class);

    public DocumentMediaResource(@PathParam("transaction") final String transaction) {
        super("media", "document", transaction);
    }

    /**
     * Fetch the metadata and binary content by content id.
     * 
     * @param contentId
     *            the unique identifier of the content record. Must be greater
     *            than zero.
     * @return JSON containing metadata and base64 encoded binary content
     */
    @GET
    @Path("attachment/{contentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchImageContent(@PathParam("contentId") final long contentId) {
        LOGGER.info("REST method, forms/{contentId}/image-attachment, was called");
        if (contentId <= 0) {
            this.msg = "Web Service Error: Content Id must be greater than zero in order to retrieve image attachment";
            LOGGER.error(this.msg);
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE)
                    .entity(this.msg).build());
        }

        // Create multimedia request object with "contentId" param
        ObjectFactory f = new ObjectFactory();
        MultimediaRequest req = f.createMultimediaRequest();
        req.setHeader(this.getHeader());
        req.setContentId(BigInteger.valueOf(contentId));

        // Route message to business server
        MultimediaResponse r = null;
        try {
            Object response = this.msgRouterHelper.routeJsonMessage(this.transaction, req);
            if (response != null && response instanceof MultimediaResponse) {
                r = (MultimediaResponse) response;
            }
        } catch (MessageRoutingException e) {
            this.msg = e.getMessage();
            LOGGER.error("Unable to route attachment/{contentId} to its destination", e);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN_TYPE).entity(this.msg).build());
        }
        // if (response != null && response instanceof MultimediaResponse) {
        // r = (MultimediaResponse) response;
        // }
        // else { // TODO: Remove "else" once properly implemented
        // r = f.createMultimediaResponse();
        // MimeContentType content = f.createMimeContentType();
        // content.setAppCode("ACCT");
        // content.setContentId(BigInteger.valueOf(contentId));
        // content.setFilename("example.jpg");
        // content.setFilepath("/tmp/somefilepath/");
        // String imgContent = RMT2File
        // .getFileContentAsBase64("/Users/royterrell/Pictures/pearl-weathered-leather-1600-1200.jpg");
        // content.setBinaryData(imgContent);
        // r.setContent(content);
        // }

        // Marshal to JSON
        final Gson gson = new GsonBuilder().create();
        return Response.status(Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(gson.toJson(r)).build();
    }

    /**
     * Add the metadata and binary content.
     * 
     * @param content
     *            the metadata and binary content to be added. The binary
     *            content should be recieved as base64 encoded.
     * @return content metadata only in which a new content id should be
     *         provided.
     */
    @PUT
    @Path("attachment/save")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveImageContent(final MimeContentType content) {
        LOGGER.info("REST method, image-attachment/save, was called");
        ObjectFactory f = new ObjectFactory();

        // Create multimedia request object with "content" param
        MultimediaRequest req = f.createMultimediaRequest();
        req.setHeader(this.getHeader());
        req.getContent().add(content);

        // Route message to business server
        Object response = this.msgRouterHelper.routeJsonMessage(this.transaction, req);

        MultimediaResponse r = null;
        if (response != null && response instanceof MultimediaResponse) {
            r = (MultimediaResponse) response;
        }
        else { // TODO: Remove "else" once properly implemented
            r = f.createMultimediaResponse();
            MimeContentType contentType = f.createMimeContentType();
            contentType.setAppCode("ACCT");
            // New content id
            contentType.setContentId(BigInteger.valueOf(77777));
            contentType.setFilename("example.jpg");
            contentType.setFilepath("/tmp/somefilepath/");
        }
        // Do not return binary data for save. Only metadata.
        r.setContent(null);

        // Marshal to JSON
        final Gson gson = new GsonBuilder().create();
        return Response.status(Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(gson.toJson(r)).build();
    }
}