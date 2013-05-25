package de.akuz.android.openhab.core.requests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.util.ObjectParser;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import de.akuz.android.openhab.core.OpenHABAuthManager;
import de.akuz.android.openhab.core.objects.AbstractOpenHABObject;

public abstract class AbstractOpenHABRequest<RESULT extends AbstractOpenHABObject>
		extends GoogleHttpClientSpiceRequest<RESULT> {

	private final static String TAG = AbstractOpenHABRequest.class
			.getSimpleName();

	protected String baseUrl;

	protected Class<RESULT> resultClass;

	public AbstractOpenHABRequest(Class<RESULT> clazz, String baseUrl) {
		super(clazz);
		this.baseUrl = baseUrl;
		resultClass = clazz;
	}

	public abstract void setParameters(String... params);

	@Override
	public final RESULT loadDataFromNetwork() throws Exception {
		RESULT result = executeRequest();
		result.setBaseUrl(baseUrl);
		return result;
	}

	protected HttpRequest getRequest(String url) throws Exception {
		Log.d(TAG, "Building request for URL " + url);
		HttpRequest request = getHttpRequestFactory().buildGetRequest(
				new GenericUrl(url));
		request.setFollowRedirects(true);
		HttpHeaders headers = request.getHeaders();
		headers.setAccept("application/xml");
		if (OpenHABAuthManager.hasCredentials()) {
			Log.d(TAG, "Setting basic auth for request");
			headers = headers.setBasicAuthentication(
					OpenHABAuthManager.getUsername(),
					OpenHABAuthManager.getPassword());
		}
		headers.set("Accept-Charset", "utf-8");
		request.setHeaders(headers);
		ObjectParser parser = getObjectParser();
		request.setParser(parser);
		return request;
	}

	protected ObjectParser getObjectParser() {
		XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
		dictionary.set("", "");
		XmlObjectParser parser = new XmlObjectParser(dictionary);
		return parser;
	}

	protected RESULT parseInputStream(InputStream in) throws IOException {
		ObjectParser parser = getObjectParser();
		return parser.parseAndClose(in, Charset.forName("utf-8"), resultClass);
	}

	protected abstract RESULT executeRequest() throws Exception;

}
