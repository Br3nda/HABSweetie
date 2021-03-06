package de.akuz.android.openhab.core.requests;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;

import de.akuz.android.openhab.core.objects.Page;
import de.akuz.android.openhab.settings.OpenHABConnectionSettings;

public class PageRequest extends AbstractOpenHABRequest<Page> {

	private final static String TAG = PageRequest.class.getSimpleName();

	private String pageUrl;

	public PageRequest(OpenHABConnectionSettings settings, String sitemapId,
			String pageId) {
		this(settings, buildPageUrl(settings, sitemapId, pageId));
	}

	private static String buildPageUrl(OpenHABConnectionSettings settings,
			String sitemapId, String pageId) {
		StringBuilder b = new StringBuilder();
		b.append(settings.getBaseUrl());
		if (!settings.getBaseUrl().endsWith("/")) {
			b.append('/');
		}
		b.append("rest/sitemaps/");
		b.append(sitemapId);
		b.append('/');
		b.append(pageId);
		return b.toString();
	}

	public PageRequest(OpenHABConnectionSettings settings, String pageUrl) {
		super(Page.class, settings);
		this.pageUrl = pageUrl;
	}

	@Override
	public void setParameters(String... params) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Page executeRequest() throws Exception {
		HttpRequest request = getRequest(pageUrl);
		HttpResponse response = request.execute();

		Page result = parseInputStream(response.getContent());
		return result;
	}

}
