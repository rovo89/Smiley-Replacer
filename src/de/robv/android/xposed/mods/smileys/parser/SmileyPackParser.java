package de.robv.android.xposed.mods.smileys.parser;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.graphics.Movie;
import android.graphics.drawable.Drawable;

public class SmileyPackParser {
	public SmileyPackParser(String filename) throws IOException {
		this(new File(filename));
	}

	public SmileyPackParser(File file) throws IOException {
		this.file = file;
		reparse();
	}

	public File getFile() {
		return file;
	}

	public String getTitle() {
		return title;
	}

	public String getSummary() {
		return summary;
	}

	public Smiley[] getSmileys() {
		return smileys.toArray(new Smiley[smileys.size()]);
	}

	public SmileyGroup[] getSmileyGroups() {
		return groups.toArray(new SmileyGroup[groups.size()]);
	}

	public void clear() throws IOException {
		title = "";
		summary = "";
		descriptions.clear();
		smileys.clear();
		if (zip != null)
			zip.close();
		zip = null;
	}

	public void reparse() throws IOException {
		clear();
		zip = new ZipFile(file);

		ZipEntry infoEntry = null;
		ZipEntry codesEntry = null;
		ZipEntry[] descEntries = new ZipEntry[] {null, null, null};
		HashSet<String> existingFiles = new HashSet<String>();

		// look for relevant configuration files
		for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = e.nextElement();
			if (entry.isDirectory())
				continue;

			String name = entry.getName();
			if (name.equals(FILE_INFO)) {
				infoEntry = entry;
			} else if (name.equals(FILE_CODES)) {
				codesEntry = entry;
			} else if (name.equals(FILE_DESCRIPTIONS)) {
				descEntries[0] = entry;
			} else if (name.equals(FILE_DESCRIPTIONS_LANG)) {
				descEntries[1] = entry;
			} else if (name.equals(FILE_DESCRIPTIONS_LANG_COUNTRY)) {
				descEntries[2] = entry;
			}

			existingFiles.add(name);
		}

		// no codes, no smileys => error
		if (codesEntry == null)
			throw new IOException("Required file " + FILE_CODES + " not found");

		// parse information (title and description)
		if (infoEntry != null)
			parseInfo(zip.getInputStream(infoEntry));
		else
			title = file.getName();

		// parse descriptions, more specific one override general ones
		for (int i = 0; i < descEntries.length; i++) {
			if (descEntries[i] != null)
				parseDescriptions(zip.getInputStream(descEntries[i]));
		}

		// parse codes and create triples
		parseCodes(zip.getInputStream(codesEntry), existingFiles);
	}

	protected void parseInfo(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		int idx = 0;
		String line;
		while ((line = br.readLine()) != null) {
			idx++;
			if (idx == 1) {
				title = line;
				continue;
			}
			if (idx > 2)
				summary += "\n";
			summary += line;
		}
	}

	protected void parseDescriptions(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.charAt(0) == '#')
				continue;

			String[] parts = line.split("\\s+", 2);
			if (parts.length != 2)
				continue;

			descriptions.put(parts[0], parts[1]);
		}
	}

	protected void parseCodes(InputStream is, HashSet<String> existingFiles) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.charAt(0) == '#')
				continue;

			String[] parts = line.split("\\s+");

			String image = parts[0];
			if (!existingFiles.contains(image))
				continue;

			String description = descriptions.get(image);
			if (description == null)
				description = image.split("\\.", 2)[0];

			SmileyGroup group = null;
			for (int i = 1; i < parts.length; i++) {
				String code = parts[i];
				Smiley smiley = new Smiley(code, image, description);
				smileys.add(smiley);

				if (group == null) {
					group = new SmileyGroup(smiley);
					groups.add(group);
				} else {
					group.addCode(code);
				}
			}
		}
	}

	public static void setLocale(Locale locale) {
		if (locale == null)
			locale = Locale.getDefault();

		FILE_DESCRIPTIONS_LANG  = "descriptions-" + locale.getLanguage() + ".txt";
		FILE_DESCRIPTIONS_LANG_COUNTRY = "descriptions-" + locale.getLanguage() + "-r" + locale.getCountry() + ".txt";
	}

	@Override
	protected void finalize() throws Throwable {
		if (zip != null) {
			zip.close();
			zip = null;
		}
	}

	@Override
	public String toString() {
		return title;
	}

	protected final File file;
	protected ZipFile zip;
	protected String title;
	protected String summary;
	protected final HashMap<String, String> descriptions = new HashMap<String, String>(10);
	protected final ArrayList<Smiley> smileys = new ArrayList<Smiley>(20);
	protected final ArrayList<SmileyGroup> groups = new ArrayList<SmileyGroup>(10);

	protected static final String FILE_INFO = "info.txt";
	protected static final String FILE_CODES = "codes.txt";
	protected static final String FILE_DESCRIPTIONS = "descriptions.txt";
	protected static String FILE_DESCRIPTIONS_LANG;
	protected static String FILE_DESCRIPTIONS_LANG_COUNTRY;

	static {
		setLocale(null);
	}

	public class Smiley {
		protected final String code;
		protected final String image;
		protected final String description;
		protected byte[] imageBytes;

		public Smiley(String code, String image, String description) {
			this.code = code;
			this.image = image;
			this.description = description;
		}

		public String getCode() {
			return code;
		}

		public String getImageName() {
			return image;
		}

		public byte[] getImageBytes() throws IOException {
			if (imageBytes != null)
				return imageBytes;

			InputStream is = zip.getInputStream(zip.getEntry(image));

			int len;
			int size = 1024;
			byte[] buf = new byte[size];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			while ((len = is.read(buf, 0, size)) != -1)
				bos.write(buf, 0, len);

			imageBytes = bos.toByteArray();
			return imageBytes;
		}

		public ByteArrayInputStream getImageStream() throws IOException {
			return new ByteArrayInputStream(getImageBytes());
		}

		public Drawable getImageAsDrawable() throws IOException {
			return Drawable.createFromStream(getImageStream(), null);
		}

		public Movie getImageAsMovie() throws IOException {
			byte[] smileyBytes = getImageBytes();
			return Movie.decodeByteArray(smileyBytes, 0, smileyBytes.length);
		}

		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append("code=");
			sb.append(code);
			sb.append(", image=");
			sb.append(image);
			sb.append(", description=");
			sb.append(description);
			sb.append('}');
			return sb.toString();
		}
	}

	/** Same image and description, just different codes */
	public class SmileyGroup {
		private final Smiley base;
		private final ArrayList<String> codes = new ArrayList<String>(3);

		public SmileyGroup(Smiley base) {
			this.base = base;
			addCode(base.getCode());
		}

		public void addCode(String code) {
			codes.add(code);
		}

		public Smiley getBase() {
			return base;
		}

		public String[] getCodes() {
			return codes.toArray(new String[codes.size()]);
		}
	}
}
