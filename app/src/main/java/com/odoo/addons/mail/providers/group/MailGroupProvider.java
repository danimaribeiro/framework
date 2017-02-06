package com.odoo.addons.mail.providers.group;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.core.orm.provider.BaseModelProvider;
import com.odoo.core.orm.OModel;

public class MailGroupProvider extends BaseModelProvider {

	public static String AUTHORITY = "com.odoo.addons.mail.providers.group";
	public static String PATH = "mail_group";
	//public static Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY, PATH);

//	@Override
//	public String authority() {
//		return MailGroupProvider.AUTHORITY;
//	}
//
//	@Override
//	public OModel model(Context context) {
//		return new MailGroup(context);
//	}
//
//	@Override
//	public String path() {
//		return MailGroupProvider.PATH;
//	}
//
//	@Override
//	public Uri uri() {
//		return MailGroupProvider.CONTENT_URI;
//	}

}
