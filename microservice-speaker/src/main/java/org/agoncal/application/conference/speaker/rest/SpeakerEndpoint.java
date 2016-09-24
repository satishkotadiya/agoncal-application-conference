package org.agoncal.application.conference.speaker.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.agoncal.application.conference.commons.rest.LinkableEndpoint;
import org.agoncal.application.conference.speaker.domain.AcceptedTalk;
import org.agoncal.application.conference.speaker.domain.Speaker;
import org.agoncal.application.conference.speaker.domain.Speakers;
import org.agoncal.application.conference.speaker.repository.SpeakerRepository;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */
@Path("/speakers")
@Api(description = "Speakers REST Endpoint")
@RequestScoped
@Produces("application/json")
@Consumes("application/json")
public class SpeakerEndpoint extends LinkableEndpoint<Speaker> {

    // ======================================
    // =          Injection Points          =
    // ======================================

    @Inject
    private SpeakerRepository speakerRepository;

    // ======================================
    // =            Constructors            =
    // ======================================

    public SpeakerEndpoint() {
        super(SpeakerEndpoint.class);
    }

    // ======================================
    // =          Business methods          =
    // ======================================

    @POST
    @ApiOperation(value = "Adds a new speaker to the conference")
    @ApiResponses(value = {
        @ApiResponse(code = 405, message = "Invalid input")}
    )
    public Response add(Speaker speaker) {
        Speaker created = speakerRepository.create(speaker);
        return Response.created(getURIForSelf(speaker)).entity(created).build();
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Finds a speaker by ID", response = Speaker.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid input"),
        @ApiResponse(code = 404, message = "Speaker not found")}
    )
    public Response retrieve(@PathParam("id") String id, @DefaultValue("false") @QueryParam("expand") boolean expand, @Context Request request) {

        Speaker speaker = speakerRepository.findById(id);

        if (speaker == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        EntityTag etag = new EntityTag(Integer.toString(speaker.hashCode()));
        Response.ResponseBuilder preconditions = request.evaluatePreconditions(etag);

        // cached resource did change -> serve updated content
        if (preconditions == null) {
            speaker.addSelfLink(getURIForSelf(speaker));
            speaker.addCollectionLink(getURIForCollection());
            if (expand) {
                for (AcceptedTalk acceptedTalk : speaker.getAcceptedTalks()) {
                    acceptedTalk.addLink("self", getUriInfo().getAbsolutePath().resolve(acceptedTalk.getId()));
                }
            } else {
                speaker.setBio(null);
                speaker.setAcceptedTalks(null);
            }

            preconditions = Response.ok(speaker).tag(etag);
        }

        return preconditions.build();
    }

    @GET
    @ApiOperation(value = "Finds all the speakers", response = Speaker.class, responseContainer = "List")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Speakers not found")}
    )
    public Response allSpeakers(@DefaultValue("1") @QueryParam("page") Integer pageNumber) {
        List<Speaker> allSpeakers = speakerRepository.findAllSpeakers(pageNumber);

        if (allSpeakers == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        for (Speaker speaker : allSpeakers) {
            speaker.addSelfLink(getURIForSelf(speaker));
        }

        Speakers spakers = new Speakers(allSpeakers);
        Integer last = speakerRepository.getNumberOfPages();
        spakers.addSelfLink(getURIForPage(pageNumber));
        spakers.addFirst(getURIForPage(1));
        spakers.addLast(getURIForPage(last));
        spakers.addNext(getURIForPage(pageNumber < last ? pageNumber + 1 : last));
        spakers.addPrevious(getURIForPage(pageNumber == 1 ? 1 : pageNumber - 1));

        return Response.ok(buildEntities(spakers)).build();
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes a speaker")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid speaker value")}
    )
    public Response remove(@PathParam("id") String id) {
        speakerRepository.delete(id);
        return Response.noContent().build();
    }

    // ======================================
    // =           Private methods          =
    // ======================================

    private GenericEntity<Speakers> buildEntities(final Speakers speakers) {
        return new GenericEntity<Speakers>(speakers) {
        };
    }
}
