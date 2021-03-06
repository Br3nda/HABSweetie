package de.akuz.android.openhab.core.requests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import android.util.Log;

import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.util.ObjectParser;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import de.akuz.android.openhab.core.objects.AbstractOpenHABObject;
import de.akuz.android.openhab.settings.OpenHABConnectionSettings;

public abstract class AbstractOpenHABRequest<RESULT extends AbstractOpenHABObject>
		extends GoogleHttpClientSpiceRequest<RESULT> {

	private final static String TAG = AbstractOpenHABRequest.class
			.getSimpleName();

	private static XmlNamespaceDictionary dictionary = new XmlNamespaceDictionary();
	private static XmlObjectParser parser;
	static {
		dictionary.set("", "");
		parser = new XmlObjectParser(dictionary);
	}

	protected String baseUrl;

	protected OpenHABConnectionSettings setting;

	protected Class<RESULT> resultClass;

	public AbstractOpenHABRequest(Class<RESULT> clazz,
			OpenHABConnectionSettings setting) {
		super(clazz);
		this.setting = setting;
		this.baseUrl = setting.getBaseUrl();
		resultClass = clazz;
	}

	public AbstractOpenHABRequest(Class<RESULT> clazz, String baseUrl) {
		super(clazz);
		this.baseUrl = baseUrl;
		resultClass = clazz;
	}

	public abstract void setParameters(String... params);

	@Override
	public final RESULT loadDataFromNetwork() throws Exception {
		RESULT result = executeRequest();
		if (result != null) {
			result.setBaseUrl(baseUrl);
		}
		return result;
	}

	protected HttpRequest getRequest(String url) throws Exception {
		Log.d(TAG, "Building request for URL " + url);
		HttpRequest request = getHttpRequestFactory().buildGetRequest(
				new GenericUrl(url));
		configureRequest(request);
		ObjectParser parser = getObjectParser();
		request.setParser(parser);
		return request;
	}

	protected HttpRequest configureRequest(HttpRequest request) {
		request.setCurlLoggingEnabled(false);
		request.setLoggingEnabled(false);
		request.setFollowRedirects(true);
		HttpHeaders headers = request.getHeaders();
		headers.setAccept("application/xml");

		if (setting != null && setting.hasCredentials()) {
			Log.d(TAG,
					"Setting Authorization from supplied connection settings");
			BasicAuthentication auth = new BasicAuthentication(
					setting.getUsername(), setting.getPassword());
			request.setInterceptor(auth);
		}
		headers.set("Accept-Charset", "utf-8");
		request.setHeaders(headers);
		return request;
	}

	protected ObjectParser getObjectParser() {
		return parser;
	}

	protected RESULT parseInputStream(InputStream in) throws IOException {
		ObjectParser parser = getObjectParser();
		RESULT result = parser.parseAndClose(in, Charset.forName("utf-8"),
				resultClass);
		result.setReceivedAt(System.currentTimeMillis());
		return result;
	}

	public OpenHABConnectionSettings getConnectionSettings() {
		return setting;
	}

	protected abstract RESULT executeRequest() throws Exception;

}
