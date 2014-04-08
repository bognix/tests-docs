import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.standard.Standard;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

 /*
 * @author Bogna 'bognix' Knychala
 */
public class ClassesAndMethodsToXML extends Standard {

	private static String fileName = "classesAndMethods.xml";
	private static String testAnnotationName = "Test";
	private static String ownershipTag = "@ownership";

	public static boolean start(RootDoc root) {
		String path = createPathForOutputBasedOnCommandLineOptions(root.options());
		createXML(root.classes(), path);
		return true;
	}

	public static boolean validOptions(String[][] options, DocErrorReporter reporter){
		return true;
	}

	private static String createPathForOutputBasedOnCommandLineOptions(String[][] options) {
		String targetDir = null;
		for (int i=0; i<options.length; i++) {
			if (options[i][0].equals("-d")) {
				targetDir = options[i][1];
			}
		}

		if (targetDir == null) {
			try {
				//In case output dir was not provided create file in current location
				targetDir = new File( "." ).getCanonicalPath();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		//In case output dir doesn't exist - create
		if (!new File(targetDir).exists()) {
			new File(targetDir).mkdirs();
		}

		return targetDir + File.separator + fileName;
	}

	private static void createXML(ClassDoc[] classes, String outputPath) {
		Document doc = buildDocument();
		Element rootClassesElement = doc.createElement("classes");
		doc.appendChild(rootClassesElement);
		Element classNode;

		//For all classes in input
		for (int i=0; i < classes.length; i++) {
			List<String> interestingMethods = extractInterestingMethods(classes[i]);
			//If class has methods with testAnnotationName
			if (interestingMethods.size() > 0) {
				//Create Element for class and append to document
				classNode = createClassNode(doc, classes[i], interestingMethods);
				rootClassesElement.appendChild(classNode);
			}
		}

		//save XML file to provided path
		writeContentToXMLFile(doc, outputPath);
	}


	private static Document buildDocument() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		Document doc = docBuilder.newDocument();
		return doc;
	}

	private static List<String> extractInterestingMethods(ClassDoc classDoc) {
		List<String> interestingMethods = new ArrayList<>();
		//Get all methods
		MethodDoc[] methods = classDoc.methods();
		//For each method
		for (int j=0; j < methods.length; j++) {
			//Get annotations
			AnnotationDesc[] a = methods[j].annotations();
			if (a.length > 0) {
				for (int k=0; k < a.length; k++) {
					//Check if method has wanted annotation
					if (a[k].annotationType().toString().equals(testAnnotationName)) {
						if (!interestingMethods.contains(methods[j].name())) {
							interestingMethods.add(methods[j].name());
						}
					}
				}
			}
		}
		return interestingMethods;
	}

	private static Element createClassNode(
			Document document, ClassDoc classDoc, List<String> methodsInClass
	) {
		Element classNode = document.createElement("class");
		classNode.setAttribute("name", classDoc.name());
		classNode = setOwnership(classDoc.tags(), classNode);
		Element methodsNode = document.createElement("methods");
		for (String method: methodsInClass) {
			Element methodNode = document.createElement("method");
			methodNode.appendChild(document.createTextNode(method));
			methodsNode.appendChild(methodNode);
		}
		classNode.appendChild(methodsNode);
		return classNode;
	}

	private static Element setOwnership(Tag[] tags, Element classNode) {
		boolean ownershipSet = false;

		for (Tag tag: tags) {
			if (tag.name().equals(ownershipTag)) {
				classNode.setAttribute("ownership", tag.text());
				ownershipSet = true;
			}
		}

		if (!ownershipSet) {
			classNode.setAttribute("ownership", "");
		}
		return classNode;
	}

	private static void writeContentToXMLFile(Document doc, String path) {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		DOMSource source = new DOMSource(doc);

		StreamResult result = new StreamResult(
			new File(path)
		);
		try {
			transformer.transform(source, result);
		} catch (TransformerException ex) {
			throw new RuntimeException(ex);
		}
		System.out.println("File saved!");
		System.out.println("Location: " + path);
	}
}
