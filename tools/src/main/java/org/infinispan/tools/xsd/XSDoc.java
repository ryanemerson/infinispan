package org.infinispan.tools.xsd;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.infinispan.tools.ToolUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import gnu.getopt.Getopt;

public class XSDoc {

   public static class Schema {
      final String namespace;
      final String name;
      final Document doc;
      final int major;
      final int minor;

      public Schema(Document doc, String name) {
         this.name = name;
         this.doc = doc;
         String versionedNamespace = getDocumentNamespace(doc);
         if (versionedNamespace.startsWith("urn:")) {
            int versionSeparator = versionedNamespace.lastIndexOf(':');
            namespace = versionedNamespace.substring(0, versionSeparator);
            String[] versionParts = versionedNamespace.substring(versionSeparator + 1).split("\\.");
            major = Integer.parseInt(versionParts[0]);
            minor = Integer.parseInt(versionParts[1]);
         } else {
            namespace = versionedNamespace;
            major = 0;
            minor = 0;
         }
      }

      public boolean since(Schema schema) {
         return (schema == null) || (this.major > schema.major) || ((this.major == schema.major) && (this.minor >= schema.minor));
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Schema schema = (Schema) o;

         if (major != schema.major) return false;
         if (minor != schema.minor) return false;
         if (!namespace.equals(schema.namespace)) return false;
         return name.equals(schema.name);
      }

      @Override
      public int hashCode() {
         int result = namespace.hashCode();
         result = 31 * result + name.hashCode();
         result = 31 * result + major;
         result = 31 * result + minor;
         return result;
      }
   }

   private final Map<String, Document> xmls = new LinkedHashMap<>(32);
   private final Map<String, Schema> latestSchemas = new LinkedHashMap<>(32);
   private final Collection<Schema> skipSchemas = new HashSet<>(32);

   private final Transformer xslt;
   private final DocumentBuilder docBuilder;
   private final Document indexDoc;
   private final Element indexRoot;
   private final TransformerFactory factory;

   XSDoc() throws Exception {
      factory = TransformerFactory.newInstance();
      factory.setURIResolver((href, base) -> {
         Document doc = xmls.get(ToolUtils.getBaseFileName(href));
         if (doc != null) {
            return new DOMSource(doc);
         } else {
            return null;
         }
      });
      ClassLoader cl = XSDoc.class.getClassLoader();
      try (InputStream xsl = cl.getResourceAsStream("xsd/xsdoc.xslt")) {
         xslt = factory.newTransformer(new StreamSource(xsl));
      }
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      docBuilder = dbf.newDocumentBuilder();
      indexDoc = docBuilder.newDocument();
      indexRoot = indexDoc.createElement("files");
      indexDoc.appendChild(indexRoot);
   }

   void load(String fileName, boolean skipTransform) throws Exception {
      Document doc = docBuilder.parse(new File(fileName));
      String name = ToolUtils.getBaseFileName(fileName);
      xmls.put(name, doc);
      Schema schema = new Schema(doc, name);
      Schema current = latestSchemas.get(schema.namespace);
      if (schema.since(current)) {
         latestSchemas.put(schema.namespace, schema);
      }
      if (skipTransform) {
         skipSchemas.add(schema);
      }
   }

   private void transform(String name, Document doc, File outputDir) {
      try {
         xslt.transform(new DOMSource(doc), new StreamResult(new File(outputDir, name + ".html")));
         Element item = indexDoc.createElement("file");
         item.setAttribute("name", name + ".html");
         String ns = getDocumentNamespace(doc);
         item.setAttribute("ns", ns);
         indexRoot.appendChild(item);
      } catch (TransformerException e) {
         throw new RuntimeException(e);
      }
   }

   public static String getDocumentNamespace(Document doc) {
      Node child = doc.getFirstChild();
      while (!(child instanceof Element)) child = child.getNextSibling();
      return ((Element) child).getAttribute("targetNamespace");
   }

   void transformAll(File outputDir) {
      latestSchemas.values().stream()
            .filter(schema -> !skipSchemas.contains(schema))
            .forEach(schema -> transform(schema.name, schema.doc, outputDir));
   }

   private void generateIndex(File outputDir) throws Exception {
      ToolUtils.printDocument(indexDoc, System.out);
      ClassLoader cl = XSDoc.class.getClassLoader();
      try (InputStream xsl = cl.getResourceAsStream("xsd/index.xslt")) {
         Transformer indexXSLT = factory.newTransformer(new StreamSource(xsl));
         indexXSLT.transform(new DOMSource(indexDoc), new StreamResult(new File(outputDir, "index.html")));
      }
   }


   public static void main(String[] argv) throws Exception {
      XSDoc xsDoc = new XSDoc();
      String outputDir = System.getProperty("user.dir");
      Collection<String> skipSchema = new HashSet<>(32);
      Getopt opts = new Getopt("xsdoc", argv, "o:s:");
      for (int opt = opts.getopt(); opt > -1; opt = opts.getopt()) {
         switch (opt) {
            case 'o':
               outputDir = opts.getOptarg();
               break;
            case 's':
               skipSchema.add(ToolUtils.getBaseFileName(opts.getOptarg()));
               break;
         }
      }
      File outDir = new File(outputDir);
      outDir.mkdirs();
      for (int i = opts.getOptind(); i < argv.length; i++) {
         xsDoc.load(argv[i], skipSchema.contains(ToolUtils.getBaseFileName(argv[i])));
      }
      xsDoc.transformAll(outDir);
      xsDoc.generateIndex(outDir);
   }

}
