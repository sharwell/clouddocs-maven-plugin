package com.rackspace.cloud.api.docs;

import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineInput;
import com.rackspace.papi.components.translation.xproc.calabash.CalabashPipelineBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CalabashHelper {
    private static Source run(final String pipelineURI, final InputSource inputSource, final Map<String, Object> map) throws FileNotFoundException {
        Pipeline pipeline = new CalabashPipelineBuilder(false, true).build(pipelineURI);
        
        StringBuilder strBuff = new StringBuilder("<c:param-set xmlns:c=\"http://www.w3.org/ns/xproc-step\">");
        
        if(null!=map){
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof File) {
                    strBuff
                        .append("<c:param name=\"")
                        .append(escapeXmlAttribute(entry.getKey()))
                        .append("\" namespace=\"\" value=\"")
                        .append(escapeXmlAttribute(((File)entry.getValue()).toURI().toString()))
                        .append("\"/>");
                } else if (entry.getValue() instanceof URI || entry.getValue() instanceof String) {
                    strBuff
                        .append("<c:param name=\"")
                        .append(escapeXmlAttribute(entry.getKey()))
                        .append("\" namespace=\"\" value=\"")
                        .append(escapeXmlAttribute(entry.getValue().toString()))
                        .append("\"/>");
                } else if (entry.getValue() != null) {
                    throw new UnsupportedOperationException(String.format("The map cannot contain values of type %s.", entry.getValue().getClass()));
                } else {
                    // ignore nulls
                }
            }
        }
        strBuff.append("</c:param-set>");
        String params=strBuff.toString();
        final InputStream paramsStream = new ByteArrayInputStream(params.getBytes());
        
        //System.out.println("~!~!~!~!~!~!~!~!~!Sending: \n"+params+"\n");
        @SuppressWarnings("rawtypes")
        List<PipelineInput> pipelineInputs = new ArrayList<PipelineInput>() {{
            add(PipelineInput.port("source", inputSource));
            add(PipelineInput.port("parameters", new InputSource(paramsStream)));
        }};
        
        pipeline.run(pipelineInputs);
        List<Source> sources = pipeline.getResultPort("result"); // result of xinclude;

        return sources.get(0);
    }

    private static String escapeXmlAttribute(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("%", "&#37;");
    }

    public static Source createSource(Source source, String pipelineURI, Map<String, Object> map)
            throws MojoExecutionException {

        try {
            if (!(source instanceof SAXSource)) {
                throw new MojoExecutionException("Expecting a SAXSource");
            }
            SAXSource saxSource = (SAXSource) source;

            return run(pipelineURI, saxSource.getInputSource(),map);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Failed to find source.", e);
        }
        
    }

}
