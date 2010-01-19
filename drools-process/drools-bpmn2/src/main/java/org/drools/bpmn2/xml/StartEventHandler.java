package org.drools.bpmn2.xml;

import java.util.List;
import java.util.Map;

import org.drools.bpmn2.core.Message;
import org.drools.process.core.event.EventTypeFilter;
import org.drools.workflow.core.Node;
import org.drools.workflow.core.node.ConstraintTrigger;
import org.drools.workflow.core.node.EventTrigger;
import org.drools.workflow.core.node.StartNode;
import org.drools.workflow.core.node.Trigger;
import org.drools.xml.ExtensibleXmlParser;
import org.drools.xml.ProcessBuildData;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class StartEventHandler extends AbstractNodeHandler {
    
    protected Node createNode(Attributes attrs) {
        return new StartNode();
    }
    
    @SuppressWarnings("unchecked")
	public Class generateNodeFor() {
        return StartNode.class;
    }

    @SuppressWarnings("unchecked")
    protected void handleNode(final Node node, final Element element, final String uri, 
            final String localName, final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        StartNode startNode = (StartNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, startNode);
            } else if ("conditionalEventDefinition".equals(nodeName)) {
                String constraint = null;
                org.w3c.dom.Node subNode = xmlNode.getFirstChild();
                while (subNode != null) {
                    String subnodeName = subNode.getNodeName();
                    if ("condition".equals(subnodeName)) {
                        constraint = xmlNode.getTextContent();
                        break;
                    }
                    subNode = subNode.getNextSibling();
                }
                ConstraintTrigger trigger = new ConstraintTrigger();
                trigger.setConstraint(constraint);
                startNode.addTrigger(trigger);
                break;
            } else if ("signalEventDefinition".equals(nodeName)) {
                String type = ((Element) xmlNode).getAttribute("signalRef");
                if (type != null && type.trim().length() > 0) {
                    EventTrigger trigger = new EventTrigger();
                    EventTypeFilter eventFilter = new EventTypeFilter();
                    eventFilter.setType(type);
                    trigger.addEventFilter(eventFilter);
                    String mapping = (String) startNode.getMetaData("TriggerMapping");
                    if (mapping != null) {
                        trigger.addInMapping(mapping, "event");
                    }
                    startNode.addTrigger(trigger);
                }
            } else if ("messageEventDefinition".equals(nodeName)) {
                String messageRef = ((Element) xmlNode).getAttribute("messageRef");
                Map<String, Message> messages = (Map<String, Message>)
                    ((ProcessBuildData) parser.getData()).getMetaData("Messages");
                if (messages == null) {
                    throw new IllegalArgumentException("No messages found");
                }
                Message message = messages.get(messageRef);
                if (message == null) {
                    throw new IllegalArgumentException("Could not find message " + messageRef);
                }
                startNode.setMetaData("MessageType", message.getType());
                EventTrigger trigger = new EventTrigger();
                EventTypeFilter eventFilter = new EventTypeFilter();
                eventFilter.setType("Message-" + messageRef);
                trigger.addEventFilter(eventFilter);
                String mapping = (String) startNode.getMetaData("TriggerMapping");
                if (mapping != null) {
                    trigger.addInMapping(mapping, "event");
                }
                startNode.addTrigger(trigger);
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }
    
    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, StartNode startNode) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        // targetRef
        subNode = subNode.getNextSibling();
        String to = subNode.getTextContent();
        startNode.setMetaData("TriggerMapping", to);
    }

    public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		StartNode startNode = (StartNode) node;
		writeNode("startEvent", startNode, xmlDump, includeMeta);
		List<Trigger> triggers = startNode.getTriggers();
		if (triggers != null) {
		    xmlDump.append(">" + EOL);
		    if (triggers.size() > 1) {
		        throw new IllegalArgumentException("Multiple start triggers not supported");
		    }
		    Trigger trigger = triggers.get(0);
		    if (trigger instanceof ConstraintTrigger) {
		        xmlDump.append("      <conditionalEventDefinition>" + EOL);
                xmlDump.append("        <condition xs:type=\"tFormalExpression\" language=\"" + XmlBPMNProcessDumper.RULE_LANGUAGE + "\">" + ((ConstraintTrigger) trigger).getConstraint() + "</condition>" + EOL);
                xmlDump.append("      </conditionalEventDefinition>" + EOL);
		    } else if (trigger instanceof EventTrigger) {
		        EventTrigger eventTrigger = (EventTrigger) trigger;
		        if (!trigger.getInMappings().isEmpty()) {
		            String mapping = eventTrigger.getInMappings().keySet().iterator().next();
		            xmlDump.append(
	                    "      <dataOutput id=\"_" + startNode.getId() + "_Output\" />" + EOL +
                        "      <dataOutputAssociation>" + EOL +
                        "        <sourceRef>_" + startNode.getId() + "_Output</sourceRef>" + EOL +
                        "        <targetRef>" + mapping + "</targetRef>" + EOL +
                        "      </dataOutputAssociation>" + EOL +
                        "      <outputSet>" + EOL +
                        "        <dataOutputRefs>_" + startNode.getId() + "_Output</dataOutputRefs>" + EOL +
                        "      </outputSet>" + EOL);
		        }
		        String type = ((EventTypeFilter) eventTrigger.getEventFilters().get(0)).getType();
		        if (type.startsWith("Message-")) {
                    type = type.substring(8);
                    xmlDump.append("      <messageEventDefinition messageRef=\"" + type + "\"/>" + EOL);
                } else {
                    xmlDump.append("      <signalEventDefinition signalRef=\"" + type + "\" />" + EOL);
                }
            } else {
		        throw new IllegalArgumentException("Unsupported trigger type " + trigger);
		    }
		    endNode("startEvent", xmlDump);
		} else {
		    endNode(xmlDump);
		}
	}

}
