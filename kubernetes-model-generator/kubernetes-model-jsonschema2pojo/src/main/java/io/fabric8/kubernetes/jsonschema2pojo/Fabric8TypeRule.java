package io.fabric8.kubernetes.jsonschema2pojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.rules.TypeRule;

public class Fabric8TypeRule extends TypeRule {

  protected Fabric8TypeRule(RuleFactory ruleFactory) {
    super(ruleFactory);
  }

  @Override
  public JType apply(String nodeName, JsonNode node, JsonNode parent, JClassContainer jClassContainer, Schema schema) {
    if (node.has("existingJavaType") && "byte[]".equals(node.get("existingJavaType").asText())) {
      return jClassContainer.owner().BYTE.array();
    }

    try {
      return super.apply(nodeName, node, parent, jClassContainer, schema);
    } catch (ClassCastException e) {
      System.out.println(node);
      throw new RuntimeException(e);
    }
  }

}
