
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * @author bognix
 */
public class ClassesAndMethodsToXML extends Standard {

	private static HashMap<String,List<String>> methodsAndClasses;
	private static String fileName = "classesAndMethods.xml";

	public static boolean start(RootDoc root) {
			String annotationName = "Test";
			methodsAndClasses = getTestMethodsAndClasses(root.classes(), annotationName);
			buildXML(methodsAndClasses, root.options());
			return true;
	}

	public static boolean validOptions(String[][] options, DocErrorReporter reporter){
		return true;
	}

	private static HashMap<String, List<String>> getTestMethodsAndClasses(
		ClassDoc[] classes, String annotationName
	) {
		HashMap<String, List<String>> interestingClasses = new HashMap<>();
		//For each class in file
		for (int i=0; i < classes.length; i++) {
			List<String> interestingMetods = new ArrayList<>();
			//Get all methods
			MethodDoc[] methods = classes[i].methods();
			//For each method
			for (int j=0; j < methods.length; j++) {
				//Get annotations
				AnnotationDesc[] a = methods[j].annotations();
				if (a.length > 0) {
					for (int k=0; k < a.length; k++) {
						//Check if method has wanted annotation
						if (a[k].annotationType().toString().equals(annotationName)) {
							if (!interestingMetods.contains(methods[j].name())) {
								interestingMetods.add(methods[j].name());
							}
						}
					}
				}
			}
			if (interestingMetods.size() > 0) {
				interestingClasses.put(classes[i].name(), interestingMetods);
			}
		}
		return interestingClasses;
	}

	private static void buildXML(HashMap<String, List<String>> input, String[][] options) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		Document doc = docBuilder.newDocument();
		Element rootClassesElement = doc.createElement("classes");
		doc.appendChild(rootClassesElement);
		for (String className: input.keySet()) {
			Element classNode = doc.createElement("class");
			classNode.setAttribute("name", className);
			List<String> methods = input.get(className);
			Element methodsNode = doc.createElement("methods");
			for (String method: methods) {
				Element methodNode = doc.createElement("method");
				methodNode.appendChild(doc.createTextNode(method));
				methodsNode.appendChild(methodNode);
			}
			classNode.appendChild(methodsNode);
			rootClassesElement.appendChild(classNode);
		}

		//Write content into file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		DOMSource source = new DOMSource(doc);

		//Check if fileName is provided
		String targetDir = "";
		String targetFile = "";
		for (int i=0; i<options.length; i++) {
			if (options[i][0].equals("-d")) {
				targetDir = options[i][1];
			}
		}
		if (targetDir.isEmpty()) {
			try {
				targetDir = new File( "." ).getCanonicalPath();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		if (!new File(targetDir).exists()) {
			new File(targetDir).mkdirs();
		}

		String output = targetDir + File.separator + fileName;
		StreamResult result = new StreamResult(
			new File(output)
		);
		try {
			transformer.transform(source, result);
		} catch (TransformerException ex) {
			throw new RuntimeException(ex);
		}
		System.out.println("File saved!");
		System.out.println("Location: " + output);
	}
}
