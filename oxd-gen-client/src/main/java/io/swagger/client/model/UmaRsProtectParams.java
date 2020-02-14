/*
 * oxd-server
 * oxd-server
 *
 * OpenAPI spec version: 4.2
 * Contact: yuriyz@gluu.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.client.model.RsResource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * UmaRsProtectParams
 */


public class UmaRsProtectParams {
  @SerializedName("oxd_id")
  private String oxdId = null;

  @SerializedName("overwrite")
  private Boolean overwrite = null;

  @SerializedName("resources")
  private List<RsResource> resources = new ArrayList<RsResource>();

  public UmaRsProtectParams oxdId(String oxdId) {
    this.oxdId = oxdId;
    return this;
  }

   /**
   * Get oxdId
   * @return oxdId
  **/
  @Schema(example = "bcad760f-91ba-46e1-a020-05e4281d91b6", required = true, description = "")
  public String getOxdId() {
    return oxdId;
  }

  public void setOxdId(String oxdId) {
    this.oxdId = oxdId;
  }

  public UmaRsProtectParams overwrite(Boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

   /**
   * Get overwrite
   * @return overwrite
  **/
  @Schema(required = true, description = "")
  public Boolean isOverwrite() {
    return overwrite;
  }

  public void setOverwrite(Boolean overwrite) {
    this.overwrite = overwrite;
  }

  public UmaRsProtectParams resources(List<RsResource> resources) {
    this.resources = resources;
    return this;
  }

  public UmaRsProtectParams addResourcesItem(RsResource resourcesItem) {
    this.resources.add(resourcesItem);
    return this;
  }

   /**
   * Get resources
   * @return resources
  **/
  @Schema(required = true, description = "")
  public List<RsResource> getResources() {
    return resources;
  }

  public void setResources(List<RsResource> resources) {
    this.resources = resources;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UmaRsProtectParams umaRsProtectParams = (UmaRsProtectParams) o;
    return Objects.equals(this.oxdId, umaRsProtectParams.oxdId) &&
        Objects.equals(this.overwrite, umaRsProtectParams.overwrite) &&
        Objects.equals(this.resources, umaRsProtectParams.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oxdId, overwrite, resources);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UmaRsProtectParams {\n");
    
    sb.append("    oxdId: ").append(toIndentedString(oxdId)).append("\n");
    sb.append("    overwrite: ").append(toIndentedString(overwrite)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
