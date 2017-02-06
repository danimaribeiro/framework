package com.odoo.addons.mail.providers.notification;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.mail.models.MailNotification;
import com.odoo.core.orm.provider.BaseModelProvider;
import com.odoo.core.orm.OModel;

public class MailNotificationProvider extends BaseModelProvider {

	public static String AUTHORITY = "com.odoo.addons.mail.providers.notification";
	public static String PATH = "mail_notification";
	//public static Uri CONTENT_URI = BaseModelProvider.buildURI(AUTHORITY, PATH);

//	@Override
//	public String authority() {
//		return MailNotificationProvider.AUTHORITY;
//	}
//
//	@Override
//	public OModel model(Context context) {
//		return new MailNotification(context);
//	}
//
//	@Override
//	public String path() {
//		return MailNotificationProvider.PATH;
//	}
//
//	@Override
//	public Uri uri() {
//		return MailNotificationProvider.CONTENT_URI;
//	}

}
