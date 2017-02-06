package com.odoo.addons.mail.models;

import java.util.List;

import android.content.Context;

import com.odoo.addons.mail.providers.notification.MailNotificationProvider;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.OColumn.RelationType;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.provider.BaseModelProvider;
import com.odoo.core.support.OUser;
import com.odoo.core.rpc.helper.ODomain;

public class MailNotification extends OModel {

	@Odoo.api.v7
	OColumn read = new OColumn("Read", OBoolean.class).setDefaultValue(false);

	@Odoo.api.v8
	@Odoo.api.v10
	OColumn is_read = new OColumn("Is Read", OBoolean.class).setDefaultValue(true);
	OColumn starred = new OColumn("Starred", OBoolean.class).setDefaultValue(false);
	OColumn partner_id = new OColumn("Partner_id", ResPartner.class,
			RelationType.ManyToOne);
	OColumn message_id = new OColumn("Message_id", MailMessage.class,
			RelationType.ManyToOne).setLocalColumn();

	Context mContext = null;

	public MailNotification(Context context, OUser oUser) {
		super(context, "mail.notification", oUser);
		mContext = context;
	}

	public Boolean getStarred(int msgid) {
		boolean starred = false;
		List<ODataRow> row = (List<ODataRow>) select(
				new String[] {"starred"}, "message_id = ?",  new String[] { msgid + "" });
		if (row.size() > 0)
			starred = row.get(0).getBoolean("starred");
		return starred;
	}

	public Boolean getIsread(int msgid) {
		boolean isread = false;
		List<ODataRow> row = (List<ODataRow>) select(
				new String[] { "is_read" }, "message_id = ?", new String[] { msgid + "" });
		isread = row.get(0).getBoolean("is_read");
		return isread;
	}

	@Override
	public ODomain defaultDomain() {
		ODomain domain = new ODomain();
		domain.add("partner_id", "=", getUser().getPartnerId());
		return domain;
	}

	// TODO Verificar estes checks
//	@Override
//	public Boolean checkForLocalLatestUpdate() {
//		return false;
//	}
//
//	@Override
//	public Boolean checkForLocalUpdate() {
//		return false;
//	}
//
//	@Override
//	public Boolean canCreateOnServer() {
//		return false;
//	}
//
//	@Override
//	public Boolean canDeleteFromLocal() {
//		return false;
//	}
//
//	@Override
//	public Boolean canDeleteFromServer() {
//		return false;
//	}
//
//	@Override
//	public Boolean canUpdateToServer() {
//		return false;
//	}
//
//	@Override
//	public Boolean checkForCreateDate() {
//		return false;
//	}
//
//	@Override
//	public Boolean checkForWriteDate() {
//		return false;
//	}
//
//	@Override
//	public OContentProvider getContentProvider() {
//		return new MailNotificationProvider();
//	}
}
