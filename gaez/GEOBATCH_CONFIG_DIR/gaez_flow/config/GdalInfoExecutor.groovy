import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import it.geosolutions.geobatch.tools.filter.FreeMarkerFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GdalInfoExecutor {
	final static Logger LOGGER = Logger.getLogger(GdalInfoExecutor.class
			.toString());

	public static Document parser(String xmlString){
//		System.out.print("Parsing:"+xmlString);
		
			final DocumentBuilderFactory factory =
			   DocumentBuilderFactory.newInstance();
			final InputSource source = new InputSource(new StringReader(xmlString));
			try {
				final DocumentBuilder docBuilder=factory.newDocumentBuilder();
				final Document document =docBuilder.parse(source);
				return document;
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			finally {
				
			}
			return null;
	}
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final File file=new File("//media/bigshare/data/ciok/gaez/faocmb00.rst");
		final String[] cmd=buildCommand(file);
		final String res = exec(cmd);
		
//		LOGGER.log(Level.ALL, res);
//		System.out.println(res);
		
		final String xml=res.substring(res.indexOf("<"));
		FileReader fr=new FileReader(new File("src/main/resources/transform.xsl"));
		FreeMarkerFilter f=new FreeMarkerFilter(".",fr);
		
		FileWriter fw=new FileWriter(new File("src/main/resources/transform_out.xsl"));
		try {
			Document doc=parser(xml);
			TemplateModel model=f.wrapRoot(doc);
			
			f.process(model, fw);
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TemplateModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		fr.close();
		fw.flush();
		fw.close();

	}

	/**
	 * 
	 * @param url
	 * @param user
	 * @param pass
	 * @param instrID
	 * @param link
	 * @param type
	 *            (can be):<br>
	 *            cruises instruments
	 * @return
	 */
	public static String[] buildCommand(final File file) {
		final String[] command = new String[2];
		command[0] = "gdalinfo";
		command[1] = file.getAbsolutePath();
		LOGGER.log(Level.ALL, "COMMAND: " + command[0] + " " + command[1]);
		return command;
	}

	public static String exec(final String[] command) {
		final ProcessBuilder pb = new ProcessBuilder(command);
		BufferedReader input = null;
		BufferedReader output = null;
		try {
			final Process proc = pb.start();

			// read error messages
			String errorsLine;
			input = new BufferedReader(new InputStreamReader(
					proc.getErrorStream()));
			final StringBuffer errors = new StringBuffer();
			try {
				while ((errorsLine = input.readLine()) != null)
					errors.append(errorsLine);
			} catch (IOException e) {
				return e.getLocalizedMessage();
			} finally {
				try {
					if (input != null)
						input.close();
				} catch (Throwable t) {
				}
			}

			// output
			output = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			final StringBuffer stdout = new StringBuffer();
			String outputLine;
			try {
				
				while ((outputLine = output.readLine()) != null)
					stdout.append(outputLine);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (output != null)
						output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			final int result = proc.waitFor();

			if (checkStatus(result))
				return stdout.toString();
			else
				return errors.toString();

		} catch (IOException e) {
			return e.getLocalizedMessage();
		} catch (InterruptedException e) {
			return e.getLocalizedMessage();
		} finally {

		}
	}

	/**
	 * if status is good returns true
	 * 
	 * @param ret
	 * @return
	 */
	public static boolean checkStatus(final int ret) {
		switch (ret) {
		case 0:
			return true; // OK
		default:
			return false;
		}

	}

} 
