package com.odoo.addons.mail.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.OColumn.RelationType;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OHtml;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.StringUtils;

import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OArguments;

public class MailMessage extends OModel {
	public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
			".core.provider.content.sync.mail_message";

	private Context mContext = null;

	OColumn type = new OColumn("Type", OInteger.class).setDefaultValue("email");
	OColumn email_from = new OColumn("Email", OVarchar.class).setSize(64)
			.setDefaultValue("false");
	OColumn author_id = new OColumn("Author", ResPartner.class,
			RelationType.ManyToOne);
	OColumn partner_ids = new OColumn("To", ResPartner.class,
			RelationType.ManyToMany).setRequired().setRecordSyncLimit(25);
	OColumn notified_partner_ids = new OColumn("Notified Partners",
			ResPartner.class, RelationType.ManyToMany).setRecordSyncLimit(25);
	OColumn attachment_ids = new OColumn("Attachments", IrAttachment.class,
			RelationType.ManyToMany);
	OColumn parent_id = new OColumn("Parent", MailMessage.class,
			RelationType.ManyToOne).setDefaultValue(0);
	OColumn child_ids = new OColumn("Childs", MailMessage.class,
			RelationType.OneToMany).setRelatedColumn("parent_id");
	OColumn model = new OColumn("Model", OVarchar.class).setSize(64)
			.setDefaultValue("false");
	OColumn res_id = new OColumn("Resource ID", OInteger.class).setDefaultValue(0);
	OColumn record_name = new OColumn("Record name", OText.class)
			.setDefaultValue("false");
	OColumn notification_ids = new OColumn("Notifications",
			MailNotification.class, RelationType.OneToMany)
			.setRelatedColumn("message_id");
	OColumn subject = new OColumn("Subject", OVarchar.class).setSize(100).setDefaultValue(
			"false").setRequired();
	OColumn date = new OColumn("Date", ODateTime.class).setDefaultValue(
			ODateUtils.getUTCDate(ODateUtils.DEFAULT_FORMAT));
	OColumn body = new OColumn("Body", OHtml.class).setDefaultValue("").setRequired();
	OColumn vote_user_ids = new OColumn("Voters", ResUsers.class,
			RelationType.ManyToMany);

	@Odoo.Functional(method = "getToRead", store = true, depends = { "notification_ids" })
	OColumn to_read = new OColumn("To Read", OBoolean.class).setDefaultValue(1);
	@Odoo.Functional(method = "getStarred", store = true, depends = { "notification_ids" })
	OColumn starred = new OColumn("Starred", OBoolean.class).setDefaultValue(0);

	@Odoo.Functional(method = "storeAuthorName", store = true, depends = {
			"author_id", "email_from" })
	OColumn author_name = new OColumn("Author Name", OVarchar.class)
			.setLocalColumn();

	@Odoo.Functional(method = "storeShortBody", store = true, depends = { "body" })
	OColumn short_body = new OColumn("Short Body", OVarchar.class)
			.setLocalColumn();
	// Functional Fields
	@Odoo.Functional(method = "setMessageTitle", store = true, depends = {
			"record_name", "subject" })
	OColumn message_title = new OColumn("Title", OVarchar.class)
			.setLocalColumn();
	@Odoo.Functional(method = "hasVoted")
	OColumn has_voted = new OColumn("Has voted", OVarchar.class);
	@Odoo.Functional(method = "getVoteCounter")
	OColumn vote_counter = new OColumn("Votes", OInteger.class);
	@Odoo.Functional(method = "getPartnersName")
	OColumn partners_name = new OColumn("Partners", OVarchar.class);

	@Odoo.Functional(method = "getReplies", depends = { "child_ids" }, store = true)
	OColumn total_childs = new OColumn("Replies", OVarchar.class)
			.setLocalColumn().setDefaultValue(0);
	private List<Integer> mNewCreateIds = new ArrayList<Integer>();
	private MailNotification notification = null;

	public MailMessage(Context context, OUser oUser) {
		super(context, "mail.message", oUser);
		mContext = context;
		notification = new MailNotification(mContext, oUser);
		write_date.setDefaultValue(false);
		create_date.setDefaultValue(false);
		// TODO estava setando false aqui
		to_read.setLocalColumn();
		starred.setLocalColumn();
	}

	public String getReplies(OValues values) {
		JSONArray childs = (JSONArray) values.get("child_ids");
		if (childs.length() > 0)
			return childs.length() + "";
		return "0";
	}

	public Integer author_id() {
		return new ResPartner(mContext, getUser()).selectRowId(getUser().getPartnerId());
	}

	public Boolean getValueofReadUnReadField(int id) {
		boolean read = false;
        List<ODataRow> row = (List<ODataRow>) select(new String[] {"to_read"});
        if (row.size() > 0)
            read = row.get(0).getBoolean("to_read");
		return read;
	}

	public Boolean getToRead(OValues vals) {
		try {
			JSONArray ids = (JSONArray) vals.get("notification_ids");
			if (ids.length() > 0) {
				List<ODataRow> noti = notification.select(new String[] {"is_read", "read"});
				if (noti.size() > 0)
					return (noti.get(0).contains("is_read")) ? !noti.get(0)
							.getBoolean("is_read") : !noti.get(0).getBoolean("read");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return vals.getBoolean("to_read");
	}

	public Boolean getStarred(OValues vals) {
		try {
			JSONArray ids = (JSONArray) vals.get("notification_ids");
			if (ids.length() > 0) {
                List<ODataRow> noti = notification.select(new String[] {"starred"});
                if (noti.size() > 0)
					return noti.get(0).getBoolean("starred");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return vals.getBoolean("starred");
	}

	public boolean markAsTodo(Cursor c, Boolean todo_state) {
		try {
			OArguments args = new OArguments();
			args.add(new JSONArray().put(c.getInt(c.getColumnIndex("id"))));
			args.add(todo_state);
			args.add(true);
			getServerDataHelper().callMethod("set_message_starred", args, null);
			ORecordValues values = new ORecordValues();
			values.put("starred", (todo_state) ? 1 : 0);
			values.put("to_read", 1);
            getServerDataHelper().updateOnServer(values, c.getInt(c.getColumnIndex(OColumn.ROW_ID)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

    //TODO Modificar aqui
//	@Override
//	public int create(ORecordValues values) {
//		int newId = super.create(values);
//		if (values.contains("to_read") && values.getBoolean("to_read"))
//			mNewCreateIds.add(newId);
//		return newId;
//	}

	public List<Integer> newMessageIds() {
		return mNewCreateIds;
	}

	public Integer sendQuickReply(OValues values, String subject, String body,
			Integer parent_id, Integer parent_childs) {
		body += mContext.getResources().getString(R.string.mail_watermark);
		OValues vals = new OValues();
		vals.put("author_name", getUser().getName());
		vals.put("subject", subject);
		vals.put("body", body);
		vals.put("short_body", storeShortBody(vals));
		vals.put("parent_id", parent_id);
		vals.put("author_id", author_id());
		vals.put("to_read", 0);
		vals.put("starred", 0);
		vals.put("date", ODateUtils.getUTCDate(ODateUtils.DEFAULT_FORMAT));
		List<Integer> p_ids = new ArrayList<Integer>();
		//for (ODataRow partner : select(parent_id).getM2MRecord("partner_ids")
		//   	.browseEach()) {
		//	p_ids.add(partner.getInt(OColumn.ROW_ID));
		//}
		vals.put("partner_ids", p_ids.toString());
		if (values != null)
			vals.put("attachment_ids", values.get("attachment_ids"));
		// Updating parent childs
		OValues parent_vals = new OValues();
		parent_vals.put("total_childs", (parent_childs + 1));
		//resolver().update(parent_id, parent_vals);
		return 0; //resolver().insert(vals);
	}

	public void markMailReadUnread(int mail_id, Boolean to_read) {
		try {
//			ODataRow row = select(mail_id);
//			List<Integer> mailIds = new ArrayList<Integer>();
//			int parent_id = row.getM2ORecord("parent_id").getId();
//			if (parent_id != 0) {
//				row = row.get(0).getM2ORecord("parent_id").browse();
//			}
//			parent_id = row.getInt("id");
//			for (ODataRow child : row.getO2MRecord("child_ids").browseEach()) {
//				mailIds.add(child.getInt("id"));
//			}
//			mailIds.add(parent_id);
//			Object default_model = false;
//			Object default_res_id = false;
//			default_model = row.get("model");
//			default_res_id = row.get("res_id");
//			JSONObject newContext = new JSONObject();
//			newContext.put("default_parent_id", parent_id);
//			newContext.put("default_model", default_model);
//			newContext.put("default_res_id", default_res_id);
//
//			OArguments args = new OArguments();
//			args.add(new JSONArray(mailIds.toString()));
//			args.add(!to_read);
//			args.add(true);
//			args.add(newContext);
//			Integer updated = (Integer) getSyncHelper().callMethod(
//					"set_message_read", args, null);
//			if (updated > 0) {
//				for (int id : mailIds) {
//					OValues values = new OValues();
//					values.put("is_dirty", 0);
//					resolver().update(selectRowId(id), values);
//				}
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getPartnersName(ODataRow row) {
		String partners = "to ";
		List<String> partners_name = new ArrayList<String>();
		for (ODataRow p : row.getM2MRecord("partner_ids").browseEach()) {
			if (partners_name.size() < 10) {
				partners_name.add(p.getString("name"));
			}
		}
		return partners + TextUtils.join(", ", partners_name);
	}

	public String getVoteCounter(ODataRow row) {
		int votes = row.getM2MRecord(vote_user_ids.getName()).getRelIds()
				.size();
		if (votes > 0)
			return votes + "";
		return "";
	}

	public Boolean hasVoted(ODataRow row) {
		for (Integer id : row.getM2MRecord("vote_user_ids").getRelIds()) {
			if (id == author_id()) {
				return true;
			}
		}
		return false;
	}

	public Boolean hasAttachment(ODataRow row) {
		if (row.getM2MRecord("attachment_ids").getRelIds().size() > 0)
			return true;
		return false;
	}

	public String setMessageTitle(OValues row) {
		String title = "false";
		if (!row.getString("record_name").equals("false"))
			title = row.getString("record_name");
		if (!title.equals("false") && !row.getString("subject").equals("false"))
			title += ": " + row.getString("subject");
		if (title.equals("false") && !row.getString("subject").equals("false"))
			title = row.getString("subject");
		if (title.equals("false"))
			title = "comment";
		return title;
	}

	public String getChildCount(ODataRow row) {
		int childs = row.getO2MRecord("child_ids").getIds(this).size();
		return (childs > 0) ? childs + " replies" : " ";
	}

	public String storeAuthorName(OValues row) {
		try {
			if (row.getString("author_id").equals("false"))
				return row.getString("email_from");
			JSONArray author_id = (JSONArray) row.get("author_id");
			return author_id.getString(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return row.getString("email_from");
	}

	public String storeShortBody(OValues row) {
		String body = StringUtils.htmlToString(row.getString("body"));
		int end = (body.length() > 100) ? 100 : body.length();
		return body.substring(0, end);
	}

	@Override
	public ODomain defaultDomain() {
		Integer user_id = getUser().getUserId();
		ODomain domain = new ODomain();
		domain.add("|");
		domain.add("partner_ids.user_ids", "in", new JSONArray().put(user_id));
		domain.add("|");
		domain.add("notification_ids.partner_id.user_ids", "in",
				new JSONArray().put(user_id));
		domain.add("author_id.user_ids", "in", new JSONArray().put(user_id));
		return domain;
	}


	@Override
	public Uri uri() {
		return buildURI(AUTHORITY);
	}

	public Uri mailUri() {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority());
		uriBuilder.path(getDatabaseLocalPath() + "/inbox");
		uriBuilder.scheme("content");
		return uriBuilder.build();
	}

	public Uri mailDetailUri() {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority());
		uriBuilder.path(getDatabaseLocalPath() + "/details");
		uriBuilder.scheme("content");
		return uriBuilder.build();
	}
}
