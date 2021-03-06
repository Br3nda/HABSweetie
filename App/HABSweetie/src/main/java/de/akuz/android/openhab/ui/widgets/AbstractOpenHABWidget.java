package de.akuz.android.openhab.ui.widgets;

import javax.inject.Inject;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import de.akuz.android.openhab.core.objects.Item;
import de.akuz.android.openhab.core.objects.Page;
import de.akuz.android.openhab.core.objects.Widget;
import de.akuz.android.openhab.util.HABSweetiePreferences;

public abstract class AbstractOpenHABWidget extends LinearLayout implements
		OnClickListener, ItemUpdateListener {

	protected final static String TAG = AbstractOpenHABWidget.class
			.getSimpleName();

	protected Widget widget;

	private View rootView;

	private LayoutInflater layoutInflater;

	protected ItemCommandInterface commandInterface;

	private SendingDelayer sendingDelayer;

	@Inject
	OpenHABWidgetFactory widgetFactory;

	@Inject
	HABSweetiePreferences prefs;

	public AbstractOpenHABWidget(Context context, Widget widget) {
		super(context);
		layoutInflater = LayoutInflater.from(context);
		this.widget = widget;
		buildUi();
	}

	public final void updateWidget(Widget widget) {
		this.widget = widget;
		if (hasChildren()) {
			rootView.setOnClickListener(this);
		}
		widgetUpdated(widget);
		updateItem(widget.getItem());

	}

	protected abstract void widgetUpdated(Widget widget);

	protected void setView(int resId) {
		rootView = layoutInflater.inflate(resId, null);
		removeAllViews();
		addView(rootView);
	}

	@SuppressWarnings("unchecked")
	protected <T extends View> T findView(int id) {
		return (T) rootView.findViewById(id);
	}

	public boolean hasChildren() {
		return widget.getLinkedPage() != null;
	}

	public void setItemCommandInterface(ItemCommandInterface commandInterface) {
		this.commandInterface = commandInterface;
		if (sendingDelayer != null) {
			sendingDelayer.interrupt();
		}
		sendingDelayer = new SendingDelayer(widget.getItem(), commandInterface,
				this);
	}

	public void setMargins(int right, int left, int top, int bottom) {
		LayoutParams params = (LayoutParams) rootView.getLayoutParams();
		params.bottomMargin = bottom;
		params.topMargin = top;
		params.leftMargin = left;
		params.rightMargin = right;
		params.height = LayoutParams.WRAP_CONTENT;
		params.width = LayoutParams.MATCH_PARENT;
		rootView.setLayoutParams(params);
	}

	public void setWidgetBackground(int resId) {
		if (rootView != null) {
			rootView.setBackgroundResource(resId);
		}
	}

	protected abstract void buildUi();

	protected void sendCommand(String command) {
		if (commandInterface != null) {
			commandInterface.sendCommand(widget.getItem(), command, this);
		} else {
			Log.w(TAG,
					"Command Interface is NULL, therefore I can't send anything");
		}
	}

	protected void sendCommandDelayed(String command) {
		if (prefs.getCommandSendingDelay() == 0) {
			sendCommand(command);
			return;
		}
		if (sendingDelayer != null && sendingDelayer.isAlive()) {
			sendingDelayer.updateLastTriggerTimeAndCommand(
					System.currentTimeMillis(), command);
		} else {
			sendingDelayer = new SendingDelayer(widget.getItem(),
					commandInterface, this);
			sendingDelayer.send(command);
		}
	}

	public static interface ItemCommandInterface {
		public void sendCommand(Item item, String command,
				ItemUpdateListener listener);

		public void loadPage(Page page);

		public boolean serverPushEnabled();

		public void showDialog(DialogFragment dialog);
	}

	@Override
	public void onClick(View v) {
		Log.i(TAG, "Got click event in list item");
		if (commandInterface != null) {
			Log.i(TAG,
					"Command interface is not NULL and Widget has child page, loading child page");
			commandInterface.loadPage(widget.getLinkedPage());
		} else {
			Log.w(TAG,
					"Received onClick interface but commandInterface is NULL");
		}

	}

	@Override
	public void itemUpdateReceived(Item item) {
		updateItem(item);

	}

	protected View getWidgetRootView() {
		return rootView;
	}

	public abstract void updateItem(Item item);

	private class SendingDelayer extends Thread {

		private long sendDelay = prefs.getCommandSendingDelay();

		private long lastTriggerTime;

		private String lastCommand;

		private ItemCommandInterface commandInterface;

		private Item item;

		private ItemUpdateListener updateListener;

		public SendingDelayer(Item item, ItemCommandInterface commandInterface,
				ItemUpdateListener updateListener) {
			this.item = item;
			this.commandInterface = commandInterface;
			this.updateListener = updateListener;
		}

		public void updateLastTriggerTimeAndCommand(long millis, String command) {
			this.lastTriggerTime = millis;
			this.lastCommand = command;
		}

		@Override
		public void run() {
			while ((lastTriggerTime + sendDelay) > System.currentTimeMillis()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// Ignore
				}
			}
			if (!isInterrupted()) {
				commandInterface.sendCommand(item, lastCommand, updateListener);
			}

		}

		public void send(String command) {
			lastCommand = command;
			lastTriggerTime = System.currentTimeMillis();
			if (!this.isAlive()) {
				start();
			}
		}

	}

}
