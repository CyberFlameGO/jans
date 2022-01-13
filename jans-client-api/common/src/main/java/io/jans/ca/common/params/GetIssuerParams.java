package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetIssuerParams implements IParams {

    @JsonProperty(value = "op_host")
    private String op_host;

    @JsonProperty(value = "op_discovery_path")
    private String op_discovery_path;

    @JsonProperty(value = "op_configuration_endpoint")
    private String op_configuration_endpoint;

    @JsonProperty(value = "resource")
    private String resource;

    public String getOpHost() {
        return op_host;
    }

    public void setOpHost(String opHost) {
        this.op_host = opHost;
    }

    public String getOpDiscoveryPath() {
        return op_discovery_path;
    }

    public void setOpDiscoveryPath(String opDiscoveryPath) {
        this.op_discovery_path = opDiscoveryPath;
    }

    public String getOpConfigurationEndpoint() {
        return op_configuration_endpoint;
    }

    public void setOpConfigurationEndpoint(String op_configuration_endpoint) {
        this.op_configuration_endpoint = op_configuration_endpoint;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        return "WebfingerParams{" +
                "op_host='" + op_host + '\'' +
                ", op_discovery_path='" + op_discovery_path + '\'' +
                ", op_configuration_endpoint='" + op_configuration_endpoint + '\'' +
                ", resource='" + resource + '\'' +
                '}';
    }

}
