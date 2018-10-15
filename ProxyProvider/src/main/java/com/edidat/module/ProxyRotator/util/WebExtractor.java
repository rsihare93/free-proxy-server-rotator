package com.edidat.module.ProxyRotator.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import com.edidat.module.ProxyRotator.pojo.NetworkProxy;

public class WebExtractor {

	private static final Logger logger = LogManager.getLogger(WebExtractor.class);
	private static WebExtractor webExtractor;

	private WebExtractor() {

	}

	public static final WebExtractor getInstance() {
		if (webExtractor == null) {
			webExtractor = new WebExtractor();
		}
		return webExtractor;
	}

	public Optional<Document> getHTMLDocument(String url) {
		Optional<Document> toReturn = Optional.empty();
		int numberOfRetries = 3;
		while (numberOfRetries > 0) {
			NetworkProxy netProxy = null;
			try {
				return Optional.of(Jsoup.connect(url).get());
			} catch (IllegalArgumentException e) {
				logger.error("Url : {} is invalid", url, e);
				break;
			} catch (IOException e) {
				logger.error("Either url : {} or proxy : {} is not rechable {}", url, netProxy, e);
				numberOfRetries--;
				continue;
			} catch (Exception e) {
				logger.error("Exception Occured while fetching html document for url and proxy {}, {}", url, netProxy,
						e);
				break;
			}
		}
		return toReturn;
	}

	public Optional<Set<CellData>> extractList(String url, String domSelector, boolean useProxy) throws IOException {
		Optional<Set<CellData>> toReturn = Optional.empty();
		Optional<Document> docOtional = getHTMLDocument(url);
		if (!docOtional.isPresent()) {
			return toReturn;
		}
		Document doc = docOtional.get();
		Element tableElement = doc.select(domSelector).first();
		String hostName = new URL(doc.baseUri()).getHost();
		Set<CellData> dataRow = new LinkedHashSet<CellData>();
		Elements tableRowElements = tableElement.select("li");
		for (int i = 0; i < tableRowElements.size(); i++) {
			CellData cellData = readCell(hostName, tableRowElements, i);
			dataRow.add(cellData);
		}
		return Optional.of(dataRow);
	}

	private CellData readCell(String hostName, Elements tableRowElements, int i) {
		CellData cellData = new CellData();
		Element row = tableRowElements.get(i);
		cellData.setUrl(getAnchoredURL(hostName, row));
		cellData.setValue(row.text());
		return cellData;
	}

	public Optional<CellData> extractElementData(Document doc, String domSelector) throws MalformedURLException {
		Optional<CellData> toReturn = Optional.empty();
		Elements element = doc.select(domSelector);
		if (element == null || element.first() == null) {
			return toReturn;
		}
		String hostName = new URL(doc.baseUri()).getHost();
		String anchoredURL = getAnchoredURL(hostName, element.first());
		return Optional.of(new CellData(element.first().text(), anchoredURL));
	}

	public Optional<Map<CellData, Map<CellData, CellData>>> extractTableData(String url, String domSelector,
			boolean hasHHeaders, boolean hasVHeaders, boolean useProxy) throws IOException {
		Optional<Map<CellData, Map<CellData, CellData>>> toReturn = Optional.empty();

		// 1) Get Html Document
		Optional<Document> docOtional = getHTMLDocument(url);
		if (!docOtional.isPresent()) {
			return toReturn;
		}
		Document doc = docOtional.get();
		String hostName = new URL(doc.baseUri()).getHost();

		Map<CellData, Map<CellData, CellData>> tableData = new LinkedHashMap<>();

		// Load THEAD horizontal headers///////////////////////////
		Element tableElement = doc.select(domSelector).first();
		Elements tableHeaderEles = tableElement.select("thead tr th");
		List<CellData> hKeys = new ArrayList<CellData>();
		for (int i = 0; i < tableHeaderEles.size(); i++) {
			if (!hasHHeaders) {
				break;
			}
			CellData key = readCell(hostName, tableHeaderEles, i);
			hKeys.add(key);
		}
		//////////////////////////////////////////////////
		Elements tableRowElements = tableElement.select(":not(thead) tr");
		boolean hasThead = hKeys.size() > 0;
		for (int i = 0; i < tableRowElements.size(); i++) {

			CellData currentVheader = new CellData(i + "", "");
			Element horizontalRowElement = tableRowElements.get(i);
			Elements cellElement = horizontalRowElement.select("td");

			if (cellElement.size() == 0) {
				continue;
			}

			Map<CellData, CellData> rowData = new LinkedHashMap<>();
			int k = -1;
			for (int j = 0; j < cellElement.size(); j++) {

				if (k == -1 && !hasThead) {
					k = 0;
				}
				// load horizontal headers if not thead
				if (k == 0 && hasHHeaders) {
					CellData key = readCell(hostName, cellElement, j);
					hKeys.add(key);
					System.out.println("Adding hKey " + key.getValue());
					hasThead = true;
				} else {
					if (j == 0 && hasVHeaders) {
						currentVheader = readCell(hostName, cellElement, j);
						if (currentVheader == null || StringUtils.isEmpty(currentVheader.getValue())) {
							currentVheader = new CellData(i + "", "");
						}
					} else {
						CellData cell = readCell(hostName, cellElement, j);
						CellData hkey = new CellData(j + "", "");
						if (hKeys.size() > j) {
							hkey = hKeys.get(j);
						}
						rowData.put(hkey, cell);
					}
				}
			}
			tableData.put(currentVheader, rowData);
		}
		return Optional.of(tableData);
	}

	public Optional<CellData> getSelectionTextContent(String domSelector, String url, boolean useProxy)
			throws IOException {
		Optional<Document> docOtional = getHTMLDocument(url);
		if (!docOtional.isPresent()) {
			return null;
		}
		Document doc = docOtional.get();
		String hostName = new URL(doc.baseUri()).getHost();
		Element tableElement = doc.select(domSelector).first();
		if (tableElement == null) {
			return Optional.empty();
		}
		return Optional.of(new CellData(tableElement.text(), getAnchoredURL(hostName, tableElement)));
	}

	public class CellData {
		String Value;
		String url;

		public String getValue() {
			return Value;
		}

		public void setValue(String value) {
			Value = value;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public CellData() {
			super();
		}

		public CellData(String value, String url) {
			Value = value;
			this.url = url;
		}

		public String toString() {
			return "(" + this.Value + " -> " + this.getUrl() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((Value == null) ? 0 : Value.hashCode());
			result = prime * result + ((url == null) ? 0 : url.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CellData other = (CellData) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Value == null) {
				if (other.Value != null)
					return false;
			} else if (!Value.equals(other.Value))
				return false;
			if (url == null) {
				if (other.url != null)
					return false;
			} else if (!url.equals(other.url))
				return false;
			return true;
		}

		private WebExtractor getOuterType() {
			return WebExtractor.this;
		}

	}

	private String getAnchoredURL(String hostName, Element row) {
		Elements anchorTag = row.select("a");
		if (anchorTag != null) {
			String url = anchorTag.attr("href");
			if (url == null || url.trim().isEmpty()) {
				return "";
			}
			if (!url.contains("http") || url.startsWith("/") || url.startsWith(".")) {
				return hostName + "/" + url;
			} else {
				return url;
			}
		} else {
			return "";
		}
	}

	public static void printMap(Map<CellData, Map<CellData, CellData>> table) {
		Set<CellData> vKeys = table.keySet();
		for (CellData vKey : vKeys) {
			Map<CellData, CellData> map = table.get(vKey);
			Set<CellData> hKeys = map.keySet();
			logger.debug(vKey.getValue() + "(" + vKey.getUrl() + ")\t");
			for (CellData hKey : hKeys) {
				CellData cellData = map.get(hKey);
				logger.debug(cellData.getValue() + "(" + cellData.getUrl() + ")\t");
			}
			logger.debug("\n");
		}
	}

}