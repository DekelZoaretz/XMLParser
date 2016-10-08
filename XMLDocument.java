package "YOUR_PACKAGE_NAME";

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.EncodingInfo;
import org.apache.xml.serialize.LineSeparator;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class XMLDocument {

	private String fileName;
	private Document document; 
	private boolean saveFlag = true;
	private int type;

	public static final int STRING_TYPE = 1;
	public static final int FILE_TYPE = 2;
	public static final String DEFAULT_ENCODING = "Cp1252";
	public static final String DEFAULT_XML_ENCODING = "WINDOWS-1252";
	
	private String encoding = DEFAULT_ENCODING;
	private String xmlEncoding = DEFAULT_XML_ENCODING;
	
	public XMLDocument(String fileName) throws Exception{
		this.fileName = fileName;
		
		File file = new File(fileName);
		if(file.exists()){
		    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder;
		    try {
		    	builder = factory.newDocumentBuilder();
				document = builder.parse(fileName);
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception(e);
			}
		}else{
			throw new Exception("Can't find file: " + fileName);
		}
		
	}
	
	public XMLDocument(String fileName, String encoding) throws Exception{
		this(fileName, encoding, FILE_TYPE);
	}
	
	public XMLDocument(String fileName, String encoding, int type) throws Exception{
		reload(fileName, encoding, type);
	}
	
	public XMLDocument(String fileName, String encoding, String xmlEncoding, int type) throws Exception{
		this.xmlEncoding = xmlEncoding;
		reload(fileName, encoding, type);
	}
	
	public void reload(String fileName, String encoding, int type) throws Exception{
		this.fileName = fileName;
		this.type = type;
		this.encoding = encoding;
		Reader reader = null;
		if(type == FILE_TYPE){
			File file = new File(fileName);
			if(file.exists()){
			    try {
				    FileInputStream in = new FileInputStream(file);
				    reader = new BufferedReader(new InputStreamReader(in, "UTF8"));
				    //check if should skip UTF8 BOM
					char buffer[] = new char[1];
					reader.mark(1);
					int length = reader.read(buffer);

					if (length != 1 || buffer[0] != '\uFEFF')//BOM char
					{
						//if there is no UTF8 BOM - start reading stream from beginning (don't skip first char) 
						reader.reset();
						if (encoding != "UTF8")
						{
							reader.close();
							in.close();
							in = new FileInputStream(file);
							reader = new BufferedReader(new InputStreamReader(in, encoding));
						}
					}

			    } catch (Exception e) {
					throw new Exception(e);
				}
			}else{
				throw new Exception("Can't find file: " + fileName);
			}
		}else{			
			setSaveFlag(false);
			reader = new StringReader(fileName);
		}
		
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		    InputSource source = new InputSource(reader);
		    document = builder.parse(source);
		} catch (Exception e) {
			throw new Exception("Can't find file: " + fileName);
		}
		finally
		{
			if (reader!=null)
				try{reader.close();}
				catch (Exception e){}
		}
	}
	
	public void setSaveFlag(boolean flag){
		this.saveFlag = flag;
	}
	
	public Document getDocument(){
		return document;
	}
	
	public void saveDocument(){
		if(document != null){
			OutputFormat format = new OutputFormat(document);
			if (xmlEncoding != null){
				format.setEncoding(xmlEncoding);
			}
			format.setLineSeparator(LineSeparator.Windows);
			format.setIndenting(true);
			format.setIndent(2);
			XMLSerializer serializer;
			try {
				OutputStreamWriter fw;
				fw = new OutputStreamWriter(new FileOutputStream(fileName), encoding);
				serializer = new XMLSerializer (fw, format);
				serializer.asDOMSerializer();
				serializer.serialize(document);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void saveAs(String filePath){
		saveAs(filePath, encoding, xmlEncoding);
	}
	
	public void saveAs(String filePath, String aEncoding, String aXmlEncoding){
		if(document != null){
			xmlEncoding = aXmlEncoding;
			encoding = aEncoding;
			OutputFormat format = new OutputFormat(document);
			if (xmlEncoding != null){
				format.setEncoding(xmlEncoding);
			}
			format.setLineSeparator(LineSeparator.Windows);
			format.setIndenting(true);
			format.setIndent(2);
			XMLSerializer serializer;
			try {
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(filePath), encoding);
				//write UTF8 BOM
				if (encoding == "UTF8")
					fw.write('\ufeff');
				
				serializer = new XMLSerializer (fw, format);
				serializer.asDOMSerializer();
				serializer.serialize(document);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getXML(){
		String xml = null;
		if(document != null){
			OutputFormat format = new OutputFormat(document);
			if (xmlEncoding != null){
				format.setEncoding(xmlEncoding);
			}
			format.setLineSeparator(LineSeparator.Windows);
			format.setLineWidth(65);
	        format.setIndenting(true);
	        format.setIndent(2);
//			format.setIndenting(true);
//			format.setLineWidth(0);             
//			format.setPreserveSpace(true);
			XMLSerializer serializer;
			try {
				ByteArrayOutputStream bo = new ByteArrayOutputStream();
				OutputStreamWriter fw = new OutputStreamWriter(bo, encoding);
				serializer = new XMLSerializer (fw, format);
				serializer.asDOMSerializer();
				serializer.serialize(document);
				xml = new String(bo.toByteArray(), encoding);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return xml;
	}
	
	// for now without encoding
	static public String getXML(Document document, Element elem){
		String encoding = "UTF-8";
		String xmlEncoding = "UTF-8";
		
		String xml = null;
		if(document != null){
			OutputFormat format = new OutputFormat(document);
			if (xmlEncoding != null){
				format.setEncoding(xmlEncoding);
			}
			format.setLineSeparator(LineSeparator.Windows);
			format.setLineWidth(65);
	        format.setIndenting(true);
	        format.setIndent(2);
	        format.setOmitXMLDeclaration(true);
			XMLSerializer serializer;
			try {
				ByteArrayOutputStream bo = new ByteArrayOutputStream();
				OutputStreamWriter fw = new OutputStreamWriter(bo, encoding);
				serializer = new XMLSerializer (fw, format);
				serializer.asDOMSerializer();
				serializer.serialize(elem);
				xml = bo.toString(encoding);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return xml;
	}
	
	public List<Node> selectNodes(String xpathStr){
		XPath xpath;
		List<Node> results = null;
		try {
			xpath = new DOMXPath(xpathStr);
			results = xpath.selectNodes(document);
		} catch (JaxenException e) {
			e.printStackTrace();
		}
		
        return results;
	}
	
	public Node selectSingleNode(String xpath){
		Node retVal = null;
		List <Node> results = selectNodes(xpath);
		if(results != null && results.size() > 0)
			retVal = (Node)results.get(0);
		return retVal;
	}
	
	public String selectSingleValue(String xpathStr){
		Node node = null;
		String retVal = null;
		node = (Node)selectSingleNode(xpathStr);
		if(node != null && node.getNodeType() == Node.ATTRIBUTE_NODE)
			retVal = node.getNodeValue();
		else if(node != null && node.getNodeType() == Node.ELEMENT_NODE){
			Node valueNode = node.getFirstChild();
			if(valueNode != null){
				if(valueNode.getNodeType() == Node.TEXT_NODE)
					retVal = valueNode.getNodeValue();
				else if(valueNode.getNodeType() == Node.CDATA_SECTION_NODE)
					retVal = ((CDATASection)valueNode).getData();
			}
		}
		return retVal;
	}
	
	private void createDocument(){
	    try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public void setValue(String xpath, String value){
		Node node = selectSingleNode(xpath);
		if(node != null){
			if(node.getNodeType() == Node.ATTRIBUTE_NODE)
				node.setNodeValue(value);
			else if(node.getNodeType() == Node.ELEMENT_NODE){
				if(node.getFirstChild() != null){
					node.removeChild(node.getFirstChild());
				}
				node.appendChild(document.createTextNode(value));
			}
			if(saveFlag)
				saveDocument();	
		}
	}
	
	public void setValues(String xpath, String value){
		List<Node> nodes = selectNodes(xpath);
		if(nodes != null){
			for(int i = 0 ; i < nodes.size() ; ++i){
				if(nodes.get(i).getNodeType() == Node.ATTRIBUTE_NODE)
					nodes.get(i).setNodeValue(value);
				else if(nodes.get(i).getNodeType() == Node.ELEMENT_NODE){
					if(nodes.get(i).getFirstChild() != null){
						nodes.get(i).removeChild(nodes.get(i).getFirstChild());
					}
					nodes.get(i).appendChild(document.createTextNode(value));
				}
			}
			if(saveFlag)
				saveDocument();
		}
	}
	
	public void setValuesAsCDATA(String xpath, String value){
		List<Node> nodes = selectNodes(xpath);
		if(nodes != null){
			for(int i = 0 ; i < nodes.size() ; ++i){
				if(nodes.get(i).getNodeType() == Node.ELEMENT_NODE){
					if(nodes.get(i).getFirstChild() != null){
						nodes.get(i).removeChild(nodes.get(i).getFirstChild());
					}
					nodes.get(i).appendChild(document.createCDATASection(value));
				}
			}
			if(saveFlag)
				saveDocument();
		}
	}
	
	public void removeElement(String xpath){
		Node node = selectSingleNode(xpath);
		if(node != null){
			Node parent = node.getParentNode();
			parent.removeChild(node);
			if(saveFlag)
				saveDocument();
		}
	}
	
	public void removeElements(String xpath){
		List nodes = selectNodes(xpath);
		if(nodes != null){
			Iterator it = nodes.iterator();
			while(it.hasNext()){
				Node node = (Node)it.next();
				Node parent = node.getParentNode();
				parent.removeChild(node);
			}
			if(saveFlag)
				saveDocument();
		}
	}
	
	public Element addElement(String xpath, String elemName){
		Node node = selectSingleNode(xpath);
		Element elem = null;
		if(node != null){
			if(node.getNodeType() == Node.ELEMENT_NODE){
				elem = document.createElement(elemName);
				node.appendChild(elem);	
			}
			if(saveFlag)
				saveDocument();	
		}
		return elem;
	}
	
	public Comment addComment(String xpath, String comment){
		Node node = selectSingleNode(xpath);
		Comment comt = null;
		if(node != null){
			if(node.getNodeType() == Node.ELEMENT_NODE){
				comt = document.createComment(comment);
				node.appendChild(comt);	
			}
			if(saveFlag)
				saveDocument();	
		}
		return comt;
	}
	
	public Element addElement(String xpath, String elemName, String attrName, String attrValue){
		Element elem = addElement(xpath, elemName);
		if(elem != null){
			elem.setAttribute(attrName, attrValue);
		}
		return elem;
	}
	
	public void addAttribute(String xpath, String attrName, String attrValue){
		Node node = selectSingleNode(xpath);
		if(node != null){
			if(node.getNodeType() == Node.ELEMENT_NODE){
				Element elem = (Element)node;
				elem.setAttribute(attrName, attrValue);
			}
			if(saveFlag)
				saveDocument();	
		}
	}
	
	public Element createElement(Element parent, String elementName){
		Element elem = document.createElement(elementName);
		parent.appendChild(elem);
		return elem;
	}
	
	public CDATASection createCDATASection(Element parent, String value){
		CDATASection section = document.createCDATASection(value);
		parent.appendChild(section);
		return section;
	}
	
	public static String toXml(Element element){
		String xmlEncoding = element.getOwnerDocument().getXmlEncoding();
		String encoding = element.getOwnerDocument().getInputEncoding();
		if(xmlEncoding == null)
			xmlEncoding = DEFAULT_XML_ENCODING;
		if(encoding == null)
			encoding = DEFAULT_ENCODING;
		
		String xml = "";
		OutputFormat format = new OutputFormat();
		format.setLineSeparator(LineSeparator.Unix);
		format.setIndenting(true);
		format.setLineWidth(0);             
		format.setPreserveSpace(true);
		format.setOmitXMLDeclaration(true);
		format.setEncoding(xmlEncoding);
		XMLSerializer serializer;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStreamWriter fw = new OutputStreamWriter(out, encoding);
			
			StringWriter sw = new StringWriter();
			serializer = new XMLSerializer (fw, format);
			serializer.asDOMSerializer();
			serializer.serialize(element);
			
			byte []arr = out.toByteArray();
			xml = new String(arr, encoding);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return xml;
	}
	
	static public Node getRealFirstChild(Node node) {
		NodeList list = node.getChildNodes(); 
		for (int i = 0; i < list.getLength(); ++i)
		{
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
				return list.item(i);
		}
		return null;
	}
	
	static public Node getXDataChild(Node node) {
		NodeList list = node.getChildNodes(); 
		for (int i = 0; i < list.getLength(); ++i)
		{
			if (list.item(i).getNodeType() == Node.CDATA_SECTION_NODE)
				return list.item(i);
		}
		return null;
	}
	
	public static String escapeXml(String str)
	{
	   StringBuilder sb = new StringBuilder();
	   for(int i = 0; i < str.length(); i++){
	      char c = str.charAt(i);
	      switch(c){
	      case '<': sb.append("&lt;"); break;
	      case '>': sb.append("&gt;"); break;
	      case '\"': sb.append("&quot;"); break;
	      case '&': sb.append("&amp;"); break;
	      case '\'': sb.append("&apos;"); break;
	      default:
	         if(c>0x7e) {
	            sb.append("&#"+((int)c)+";");
	         }else
	            sb.append(c);
	      }
	   }
	   return sb.toString();
	}
}

