package com.odoo.addons.mail.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.odoo.OdooActivity;
import com.odoo.R;
import com.odoo.Trustcode;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ServerDataHelper;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.support.OUser;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.ISyncFinishListener;
import com.odoo.core.service.OSyncService;
import com.odoo.core.utils.JSONUtils;
import com.odoo.core.utils.notification.ONotificationBuilder;

public class MailSyncService extends OSyncService implements
		ISyncFinishListener {
	public static final String TAG = MailSyncService.class.getSimpleName();

	@Override
	public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
		return new OSyncAdapter(context, MailMessage.class, service, true);
	}

	@Override
	public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
		Context mContext = getApplicationContext();
		try {
			MailMessage mdb = new MailMessage(mContext, user);
			if (sendMails(mContext, user, mdb, mdb.getServerDataHelper())) {
				updateMails(user);
			}
			ODomain domain = new ODomain();
			if (extras.containsKey(MailGroupSyncService.KEY_GROUP_IDS)) {
				JSONArray group_ids = new JSONArray(
						extras.getString(MailGroupSyncService.KEY_GROUP_IDS));
				domain.add("res_id", "in", group_ids);
				domain.add("model", "=", "mail.group");
			}
			adapter.syncDataLimit(30).onSyncFinish(this).setDomain(domain);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Boolean sendMails(Context context, OUser user, MailMessage mails,
			ServerDataHelper helper) {
		for (ODataRow mail : mails.select(new String[] {"partner_ids", "parent_id"}, "id = ?", new String[] { "0" })) {
			try {
				JSONArray partner_ids = new JSONArray();
				JSONArray p_ids = new JSONArray();
				List<ODataRow> partners = mail.getM2MRecord("partner_ids")
						.browseEach();
				p_ids = JSONUtils.toArray(getPartnersServerId(partners));
				partner_ids.put(6);
				partner_ids.put(false);
				partner_ids.put(p_ids);
				// Attachments
				List<Integer> attachments = getAttachmentIds(mail);
				Object attachment_ids = false;
				if (attachments.size() > 0) {
					JSONArray attachmentIds = new JSONArray();
					attachmentIds.put(6);
					attachmentIds.put(false);
					attachmentIds.put(JSONUtils.toArray(attachments));
					attachment_ids = new JSONArray().put(attachmentIds);
				}
				if ((Integer) mail.getM2ORecord("parent_id").getId() == 0) {
					_sendMail(mail, partner_ids, attachment_ids, helper, mails);
				} else {
					JSONArray attachments_reply = new JSONArray();
					if (!attachment_ids.toString().equals("false")) {
						attachments_reply = new JSONArray(JSONUtils.toArray(
								attachments).toString());
					}
					ODataRow parent = mail.getM2ORecord("parent_id").browse();
					sendReply(context, parent, mail, partner_ids,
							attachments_reply, helper, mails);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "send Mails()" + e.getMessage());
			}
		}
		return true;
	}

	private void _sendMail(ODataRow mail, JSONArray partner_ids,
			Object attachment_ids, ServerDataHelper helper, MailMessage mails) {
		try {

			JSONObject arguments = new JSONObject();
			arguments.put("composition_mode", "comment");
			arguments.put("model", false);
			arguments.put("parent_id", false);
			arguments.put("subject", mail.getString("subject"));
			arguments.put("body", mail.getString("body"));
			arguments.put("post", true);
			arguments.put("notify", false);
			arguments.put("same_thread", true);
			arguments.put("use_active_domain", false);
			arguments.put("reply_to", false);
			arguments.put("res_id", false);
			arguments.put("record_name", false);
			arguments.put("partner_ids", new JSONArray().put(partner_ids));
			arguments.put("template_id", false);
			arguments.put("attachment_ids", attachment_ids);

			HashMap<String, Object> kwargs = new HashMap<String, Object>();
			OArguments args = new OArguments();
			args.add(arguments);
			String model = "mail.compose.message";
			// Creating compose message
			Object message_id = helper.callMethod(model, "create", args, null, kwargs);
			// sending mail
			args = new OArguments();
			args.add(new JSONArray().put(message_id));
			helper.callMethod(model, "send_mail", args, null, null);
            mails.allowUpdateRecordOnServer();
            mails.delete(mail.getInt(OColumn.ROW_ID));
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "_sendMail() : " + e.getMessage());
		}
	}

	private void sendReply(Context context, ODataRow parent, ODataRow mail,
			JSONArray partner_ids, Object attachment_ids, ServerDataHelper helper,
			MailMessage mails) {
		try {
			// sending reply
			String model = (parent.getString("model").equals("false")) ? "mail.thread"
					: parent.getString("model");
			String method = "message_post";
			Object res_model = (parent.getString("model").equals("false")) ? false
					: parent.getString("model");
			Object res_id = (parent.getInt("res_id") == 0) ? false : parent
					.getInt("res_id");

			JSONObject jContext = new JSONObject();
			jContext.put("default_model", res_model);
			jContext.put("default_res_id", res_id);
			jContext.put("default_parent_id", parent.getInt("id"));
			jContext.put("mail_post_autofollow", true);
			jContext.put("mail_post_autofollow_partner_ids", new JSONArray());
			HashMap<String, Object> kwargs = new HashMap<String, Object>();
			kwargs.put("context", jContext);
			kwargs.put("subject", mail.getString("subject"));
			kwargs.put("body", mail.getString("body"));
			kwargs.put("parent_id", parent.getInt("id"));
			kwargs.put("attachment_ids", attachment_ids);
			kwargs.put("partner_ids", new JSONArray().put(partner_ids));

			OArguments args = new OArguments();
			args.add(new JSONArray().put(res_id));
			Integer messageId = (Integer) helper.callMethod(model, method,
					args, null, kwargs);
			ORecordValues vals = new ORecordValues();
			vals.put(OColumn.ROW_ID, mail.getInt(OColumn.ROW_ID));
			vals.put("id", messageId);
            //TODO Atualizar
            //mails.update(vals.getInt(OColumn.ROW_ID), vals);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "send Reply()" + e.getMessage());
		}
	}

	private List<Integer> getAttachmentIds(ODataRow row) {
		List<Integer> ids = new ArrayList<Integer>();
		try {
			IrAttachment helper = new IrAttachment(getApplicationContext(), null);
			for (ODataRow attachment : row.getM2MRecord("attachment_ids")
					.browseEach()) {
				//TODO Aqui deve atualizar no servidor
				int id = helper.getServerDataHelper().updateOnServer(null, attachment.getInt("Id"));
				if (id != 0) {
					ids.add(id);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ids;
	}

	private Boolean updateMails(OUser user) {
		_updateUpStream(user);
		_updateDownStream(user);
		return true;
	}

	private void _updateUpStream(OUser user) {
		Context context = getApplicationContext();
		MailMessage mail = new MailMessage(context, user);
		Cursor cr = context.getContentResolver().query(mail.uri(),
				new String[] { "id", OColumn.ROW_ID, "to_read", "parent_id" },
				"is_dirty = ? or is_dirty = ?", new String[] { "1", "true" },
				null);
		List<Integer> finished = new ArrayList<Integer>();
		if (cr.moveToFirst()) {
			do {
				int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
				int sync_id = parent_id;
				if (parent_id == 0) {
					sync_id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
				}
				if (finished.indexOf(sync_id) <= -1) {
					Boolean to_read = (cr.getInt(cr.getColumnIndex("to_read")) == 1) ? true
							: false;
					mail.markMailReadUnread(sync_id, to_read);
					finished.add(sync_id);
				}
			} while (cr.moveToNext());
		}
		cr.close();
	}

	private void _updateDownStream(OUser user) {
		Context context = getApplicationContext();
		Trustcode app = (Trustcode) context;
		MailMessage mail = new MailMessage(context, user);
		ResUsers users = new ResUsers(context, user);
		try {
			Odoo odoo = app.getOdoo(user);
			ODomain domain = new ODomain();
            //TODO Ajustar domain
			//domain.add("id", "in", new JSONArray(mail.ids().toString()));
			JSONObject fields = new JSONObject();
			fields.accumulate("fields", "to_read");
			fields.accumulate("fields", "starred");
			fields.accumulate("fields", "vote_user_ids");
			JSONArray result = new JSONArray(); // odoo.search_read(mail.getModelName(), fields,
					//domain.get()).getJSONArray("records");
			for (int i = 0; i < result.length(); i++) {
				JSONObject row = result.getJSONObject(i);
				int id = row.getInt("id");
				int to_read = (row.getBoolean("to_read")) ? 1 : 0;
				int starred = (row.getBoolean("starred")) ? 1 : 0;
				List<Integer> vote_user_ids = JSONUtils.toList(row
						.getJSONArray("vote_user_ids"));
				int row_id = mail.selectRowId(id);
				OValues values = new OValues();
				values.put("to_read", to_read);
				values.put("starred", starred);
				List<Integer> local_vote_user_ids = new ArrayList<Integer>();
				for (Integer user_id : vote_user_ids) {
					OValues vals = new OValues();
					vals.put("id", user_id);
					//local_vote_user_ids.add(users.createORReplace(vals));
				}
				values.put("vote_user_ids", local_vote_user_ids.toString());
				//mail.resolver().update(row_id, values);
			}
			OdooFields userFields = new OdooFields(new String[] {
					"login", "name" });
			JSONArray user_result = new JSONArray(); //odoo.search_read(users.getModelName(),
					//userFields.get(), null).getJSONArray("records");
			List<Integer> local_uids = new ArrayList<Integer>();
			for (ODataRow rows : users.select()) {
				local_uids.add(rows.getInt("id"));
			}
			for (int i = 0; i < user_result.length(); i++) {
				JSONObject row = user_result.getJSONObject(i);
				if (!local_uids.contains(row.getInt("id"))) {
					OValues values = new OValues();
					values.put("id", row.getInt("id"));
					values.put("name", row.getString("name"));
					values.put("login", row.getString("login"));
					values.put("is_dirty", false);
					//users.createORReplace(values);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
		Trustcode app = (Trustcode) getApplicationContext();
		int newTotal = (int) syncResult.stats.numInserts;
		newTotal = (newTotal != 0) ? newTotal / 2 : newTotal;
		//if (!app.appOnTop() && newTotal > 0) {
            //TODO Verificar o Id 1 aqui
			ONotificationBuilder notification = new ONotificationBuilder(
					getApplicationContext(), 1);
			notification.setAutoCancel(true);
			notification.setIcon(R.drawable.ic_odoo_o);
			notification.setTitle(newTotal + " unread messages");
			notification.setText(newTotal + " new message received (Odoo)");
			Intent rIntent = new Intent(getApplicationContext(),
					OdooActivity.class);
			notification.setResultIntent(rIntent);
			notification.build().show();
		//}
		return null;
	}

	public List<Integer> getPartnersServerId(List<ODataRow> rows) {
		List<Integer> pIds = new ArrayList<Integer>();

		for (ODataRow row : rows) {
			int server_id = row.getInt("id");
			if (server_id == 0) {
				ODomain domain = new ODomain();
				domain.add("email", "ilike", row.getString("email"));
				server_id = getPartnerId(domain, row);
			}
			pIds.add(server_id);
		}

		return pIds;
	}

	public int getPartnerId(ODomain domain, ODataRow row) {
		int server_id = 0;
		try {
			Context context = getApplicationContext();
			ResPartner partner = new ResPartner(context, null);
			Trustcode app = (Trustcode) context;
			Odoo odoo = app.getOdoo(null);
			JSONObject fields = new JSONObject();
			fields.accumulate("fields", "email");
			//JSONObject result = odoo.search_read(partner.getModelName(),
			//		fields, domain.get());
			JSONArray records = null;//result.getJSONArray("records");
			if (records.length() > 0) {
				JSONObject record = records.getJSONObject(0);
				server_id = record.getInt("id");
				OValues vals = new OValues();
				vals.put("id", server_id);
				//partner.resolver().update(row.getInt(OColumn.ROW_ID), vals);
			} else {
				// Creating partner on server
				//server_id = partner.getSyncHelper().create(partner, row);
				//Log.v(TAG, "partner created on server " + server_id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return server_id;
	}
}
