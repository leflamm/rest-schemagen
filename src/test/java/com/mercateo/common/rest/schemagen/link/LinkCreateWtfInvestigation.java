package com.mercateo.common.rest.schemagen.link;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import javax.ws.rs.Path;
import javax.ws.rs.core.Link;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.mercateo.common.rest.schemagen.GenericResource;
import com.mercateo.common.rest.schemagen.ImplementedBeanParamType;
import com.mercateo.common.rest.schemagen.JsonSchemaGenerator;
import com.mercateo.common.rest.schemagen.Something;
import com.mercateo.common.rest.schemagen.link.relation.Rel;
import com.mercateo.common.rest.schemagen.link.relation.Relation;

@SuppressWarnings("deprecation")
public class LinkCreateWtfInvestigation {

    private final String testSchema = "test";

    private JsonSchemaGenerator createJsonSchemaGenerator() {
        JsonSchemaGenerator jsonSchemaGenerator = Mockito.mock(JsonSchemaGenerator.class);
        when(jsonSchemaGenerator.createInputSchema(Matchers.any(), Matchers.any())).thenReturn(
                Optional.of(testSchema));
        when(jsonSchemaGenerator.createOutputSchema(Matchers.any(), Matchers.any())).thenReturn(
                Optional.of(testSchema));
        return jsonSchemaGenerator;
    }

    private Link createFor(Scope method, Relation relation) {
        final JsonSchemaGenerator jsonSchemaGenerator = createJsonSchemaGenerator();

        final LinkFactoryContext linkFactoryContext = new LinkFactoryContextDefault(URI.create(
                "http://host/base/"), o -> true, (o, c) -> true);
        final LinkCreator linkCreator = new LinkCreator(jsonSchemaGenerator, null);

        return linkCreator.createFor(Collections.singletonList(method), relation,
                linkFactoryContext);
    }

    private Link giveItATry(String queryParamValue) throws NoSuchMethodException,
            SecurityException {
        @Path("test")
        class ImplementedGenricResource extends
                GenericResource<Something, ImplementedBeanParamType> {
            @Override
            protected Something getReturnType(ImplementedBeanParamType param) {
                return new Something();
            }
        }

        ImplementedBeanParamType implementedBeanParamType = new ImplementedBeanParamType();
        implementedBeanParamType.setPathParam("path");
        implementedBeanParamType.setQueryParam1(queryParamValue);
        Scope scope = new CallScope(ImplementedGenricResource.class, ImplementedGenricResource.class
                .getMethod("get", Object.class), new Object[] { implementedBeanParamType }, null);

        Link link = createFor(scope, Relation.of(Rel.SELF));

        return link;
    }

    private <T extends Exception> void itFails(String queryParamValue, Class<T> exceptionType)
            throws NoSuchMethodException, SecurityException {
        try {
            giveItATry(queryParamValue);
            fail();
        } catch (Exception e) {
            assertTrue(exceptionType.isInstance(e));
        }
    }

    private void itWorks(String plainQueryParamValue, String expextedQueryParamValueInLink)
            throws NoSuchMethodException, SecurityException {
        assertEquals("qp1=" + expextedQueryParamValueInLink, giveItATry(plainQueryParamValue)
                .getUri()
                .getRawQuery());
    }

    @Test
    public void gatherSamples() throws NoSuchMethodException, SecurityException {
        itWorks("hello", "hello");
        itWorks("üöä", "%C3%BC%C3%B6%C3%A4");
        itFails("{\"foo\":\"bar\"}", IllegalArgumentException.class);
        // Huh?
        itFails("%7B%22foo%22%3A%22bar%22%7D", IllegalArgumentException.class);
        itWorks("%257B%2522foo%2522%253A%2522bar%2522%257D", "%7B%22foo%22%3A%22bar%22%7D");
        // No need to escape neither double quotes nor colons btw - it's just
        // the magic curly braces :-)
        itWorks("%257B\"foo\":\"bar\"%257D", "%7B%22foo%22%3A%22bar%22%7D");
    }

}
