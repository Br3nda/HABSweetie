package de.akuz.android.openhab.tasker;

import java.text.MessageFormat;

import javax.inject.Inject;

import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import dagger.ObjectGraph;
import de.akuz.android.openhab.BootstrapApplication;
import de.akuz.android.openhab.R;
import de.akuz.android.openhab.core.CommunicationModule;
import de.akuz.android.openhab.core.requests.ItemCommandRequest;
import de.akuz.android.openhab.settings.OpenHABConnectionSettings;
import de.akuz.android.openhab.settings.OpenHABInstance;
import de.akuz.android.openhab.util.HABSweetiePreferences;

public class TaskerActionService extends IntentService implements
		RequestListener<Void> {

	public final static String EXTRA_ITEM_ID = "itemId";
	public final static String EXTRA_INSTANCE_ID = "instanceId";
	public final static String EXTRA_ITEM_COMMAND = "itemCommand";

	private final static String TAG = TaskerActionService.class.getSimpleName();

	private ObjectGraph objectGraph;

	@Inject
	SpiceManager spiceManager;

	@Inject
	HABSweetiePreferences prefs;

	@Inject
	ConnectivityManager conManager;

	public TaskerActionService(String name) {
		super(name);
		setIntentRedelivery(true);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		objectGraph = ((BootstrapApplication) getApplication())
				.getObjectGraph().plus(
						new CommunicationModule(getApplicationContext()));
		objectGraph.inject(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final Bundle bundle = intent
				.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
		long instanceId = bundle.getLong(EXTRA_INSTANCE_ID);
		String itemName = bundle.getString(EXTRA_ITEM_ID);
		String itemCommand = bundle.getString(EXTRA_ITEM_COMMAND);
		sendCommand(prefs.loadInstance(instanceId), itemName, itemCommand);

	}

	private void sendCommand(OpenHABInstance instance, String itemName,
			String itemCommand) {
		if (instance != null) {
			OpenHABConnectionSettings settings = instance
					.getSettingForCurrentNetwork(conManager);
			ItemCommandRequest request = new ItemCommandRequest(settings,
					itemName, itemCommand);
			spiceManager.execute(request, this);
		}
	}

	@Override
	public void onRequestFailure(SpiceException spiceException) {
		Log.e(TAG, "Request to openHAB failed", spiceException);
		Throwable cause = spiceException.getCause();
		Toast.makeText(
				getApplicationContext(),
				MessageFormat.format(
						getApplicationContext().getString(
								R.string.error_generic), cause.getMessage()),
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onRequestSuccess(Void result) {
		// Yay, nothing left todo.
		Log.v(TAG, "Request succeeded");

	}
}