/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.build;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.gitblit.Constants;
import com.gitblit.utils.StringUtils;

/**
 * The Build class downloads runtime and compile-time jar files from the Apache
 * or Eclipse Maven repositories.
 * 
 * It also generates the Keys class from the gitblit.properties file.
 * 
 * Its important that this class have minimal compile dependencies since its
 * called very early in the build script.
 * 
 * @author James Moger
 * 
 */
public class Build {

	/**
	 * BuildType enumeration representing compile-time or runtime. This is used
	 * to download dependencies either for Gitblit GO runtime or for setting up
	 * a development environment.
	 */
	public static enum BuildType {
		RUNTIME, COMPILETIME;
	}

	public static void main(String... args) {
		runtime();
		compiletime();
		buildSettingKeys();
	}

	public static void runtime() {
		downloadFromApache(MavenObject.JCOMMANDER, BuildType.RUNTIME);
		downloadFromApache(MavenObject.JETTY, BuildType.RUNTIME);
		downloadFromApache(MavenObject.SERVLET, BuildType.RUNTIME);
		downloadFromApache(MavenObject.SLF4JAPI, BuildType.RUNTIME);
		downloadFromApache(MavenObject.SLF4LOG4J, BuildType.RUNTIME);
		downloadFromApache(MavenObject.LOG4J, BuildType.RUNTIME);
		downloadFromApache(MavenObject.WICKET, BuildType.RUNTIME);
		downloadFromApache(MavenObject.WICKET_EXT, BuildType.RUNTIME);
		downloadFromApache(MavenObject.WICKET_AUTH_ROLES, BuildType.RUNTIME);
		downloadFromApache(MavenObject.WICKET_GOOGLE_CHARTS, BuildType.RUNTIME);
		downloadFromApache(MavenObject.MARKDOWNPAPERS, BuildType.RUNTIME);
		downloadFromApache(MavenObject.BOUNCYCASTLE, BuildType.RUNTIME);
		downloadFromApache(MavenObject.BOUNCYCASTLE_MAIL, BuildType.RUNTIME);
		downloadFromApache(MavenObject.JSCH, BuildType.RUNTIME);
		downloadFromApache(MavenObject.ROME, BuildType.RUNTIME);
		downloadFromApache(MavenObject.JDOM, BuildType.RUNTIME);
		downloadFromApache(MavenObject.GSON, BuildType.RUNTIME);
		downloadFromApache(MavenObject.MAIL, BuildType.RUNTIME);

		downloadFromEclipse(MavenObject.JGIT, BuildType.RUNTIME);
		downloadFromEclipse(MavenObject.JGIT_HTTP, BuildType.RUNTIME);
	}

	public static void compiletime() {
		downloadFromApache(MavenObject.JUNIT, BuildType.RUNTIME);
		downloadFromApache(MavenObject.JCOMMANDER, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.JETTY, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.SERVLET, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.SLF4JAPI, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.SLF4LOG4J, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.LOG4J, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.WICKET, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.WICKET_EXT, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.WICKET_AUTH_ROLES, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.WICKET_GOOGLE_CHARTS, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.MARKDOWNPAPERS, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.BOUNCYCASTLE, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.BOUNCYCASTLE_MAIL, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.JSCH, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.ROME, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.JDOM, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.GSON, BuildType.COMPILETIME);
		downloadFromApache(MavenObject.MAIL, BuildType.COMPILETIME);

		downloadFromEclipse(MavenObject.JGIT, BuildType.COMPILETIME);
		downloadFromEclipse(MavenObject.JGIT_HTTP, BuildType.COMPILETIME);

		// needed for site publishing
		downloadFromApache(MavenObject.COMMONSNET, BuildType.RUNTIME);
	}
	
	public static void federationClient() {
		downloadFromApache(MavenObject.JCOMMANDER, BuildType.RUNTIME);
		downloadFromApache(MavenObject.SERVLET, BuildType.RUNTIME);
		downloadFromApache(MavenObject.MAIL, BuildType.RUNTIME);
		downloadFromApache(MavenObject.SLF4JAPI, BuildType.RUNTIME);
		downloadFromApache(MavenObject.SLF4LOG4J, BuildType.RUNTIME);
		downloadFromApache(MavenObject.LOG4J, BuildType.RUNTIME);
		downloadFromApache(MavenObject.GSON, BuildType.RUNTIME);
		downloadFromApache(MavenObject.JSCH, BuildType.RUNTIME);
		
		downloadFromEclipse(MavenObject.JGIT, BuildType.RUNTIME);
	}

	/**
	 * Builds the Keys class based on the gitblit.properties file and inserts
	 * the class source into the project source folder.
	 */
	public static void buildSettingKeys() {
		// Load all keys
		Properties properties = new Properties();
		FileInputStream is = null;
		try {
			is = new FileInputStream(Constants.PROPERTIES_FILE);
			properties.load(is);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Throwable t) {
					// IGNORE
				}
			}
		}
		List<String> keys = new ArrayList<String>(properties.stringPropertyNames());
		Collections.sort(keys);

		// Determine static key group classes
		Map<String, List<String>> staticClasses = new HashMap<String, List<String>>();
		staticClasses.put("", new ArrayList<String>());
		for (String key : keys) {
			String clazz = "";
			String field = key;
			if (key.indexOf('.') > -1) {
				clazz = key.substring(0, key.indexOf('.'));
				field = key.substring(key.indexOf('.') + 1);
			}
			if (!staticClasses.containsKey(clazz)) {
				staticClasses.put(clazz, new ArrayList<String>());
			}
			staticClasses.get(clazz).add(field);
		}

		// Assemble Keys source file
		StringBuilder sb = new StringBuilder();
		sb.append("package com.gitblit;\n");
		sb.append('\n');
		sb.append("/*\n");
		sb.append(" * This class is auto-generated from the properties file.\n");
		sb.append(" * Do not version control!\n");
		sb.append(" */\n");
		sb.append("public final class Keys {\n");
		sb.append('\n');
		List<String> classSet = new ArrayList<String>(staticClasses.keySet());
		Collections.sort(classSet);
		for (String clazz : classSet) {
			List<String> keySet = staticClasses.get(clazz);
			if (clazz.equals("")) {
				// root keys
				for (String key : keySet) {
					sb.append(MessageFormat.format(
							"\tpublic static final String {0} = \"{1}\";\n\n",
							key.replace('.', '_'), key));
				}
			} else {
				// class keys
				sb.append(MessageFormat.format("\tpublic static final class {0} '{'\n\n", clazz));
				sb.append(MessageFormat.format(
						"\t\tpublic static final String _ROOT = \"{0}\";\n\n", clazz));
				for (String key : keySet) {
					sb.append(MessageFormat.format(
							"\t\tpublic static final String {0} = \"{1}\";\n\n",
							key.replace('.', '_'), clazz + "." + key));
				}
				sb.append("\t}\n\n");
			}
		}
		sb.append('}');

		// Save Keys class definition
		try {
			File file = new File("src/com/gitblit/Keys.java");
			FileWriter fw = new FileWriter(file, false);
			fw.write(sb.toString());
			fw.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Download a file from the official Apache Maven repository.
	 * 
	 * @param mo
	 *            the maven object to download.
	 * @return
	 */
	private static List<File> downloadFromApache(MavenObject mo, BuildType type) {
		return downloadFromMaven("http://repo1.maven.org/maven2/", mo, type);
	}

	/**
	 * Download a file from the official Eclipse Maven repository.
	 * 
	 * @param mo
	 *            the maven object to download.
	 * @return
	 */
	private static List<File> downloadFromEclipse(MavenObject mo, BuildType type) {
		return downloadFromMaven("http://download.eclipse.org/jgit/maven/", mo, type);
	}

	/**
	 * Download a file from a Maven repository.
	 * 
	 * @param mo
	 *            the maven object to download.
	 * @return
	 */
	private static List<File> downloadFromMaven(String mavenRoot, MavenObject mo, BuildType type) {
		List<File> downloads = new ArrayList<File>();
		String[] jars = { "" };
		if (BuildType.RUNTIME.equals(type)) {
			jars = new String[] { "" };
		} else if (BuildType.COMPILETIME.equals(type)) {
			jars = new String[] { "-sources", "-javadoc" };
		}
		for (String jar : jars) {
			File targetFile = mo.getLocalFile("ext", jar);
			if (targetFile.exists()) {
				downloads.add(targetFile);
				continue;
			}
			String expectedSHA1 = mo.getSHA1(jar);
			if (expectedSHA1 == null) {
				// skip this jar
				continue;
			}
			float approximateLength = mo.getApproximateLength(jar);
			String mavenURL = mavenRoot + mo.getRepositoryPath(jar);
			if (!targetFile.getAbsoluteFile().getParentFile().exists()) {
				boolean success = targetFile.getAbsoluteFile().getParentFile().mkdirs();
				if (!success) {
					throw new RuntimeException("Failed to create destination folder structure!");
				}
			}
			ByteArrayOutputStream buff = new ByteArrayOutputStream();
			try {
				URL url = new URL(mavenURL);
				InputStream in = new BufferedInputStream(url.openStream());
				byte[] buffer = new byte[4096];
				int downloadedLen = 0;
				float lastProgress = 0f;

				updateDownload(0, targetFile);
				while (true) {
					int len = in.read(buffer);
					if (len < 0) {
						break;
					}
					downloadedLen += len;
					buff.write(buffer, 0, len);
					float progress = downloadedLen / approximateLength;
					if (progress - lastProgress >= 0.1f) {
						lastProgress = progress;
						updateDownload(progress, targetFile);
					}
				}
				in.close();
				updateDownload(1f, targetFile);

			} catch (IOException e) {
				throw new RuntimeException("Error downloading " + mavenURL + " to " + targetFile, e);
			}
			byte[] data = buff.toByteArray();
			String calculatedSHA1 = StringUtils.getSHA1(data);

			System.out.println();

			if (expectedSHA1.length() == 0) {
				updateProgress(0, "sha: " + calculatedSHA1);
				System.out.println();
			} else {
				if (!calculatedSHA1.equals(expectedSHA1)) {
					throw new RuntimeException("SHA1 checksum mismatch; got: " + calculatedSHA1);
				}
			}
			try {
				RandomAccessFile ra = new RandomAccessFile(targetFile, "rw");
				ra.write(data);
				ra.setLength(data.length);
				ra.close();
			} catch (IOException e) {
				throw new RuntimeException("Error writing to file " + targetFile, e);
			}
			downloads.add(targetFile);
		}
		return downloads;
	}

	private static void updateDownload(float progress, File file) {
		updateProgress(progress, "d/l: " + file.getName());
	}

	private static void updateProgress(float progress, String url) {
		String anim = "==========";
		int width = Math.round(anim.length() * progress);
		System.out.print("\r[");
		System.out.print(anim.substring(0, Math.min(width, anim.length())));
		for (int i = 0; i < anim.length() - width; i++) {
			System.out.print(' ');
		}
		System.out.print("] " + url);
	}

	/**
	 * MavenObject represents a complete maven artifact (binary, sources, and
	 * javadoc). MavenObjects can be downloaded and checksummed to confirm
	 * authenticity.
	 */
	private static class MavenObject {

		public static final MavenObject JCOMMANDER = new MavenObject("jCommander", "com/beust",
				"jcommander", "1.17", 34000, 32000, 141000,
				"219a3540f3b27d7cc3b1d91d6ea046cd8723290e",
				"0bb50eec177acf0e94d58e0cf07262fe5164331d",
				"c7adc475ca40c288c93054e0f4fe58f3a98c0cb5");

		public static final MavenObject JETTY = new MavenObject("Jetty",
				"org/eclipse/jetty/aggregate", "jetty-webapp", "7.4.3.v20110701", 1000000, 680000,
				2720000, "bde072b178f9650e2308f0babe58a4baaa469e3c",
				"bc75f05dd4f7fa848720ac669b8b438ee4a6b146",
				"dcd42f672e734521d1a6ccc0c2f9ecded1a1a281");

		public static final MavenObject SERVLET = new MavenObject("Servlet 2.5", "javax/servlet",
				"servlet-api", "2.5", 105000, 158000, 0,
				"5959582d97d8b61f4d154ca9e495aafd16726e34",
				"021599814ad9a605b86f3e6381571beccd861a32", null);

		public static final MavenObject SLF4JAPI = new MavenObject("SLF4J API", "org/slf4j",
				"slf4j-api", "1.6.1", 25500, 45000, 182000,
				"6f3b8a24bf970f17289b234284c94f43eb42f0e4",
				"46a386136c901748e6a3af67ebde6c22bc6b4524",
				"e223571d77769cdafde59040da235842f3326453");

		public static final MavenObject SLF4LOG4J = new MavenObject("SLF4J LOG4J", "org/slf4j",
				"slf4j-log4j12", "1.6.1", 9800, 9500, 52400,
				"bd245d6746cdd4e6203e976e21d597a46f115802",
				"7a26b08b265f55622fa1fed3bda68bbd37a465ba",
				"6e4b16bce7994e3692e82002f322a0dd2f32741e");

		public static final MavenObject LOG4J = new MavenObject("Apache LOG4J", "log4j", "log4j",
				"1.2.16", 481000, 471000, 1455000, "7999a63bfccbc7c247a9aea10d83d4272bd492c6",
				"bf945d1dc995be7fe64923625f842fbb6bf443be",
				"78aa1cbf0fa3b259abdc7d87f9f6788d785aac2a");

		public static final MavenObject WICKET = new MavenObject("Apache Wicket",
				"org/apache/wicket", "wicket", "1.4.18", 1960000, 1906000, 6818000,
				"921a50dbbebdf034f0042f2294760e7535cb7041",
				"b432d60b32449fdfb216ac23af8a3ed3e0a3368c",
				"435e70f9de94975ee30c3f1b1aa1401aea9b4e70");

		public static final MavenObject WICKET_EXT = new MavenObject("Apache Wicket Extensions",
				"org/apache/wicket", "wicket-extensions", "1.4.18", 1180000, 1118000, 1458000,
				"f568bd2ad382db935ab06fdccfdead3f10ed1f15",
				"c00a4979d7647d3367c6e4897a2fd7d0f78a73cc",
				"5e76ab69f6307e3ecb2638779008b3adf5cbf9aa");

		public static final MavenObject WICKET_AUTH_ROLES = new MavenObject(
				"Apache Wicket Auth Roles", "org/apache/wicket", "wicket-auth-roles", "1.4.18",
				44000, 45000, 166000, "44cf0647e1adca377cc4258cd7fac33aa1dd11ab",
				"2a7e9c6a9687136c2527afa2e53148cfa82696c6",
				"6e280995097e84b72b283132b8fe6796595caa38");

		public static final MavenObject WICKET_GOOGLE_CHARTS = new MavenObject(
				"Apache Wicket Google Charts Add-On", "org/wicketstuff", "googlecharts", "1.4.18",
				34000, 18750, 161000, "1f763cc8a04e62840b63787a77a479b04ad99c75",
				"1521ed6397192c464e89787502f937bc96ece8f8",
				"8b0398d58bce63ba7f7a9232c4ca24160c9b1a11");

		public static final MavenObject JUNIT = new MavenObject("JUnit", "junit", "junit", "4.8.2",
				237000, 0, 0, "c94f54227b08100974c36170dcb53329435fe5ad", "", "");

		public static final MavenObject MARKDOWNPAPERS = new MavenObject("MarkdownPapers",
				"org/tautua/markdownpapers", "markdownpapers-core", "1.1.1", 87000, 58000, 278000,
				"07046e6d8f33866398dfc3955698925df9ff7719",
				"178b49c34dbab6301ce848b67e7957bcf9b94d6a",
				"160d370f6cb119a1b46a00f37cc28d23fd27daed");

		public static final MavenObject BOUNCYCASTLE = new MavenObject("BouncyCastle",
				"org/bouncycastle", "bcprov-jdk16", "1.46", 1900000, 1400000, 4670000,
				"ce091790943599535cbb4de8ede84535b0c1260c",
				"d2b70567594225923450d7e3f80cd022c852725e",
				"873a6fe765f33fc27df498a5d1f5bf077e503b2f");

		public static final MavenObject BOUNCYCASTLE_MAIL = new MavenObject("BouncyCastle Mail",
				"org/bouncycastle", "bcmail-jdk16", "1.46", 502000, 420000, 482000,
				"08a9233bfd6ad38ea32df5e6ff91035b650584b9",
				"3ebd62bc56854767512dc5deec0a17795f2e671d",
				"3b7c5f3938f202311bdca0bf7ed46bc0118af081");

		public static final MavenObject JGIT = new MavenObject("JGit", "org/eclipse/jgit",
				"org.eclipse.jgit", "1.1.0.201109151100-r", 1318000, 1354000, 3300000,
				"bacc988346c839f79513d7bc7f5c88b22ea6e7a5",
				"90abf988d98ce0d4b162f94f63fc99c435eba6b4",
				"a46540a2857a0fdbf43debf3383295a897946c79");

		public static final MavenObject JGIT_HTTP = new MavenObject("JGit", "org/eclipse/jgit",
				"org.eclipse.jgit.http.server", "1.1.0.201109151100-r", 68000, 62000, 110000,
				"3070161a89756aac2dfc2e26d89faf31fe894ab4",
				"9cecb8e4351e616688cafbcca906f542d9b1f525",
				"20aaab759acd8eb6cb6acbb1b2934a689fb3774d");

		public static final MavenObject JSCH = new MavenObject("JSch", "com/jcraft", "jsch",
				"0.1.44-1", 214000, 211000, 413000, "2e9ae08de5a71bd0e0d3ba2558598181bfa71d4e",
				"e528f593b19b04d500992606f58b87fcfded8883",
				"d0ffadd0a4ab909d94a577b5aad43c13b617ddcb");

		public static final MavenObject COMMONSNET = new MavenObject("commons-net", "commons-net",
				"commons-net", "1.4.0", 181000, 0, 0, "eb47e8cad2dd7f92fd7e77df1d1529cae87361f7",
				"", "");

		public static final MavenObject ROME = new MavenObject("rome", "rome", "rome", "0.9",
				208000, 196000, 407000, "dee2705dd01e79a5a96a17225f5a1ae30470bb18",
				"226f851dc44fd94fe70b9c471881b71f88949cbf",
				"8d7d867b97eeb3a9196c3926da550ad042941c1b");

		public static final MavenObject JDOM = new MavenObject("jdom", "org/jdom", "jdom", "1.1",
				153000, 235000, 445000, "1d04c0f321ea337f3661cf7ede8f4c6f653a8fdd",
				"a7ed425c4c46605b8f2bf2ee118c1609682f4f2c",
				"f3df91edccba2f07a0fced70887c2f7b7836cb75");

		public static final MavenObject GSON = new MavenObject("gson", "com/google/code/gson",
				"gson", "1.7.1", 174000, 142000, 247000,
				"0697e3a1fa094a983cd12f7f6f61abf9c6ea52e2",
				"51f6f78aec2d30d0c2bfb4a5f00d456a6f7a5e7e",
				"f0872fe17d484815328538b89909d5e46d85db74");

		public static final MavenObject MAIL = new MavenObject("javax.mail", "javax/mail", "mail",
				"1.4.3", 462000, 642000, 0, "8154bf8d666e6db154c548dc31a8d512c273f5ee",
				"5875e2729de83a4e46391f8f979ec8bd03810c10", null);

		public final String name;
		public final String group;
		public final String artifact;
		public final String version;
		public final int approxLibraryLen;
		public final int approxSourcesLen;
		public final int approxJavadocLen;
		public final String librarySHA1;
		public final String sourcesSHA1;
		public final String javadocSHA1;

		private MavenObject(String name, String group, String artifact, String version,
				int approxLibraryLen, int approxSourcesLen, int approxJavadocLen,
				String librarySHA1, String sourcesSHA1, String javadocSHA1) {
			this.name = name;
			this.group = group;
			this.artifact = artifact;
			this.version = version;
			this.approxLibraryLen = approxLibraryLen;
			this.approxSourcesLen = approxSourcesLen;
			this.approxJavadocLen = approxJavadocLen;
			this.librarySHA1 = librarySHA1;
			this.sourcesSHA1 = sourcesSHA1;
			this.javadocSHA1 = javadocSHA1;
		}

		private String getRepositoryPath(String jar) {
			return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + jar
					+ ".jar";
		}

		private File getLocalFile(String basePath, String jar) {
			return new File(basePath, artifact + "-" + version + jar + ".jar");
		}

		private String getSHA1(String jar) {
			if (jar.equals("")) {
				return librarySHA1;
			} else if (jar.equals("-sources")) {
				return sourcesSHA1;
			} else if (jar.equals("-javadoc")) {
				return javadocSHA1;
			}
			return librarySHA1;
		}

		private int getApproximateLength(String jar) {
			if (jar.equals("")) {
				return approxLibraryLen;
			} else if (jar.equals("-sources")) {
				return approxSourcesLen;
			} else if (jar.equals("-javadoc")) {
				return approxJavadocLen;
			}
			return approxLibraryLen;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
