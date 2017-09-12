package com.WAT.airbnb.etc;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;

public class XmlParser {
    private String filename;

    public XmlParser(String filename) {
        this.filename = filename;
    }

    public String getConf(String name) {
        Document doc = null;
        try {
            doc = parseXML();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        Node rootNode = doc.getElementsByTagName("cfg").item(0);
        NodeList lst = rootNode.getChildNodes();
        for (int i = 0; i < lst.getLength(); i++) {
            if (lst.item(i).getNodeName().equals("database")) {
                NodeList innerList = lst.item(i).getChildNodes();
                for (int j = 0; j < innerList.getLength(); j++) {
                    Node innerNode = innerList.item(j);
                    if (innerNode.getNodeName().equals(name)) {
                        return innerNode.getTextContent();
                    }
                }
            }
        }

        return null;
    }

    public String get(String name) {
        Document doc = null;
        try {
            doc = parseXML();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        Node rootNode = doc.getElementsByTagName("cfg").item(0);
        NodeList lst = rootNode.getChildNodes();
        for (int i = 0; i < lst.getLength(); i++) {
            NodeList innerList = lst.item(i).getChildNodes();
            for (int j = 0; j < innerList.getLength(); j++) {
                Node innerNode = innerList.item(j);
                if (innerNode.getNodeName().equals(name)) {
                    return innerNode.getTextContent();
                }
            }
        }

        return null;
    }

    public Integer getCode(String codeType, String codeName) {
        Document doc = null;
        try {
            doc = parseXML();
        } catch (ParserConfigurationException pce) {
            System.out.println("Parser Configuration exception");
            pce.printStackTrace();
        } catch (SAXException se) {
            System.out.println("SAX Exception");
            se.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("IO Exception");
            ioe.printStackTrace();
        }

        if (doc != null) {

            Node rootNode = doc.getElementsByTagName("codes").item(0);
            NodeList lst = rootNode.getChildNodes();
            for (int i = 0; i < lst.getLength(); i++) {
                if (lst.item(i).getNodeName().equals(codeType)) {
                    NodeList typeList = lst.item(i).getChildNodes();
                    for (int j = 0; j < typeList.getLength(); j++) {
                        Node innerNode = typeList.item(j);
                        if (innerNode.getNodeName().equals(codeName)) {
                            return Integer.parseInt(innerNode.getTextContent());
                        }
                    }
                }
            }

            return null;
        } else {
            System.out.println("Doc is null");
            return null;
        }
    }

    private Document parseXML() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(this.filename);
        doc.getDocumentElement().normalize();
        return doc;
    }

}
