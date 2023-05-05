package io.github.vdaburon.jmeter.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.gitlab4j.api.models.WikiAttachment;
import org.gitlab4j.api.models.WikiPage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 
 * @author Vincent Daburon
 */
public class WikiPageGenerator {
	// CRLF ou LF ou CR
	public static final String LINE_SEP = System.getProperty("line.separator");
	private static final Logger LOGGER = Logger.getLogger(WikiPageGenerator.class.getName());
	public static final int K_RETURN_OK = 0;
	public static final int K_RETURN_KO = 1;
	public static final int K_IMAGE_WIDTH_DEFAULT = 950;

	// GitlabUrl, e.g : https://mygitlab.com
	public static final String K_GITLAB_URL_OPT = "gitlabUrl";

	// Gitlab project ID
	public static final String K_PROJECT_ID_OPT = "projectId";

	// Gitlab access token relate to the project
	public static final String K_ACCESS_TOKEN_OPT = "accessToken";

	// directory to find files to process
	public static final String K_DIR_WITH_FILE_OPT = "dirWithFile";

	// for image, the width in pixel, e.g : 950
	public static final String K_IMAGE_WIDTH_OPT = "imageWidth";

	// wiki page title for the new wiki page
	public static final String K_PAGE_TITLE_OPT = "pageTitle";

	public static void main(String[] args) {
		long startTimeMs = System.currentTimeMillis();

		Options options = createOptions();
		Properties parseProperties = null;

		try {
			parseProperties = parseOption(options, args);
		} catch (ParseException ex) {
			helpUsage(options);
			System.exit(K_RETURN_KO);
		}
		int exitReturn = K_RETURN_KO;

		String gitlabUrl = "NOT SET";
		String projectId = "NOT SET";
		String accessToken = "NOT SET";
		String dirWithFiles = "NOT SET";
		String pageTitle = "";

		String sTmp;
		sTmp = (String) parseProperties.get(K_GITLAB_URL_OPT);
		if (sTmp != null) {
			gitlabUrl = sTmp;
		}

		sTmp = (String) parseProperties.get(K_PROJECT_ID_OPT);
		if (sTmp != null) {
			projectId = sTmp;
		}

		sTmp = (String) parseProperties.get(K_ACCESS_TOKEN_OPT);
		if (sTmp != null) {
			accessToken = sTmp;
		}

		sTmp = (String) parseProperties.get(K_DIR_WITH_FILE_OPT);
		if (sTmp != null) {
			dirWithFiles = sTmp;
		}

		sTmp = (String) parseProperties.get(K_PAGE_TITLE_OPT);
		if (sTmp != null) {
			pageTitle = sTmp;
		}

		int imageWidth = K_IMAGE_WIDTH_DEFAULT;
		sTmp = (String) parseProperties.get(K_IMAGE_WIDTH_OPT);
		if (sTmp != null) {
			try {
				imageWidth = Integer.parseInt(sTmp);
				LOGGER.fine("imageWidth : " + sTmp);
			} catch (NumberFormatException ex) {
				LOGGER.warning("Can't parse this integer : <" + sTmp + ">, default imageWidth value = " + K_IMAGE_WIDTH_DEFAULT);
			}
		}

		LOGGER.info("Parameters : " + parseProperties);
		try {
			GitlabWikiApi gitlabWikiApi = new GitlabWikiApi(gitlabUrl, projectId, accessToken);
			// gitlabWikiApi.getGitLabApi().enableRequestResponseLogging(LOGGER, java.util.logging.Level.INFO);
			gitlabWikiApi.initConnexion();

			String contentPage = createWikiPage(gitlabWikiApi, pageTitle, dirWithFiles, imageWidth);
			LOGGER.fine("contentPage=\n" + contentPage);

		} catch (Exception ex) {
			LOGGER.warning(ex.toString());
			exitReturn = K_RETURN_KO;
		}
		exitReturn = K_RETURN_OK;

		long endTimeMs = System.currentTimeMillis();
		LOGGER.info("Duration ms =" + (endTimeMs - startTimeMs));
		LOGGER.info("End main (exit " + exitReturn + ")");

		System.exit(exitReturn);
	}

	/**
	 * Create a new Gitlab Wiki page from files in a directory, upload files and create a partial html page
	 * @param gitlabWikiApi a connexion to the Gitlab wiki
	 * @param pageTitle the page title of the new wiki page
	 * @param dirWithFiles directory to find files to process
	 * @param imageWidth for images the image width
	 * @return the content of the wiki page created
	 */
	public static String createWikiPage(GitlabWikiApi gitlabWikiApi, String pageTitle, String dirWithFiles, int imageWidth) {
		File fDirWithFiles = new File(dirWithFiles);
		
		Object[] tabFiles = listFileOrderByName(fDirWithFiles);

		if (tabFiles.length == 0) {
			return "Warning NO file in directory : " + dirWithFiles;
		}
		
		StringBuilder sb = new StringBuilder(64 * 1024);
		try {
			LOGGER.info("Generating gitlab wiki page ...");

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			String sDate = dateFormat.format(new Date());

			sb.append("<h1> Generated date " + sDate + "</h1><br/>");
			sb.append(LINE_SEP);

			String pageTitleWikiPage = "Page wiki generated " + sDate;
			if (pageTitle !=  null && pageTitle.length() > 1) {
				pageTitleWikiPage = pageTitle;
			}
			WikiPage wikipage = gitlabWikiApi.createWikiPage(pageTitleWikiPage, sb.toString());

			int nbFiles = tabFiles.length;
			for (int i = 0; i < nbFiles; i++) {
				File f = (File) tabFiles[i];

				String name = f.getName().toLowerCase();
				LOGGER.info("File number : " + (i + 1) + "/" + nbFiles + ", process the file=" + f.getName());

				if (name.endsWith("csv") || name.endsWith("jtl") || name.endsWith("xml") ||name.endsWith("gz")  || name.endsWith("zip") || name.endsWith("log")
						|| name.endsWith("gif") || name.endsWith("png") || name.endsWith("bmp") || name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("html")) {
					
					// folderRead = c:\dir1\dir2\dirIn, f =  c:\dir1\dir2\dirIn\logo.gif => nameRelative = logo.gif (remove the folderRead path)
					String nameRelative = f.getCanonicalPath().substring(fDirWithFiles.getCanonicalPath().length() + 1);

					sb.append("<h2>" + nameRelative + "</h2><br/>");
					sb.append(LINE_SEP);

					if (name.endsWith("csv") || name.endsWith("jtl") || name.endsWith("xml") ||name.endsWith("gz") || name.endsWith("zip") || name.endsWith("log")) {
						long lengthBytes = f.length();
						DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
						symbols.setGroupingSeparator(' ');

						DecimalFormat formatter = new DecimalFormat("###,###.##", symbols);

						WikiAttachment attach = gitlabWikiApi.uploadAttachment(f);
						// link to the file
						sb.append("<a href='" + attach.getLink().getUrl() + "'>" + nameRelative + "</a>&nbsp;&nbsp;&nbsp;File size=" + formatter.format(lengthBytes) + " Bytes<br/><br/>");
					}
										
					if (name.endsWith("gif") || name.endsWith("png") || name.endsWith("bmp") || name.endsWith("jpg") || name.endsWith("jpeg")) {
						// image
						WikiAttachment attach = gitlabWikiApi.uploadAttachment(f);
						sb.append("<img src='" + attach.getLink().getUrl() + "' width=\"" + imageWidth + "\"><br/><br/>");
						sb.append(LINE_SEP);
					}
				
					if (name.endsWith("html") && !name.equals("index.html")) {
						// include the html content directly in the result for the Synthesis Report, Aggregate Report, Summary Report
						String htmlPage = readAllContentFileToString(f.getAbsolutePath());
						// remove css declaration, get only table
						String htmlTableNoCssTmp = substringBetween(htmlPage, "<table class=\"table_jp\">", "</table>");
						String htmlTableNoCss = "<table>" + htmlTableNoCssTmp + "</table>";
						sb.append(htmlTableNoCss);
						sb.append(LINE_SEP);
					}
				} // end all endsWith if
			} // end for

			WikiPage wikipageFinal = gitlabWikiApi.updateWikiPage(wikipage.getSlug(),wikipage.getTitle(), sb.toString());
			LOGGER.info("Done! Page created : " + wikipageFinal.getTitle());

		} catch (final Exception ex) {
			LOGGER.warning(ex.getMessage());
		}
		return sb.toString();
	}

	/**
	 * Recursively find files in a directory and sub directory and sort files by name and directory depth
	 * @param folderRead directory where to start to find files
	 * @return a array of Files
	 */
	@SuppressWarnings("unchecked")
	public static Object[] listFileOrderByName(File folderRead) {
		ArrayList<File> aList = new ArrayList<File>();
		
		findAllFilesInDirectory(folderRead, aList);
		Object[] tabFiles = aList.toArray();
		
		Arrays.sort(tabFiles , new FileNameComparator());
		return tabFiles;
	}

	/**
	 * Read the content of a text file and save it in a String
	 * @param fileName text file to read
	 * @return the content of the text file
	 */
	private static String readAllContentFileToString(String fileName) {
		String content = "";

		try {
			content = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return content;
	}

	/**
	 * Find all files in a Directory recursively
	 * @param directory the directory to start find files
	 * @param files all files in the directory and sub directory
	 */
	public static void findAllFilesInDirectory(File directory, List<File> files) {
	    // Get all files from a directory.
	    File[] fList = directory.listFiles();
	    if(fList != null) {
	        for (File file : fList) {      
	            if (file.isFile()) {
	                files.add(file);
	            } else if (file.isDirectory()) {
	            	findAllFilesInDirectory(file, files);
	            }
	        }
	    }
	}

	/**
	 * Find a string between 2 strings
	 * @param str the initial string
	 * @param open the string begin
	 * @param close the string end
	 * @return the string between this 2 strings (remove open and end content)
	 */
	public static String substringBetween(final String str, final String open, final String close) {
		if (str == null || open == null || close == null) {
			return null;
		}
		final int start = str.indexOf(open);
		if (start != -1) {
			final int end = str.indexOf(close, start + open.length());
			if (end != -1) {
				return str.substring(start + open.length(), end);
			}
		}
		return null;
	}

	/**
	 * If incorrect parameter or help, display usage
	 * @param options  options and cli parameters
	 */
	private static void helpUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		String footer = "Ex : java -jar create-gitlab-wiki-page-for-files-in-directory-<version>-jar-with-dependencies.jar -" + K_GITLAB_URL_OPT + " https://mygitlab.com  -"
				+ K_PROJECT_ID_OPT + " MyProjectId_12007 -"  + K_ACCESS_TOKEN_OPT + " MyToken_44dPkJ007 -" + K_DIR_WITH_FILE_OPT + " results -" + K_PAGE_TITLE_OPT + " \"load test 05_03_2023 13h34m\" -" + K_IMAGE_WIDTH_OPT + " 950\n";
		formatter.printHelp(120, WikiPageGenerator.class.getName(),
				WikiPageGenerator.class.getName(), options, footer, true);
	}

	/**
	 * Parse options enter in command line interface
	 * @param optionsP parameters to parse
	 * @param args parameters from cli
	 * @return properties saved
	 * @throws ParseException parsing error
	 * @throws MissingOptionException mandatory parameter not set
	 */
	private static Properties parseOption(Options optionsP, String[] args) throws ParseException, MissingOptionException {

		Properties properties = new Properties();

		CommandLineParser parser = new DefaultParser();

		// parse the command line arguments

		CommandLine line = parser.parse(optionsP, args);

		if (line.hasOption("help")) {
			properties.setProperty("help", "help value");
			return properties;
		}

		if (line.hasOption(K_GITLAB_URL_OPT)) {
			properties.setProperty(K_GITLAB_URL_OPT, line.getOptionValue(K_GITLAB_URL_OPT));
		}

		if (line.hasOption(K_PROJECT_ID_OPT)) {
			properties.setProperty(K_PROJECT_ID_OPT, line.getOptionValue(K_PROJECT_ID_OPT));
		}

		if (line.hasOption(K_ACCESS_TOKEN_OPT)) {
			properties.setProperty(K_ACCESS_TOKEN_OPT, line.getOptionValue(K_ACCESS_TOKEN_OPT));
		}

		if (line.hasOption(K_DIR_WITH_FILE_OPT)) {
			properties.setProperty(K_DIR_WITH_FILE_OPT, line.getOptionValue(K_DIR_WITH_FILE_OPT));
		}

		if (line.hasOption(K_IMAGE_WIDTH_OPT)) {
			properties.setProperty(K_IMAGE_WIDTH_OPT, line.getOptionValue(K_IMAGE_WIDTH_OPT));
		}

		if (line.hasOption(K_PAGE_TITLE_OPT)) {
			properties.setProperty(K_PAGE_TITLE_OPT, line.getOptionValue(K_PAGE_TITLE_OPT));
		}

		return properties;
	}

	/**
	 * Options or parameters for the command line interface
	 * @return all options
	 **/
	private static Options createOptions() {
		Options options = new Options();

		Option helpOpt = Option.builder("help").hasArg(false).desc("Help and show parameters").build();

		options.addOption(helpOpt);

		Option gitlabUrlOpt = Option.builder(K_GITLAB_URL_OPT).argName(K_GITLAB_URL_OPT)
				.hasArg(true)
				.required(true)
				.desc("Gitlab url (likes : https://mygitlab.com)")
				.build();
		options.addOption(gitlabUrlOpt);

		Option projectIdOpt = Option.builder(K_PROJECT_ID_OPT).argName(K_PROJECT_ID_OPT)
				.hasArg(true)
				.required(true)
				.desc("Project ID in Gitlab")
				.build();
		options.addOption(projectIdOpt);


		Option accessTokenOpt = Option.builder(K_ACCESS_TOKEN_OPT).argName(K_ACCESS_TOKEN_OPT)
				.hasArg(true)
				.required(true)
				.desc("Access Token for Http Header Authorization: Bearer <access token>")
				.build();
		options.addOption(accessTokenOpt);

		Option dirWithFileOpt = Option.builder(K_DIR_WITH_FILE_OPT).argName(K_DIR_WITH_FILE_OPT)
				.hasArg(true)
				.required(true)
				.desc("Directory that contains files to upload and create wiki page")
				.build();
		options.addOption(dirWithFileOpt);

		Option pageTitleOpt = Option.builder(K_PAGE_TITLE_OPT).argName(K_PAGE_TITLE_OPT)
				.hasArg(true)
				.required(false)
				.desc("Page title (must not exists before create this page) without '/' or '-' in the title, default \"Page generated + date time\" (e.g : 2023-05-04T18:19:46)")
				.build();
		options.addOption(pageTitleOpt);

		Option imagewidthOpt = Option.builder(K_IMAGE_WIDTH_OPT).argName(K_IMAGE_WIDTH_OPT)
				.hasArg(true)
				.required(false)
				.desc("Image witdh, default " + K_IMAGE_WIDTH_DEFAULT)
				.build();
		options.addOption(imagewidthOpt);

		return options;
	}
}

