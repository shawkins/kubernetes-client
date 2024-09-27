
package io.fabric8.openshift.api.model.operatorhub.v1;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.builder.Editable;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "disableCopiedCSVs",
    "packageServerSyncInterval"
})
@ToString
@EqualsAndHashCode
@Accessors(prefix = {
    "_",
    ""
})
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false, lazyCollectionInitEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", refs = {
    @BuildableReference(ObjectMeta.class),
    @BuildableReference(LabelSelector.class),
    @BuildableReference(Container.class),
    @BuildableReference(PodTemplateSpec.class),
    @BuildableReference(ResourceRequirements.class),
    @BuildableReference(IntOrString.class),
    @BuildableReference(ObjectReference.class),
    @BuildableReference(LocalObjectReference.class),
    @BuildableReference(PersistentVolumeClaim.class)
})
@Generated("jsonschema2pojo")
public class OLMConfigSpecFeatures implements Editable<OLMConfigSpecFeaturesBuilder> , KubernetesResource
{

    @JsonProperty("disableCopiedCSVs")
    private Boolean disableCopiedCSVs;
    @JsonProperty("packageServerSyncInterval")
    private String packageServerSyncInterval;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public OLMConfigSpecFeatures() {
    }

    public OLMConfigSpecFeatures(Boolean disableCopiedCSVs, String packageServerSyncInterval) {
        super();
        this.disableCopiedCSVs = disableCopiedCSVs;
        this.packageServerSyncInterval = packageServerSyncInterval;
    }

    @JsonProperty("disableCopiedCSVs")
    public Boolean getDisableCopiedCSVs() {
        return disableCopiedCSVs;
    }

    @JsonProperty("disableCopiedCSVs")
    public void setDisableCopiedCSVs(Boolean disableCopiedCSVs) {
        this.disableCopiedCSVs = disableCopiedCSVs;
    }

    @JsonProperty("packageServerSyncInterval")
    public String getPackageServerSyncInterval() {
        return packageServerSyncInterval;
    }

    @JsonProperty("packageServerSyncInterval")
    public void setPackageServerSyncInterval(String packageServerSyncInterval) {
        this.packageServerSyncInterval = packageServerSyncInterval;
    }

    @JsonIgnore
    public OLMConfigSpecFeaturesBuilder edit() {
        return new OLMConfigSpecFeaturesBuilder(this);
    }

    @JsonIgnore
    public OLMConfigSpecFeaturesBuilder toBuilder() {
        return edit();
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

}