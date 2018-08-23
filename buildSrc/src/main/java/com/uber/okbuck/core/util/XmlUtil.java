package com.uber.okbuck.core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XmlUtil {

  private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

  private XmlUtil() {}

  public static Document loadXml(String xmlString) {
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      InputSource source = new InputSource(new StringReader(xmlString));
      Document doc = dBuilder.parse(source);
      doc.getDocumentElement().normalize();
      return doc;
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new RuntimeException(e);
    }
  }

  public static Document loadXml(File xmlFile) {
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();
      return doc;
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeToXml(Document document, File xmlFile) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.INDENT, "no");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");

      // normalize document
      document.normalize();

      // Set xml standalone to true to not print the attribute
      document.setXmlStandalone(true);

      // Set android namespace
      document
          .getDocumentElement()
          .setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android");

      Writer stringWriter = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

      String xmlString =
          stringWriter
              .toString()
              .replaceAll("(?s)<!--.*?-->", "")
              .replaceAll("xmlns:android=\"http://schemas.android.com/apk/res/android\"", "")
              .replaceFirst(
                  "<manifest ",
                  "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" ")
              .replaceAll(">[\\s\\S]*?<", "><");

      writeText(xmlString, xmlFile);
    } catch (IOException | TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  static void writeText(String text, File file) throws IOException {
    BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
    writer.write(text);
    writer.close();
  }
}
