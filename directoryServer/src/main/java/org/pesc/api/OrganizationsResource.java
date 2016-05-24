package org.pesc.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ResponseHeader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.pesc.api.model.CertificateInfo;
import org.pesc.api.model.Organization;
import org.pesc.api.model.SchoolCode;
import org.pesc.service.OrganizationService;
import org.pesc.service.PagedData;
import org.pesc.service.SchoolCodesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.jws.WebService;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by james on 3/22/16.
 */
@Component
@WebService
@Path("/organizations")
@Api("/organizations")
@CrossOriginResourceSharing(allowAllOrigins = true, allowCredentials = true, maxAge = 1)
public class OrganizationsResource {

    private static final Log log = LogFactory.getLog(OrganizationsResource.class);

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SchoolCodesService schoolCodesService;

    @Context
    private HttpServletResponse servletResponse;





    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Search organizations based on the optional search parameters.  All organizations are returned" +
            " when no search criteria are provided.")
    @ResponseHeader(name="X-Total-Count", description = "The total number of available records. Useful for pagination.")
    public List<Organization> findOrganization(
            @QueryParam("id") @ApiParam("The directory identifier for the organization.") Integer id,
            @QueryParam("organizationCode") @ApiParam("A code such as ATP code that identifies the organization.") String organizationCode,
            @QueryParam("organizationCodeType") @ApiParam("Indicates the type of organization code and should be one of the following: ACT, ATP, FICE, IPEDS.") String organizationCodeType,
            @QueryParam("name") @ApiParam("The case insensitive organization name or partial name.") String name,
            @QueryParam("subcode") @ApiParam("A proprietary code used to identify an organization.") String subcode,
            @QueryParam("type") @ApiParam("The type of organization (Institution, Service Provider).") String type,
            @QueryParam("ein") @ApiParam("The federal tax identification number (Employer Identification Number).") String ein,
            @QueryParam("createdTime") Long createdTime,
            @QueryParam("modifiedTime") Long modifiedTime,
            @QueryParam("active") @ApiParam("Indicates whether the organization and all it's endpoints are active.") Boolean active,
            @QueryParam("enabled") @ApiParam("Indicates whether the organization is in good standing and included in the directory.") Boolean enabled,
            @QueryParam("serviceprovider") @ApiParam("Indicates whether the organization is a service provider.") Boolean isServiceProvider,
            @QueryParam("institution") @ApiParam("Indicates whether the organization is an institution.") Boolean isInstitution,
            @QueryParam("limit") @DefaultValue("5") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset


    ) {

        if (limit == null || offset == null) {
            limit = 5;
            offset = 0;
        }
        PagedData<Organization> pagedData = new PagedData<Organization>(limit,offset);

        organizationService.search(
                id,
                organizationCode,
                organizationCodeType,
                name,
                subcode,
                type,
                ein,
                createdTime,
                modifiedTime,
                active,
                enabled,
                isServiceProvider,
                isInstitution,
                pagedData);

        servletResponse.addHeader("X-Total-Count", String.valueOf(pagedData.getTotal()) );
        return pagedData.getData();
    }

    @GET
    @Path("/{id}")
    @ApiOperation("Return the organization with the given id.")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<Organization> getOrganization(@PathParam("id") @ApiParam("The directory identifier for the organization.") Integer id) {
        ArrayList<Organization> results = new ArrayList<Organization>();

        Organization organization = organizationService.findById(id);

        if (organization != null) {
            results.add(organization);
        }

        return results;
    }


    @CrossOriginResourceSharing(allowAllOrigins = true, allowCredentials = true, maxAge = 1)
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Create an organization.")
    public Organization createOrganization(Organization org) {

        return organizationService.create(org);
    }


    @Path("/{id}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Update the organization with the given ID.")
    public Organization saveOrganization(@PathParam("id") @ApiParam("The directory identifier for the organization.") Integer id, Organization org) {
        return organizationService.update(org);
    }

    @CrossOriginResourceSharing(allowAllOrigins = true, allowCredentials = true, maxAge = 1)
    @Path("/{id}")
    @DELETE
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Delete the organization with the given ID.")
    public void removeOrganization(@PathParam("id") @ApiParam("The directory identifier for the organization.") Integer id) {

        organizationService.delete(id);

    }

    @Path("/{id}")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Add the the endpoint with the given ID to the organization's list of endpoints.")
    public void updateEndpoints(@PathParam("id") @ApiParam("The identifier for the organization.") Integer id,
                                @QueryParam("endpoint_id") @ApiParam(value="The identifier for the endpoint.", required = true) Integer endpointID,
                                @QueryParam("operation") @ApiParam(value="The operation to perform. Must by case insensitive 'add' or 'remove'.", required = true) String operation) {

        checkParameter(endpointID, "endpoint_id");
        checkParameter(operation, "operation");

        if ("add".equalsIgnoreCase(operation)) {
            organizationService.addEndpointToOrganization(id, endpointID);
        }

        else if ("remove".equalsIgnoreCase(operation)) {
            organizationService.removeEndpointToOrganization(id, endpointID);
        }

    }


    @Path("/{id}/school-code")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Add or update a school code for the institution. Code types include 'FICE', 'ATP', 'ACT', 'IPEDS' and 'OPEID'")
    public SchoolCode putSchoolCode(@PathParam("id") @ApiParam("The identifier for the organization.") Integer id,
                              @FormParam("code-type") @ApiParam("'FICE', 'ATP', 'ACT', 'IPEDS' or 'OPEID'") String codeType,
                              @FormParam("code") String code) {

        SchoolCode schoolCode = new SchoolCode();
        schoolCode.setOrganizationId(id);
        schoolCode.setCode(code);
        schoolCode.setCodeType(codeType);

        return schoolCodesService.create(schoolCode);
    }




    @Path("/{id}/enabled")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Update the enabled property of the organization.")
    public void updateEnabledProperty(@PathParam("id") @ApiParam("The identifier for the organization.") Integer id, Boolean enabled) {

        organizationService.setProperty(id, "enabled", enabled);

    }


    @Path("/{id}/signing-certificate")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Update the signing certificate of the organization.")
    public CertificateInfo udpateCertificateProperty(@PathParam("id") @ApiParam("The identifier for the organization.") Integer id, String pemCert) {

        try {

            return organizationService.setCertificate(id, pemCert);


        } catch (CertificateException e) {
            log.error("Failed to read signing certificate.", e);
            throw new WebApplicationException("Failed to save certificate", e);
        }


    }

    @Path("/{id}/signing-certificate")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ApiOperation("Get certificate info including the PEM formatted signing certificate for the given organization.")
    public CertificateInfo getCertificate(@PathParam("id") @ApiParam("The identifier for the organization.") Integer id) {

        try {

            return organizationService.getPEMCertificate(id);

        } catch (CertificateException e) {
            log.error("Failed to retrieve signing certificate.", e);
            throw new WebApplicationException("Failed to retrieve certificate", e);
        }
    }

    public static void checkParameter(Object param, String parameterName) {
        if (param == null) {
            throw new WebApplicationException(String.format("The %s parameter is required.", parameterName));
        }
    }

}
