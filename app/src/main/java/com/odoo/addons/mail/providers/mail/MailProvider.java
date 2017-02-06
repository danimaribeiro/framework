package com.odoo.addons.mail.providers.mail;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.provider.BaseModelProvider;

public class MailProvider extends BaseModelProvider {

	public static String PATH = "mail_message";
	//public static Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY, PATH);

	public static final int MAIL_INBOX = 12;
	public static final int MAIL_DETAIL_LIST = 13;

//	@Override
//	public boolean onCreate() {
//		boolean state = super.onCreate();
//		addURI(AUTHORITY, PATH + "/inbox", MAIL_INBOX);
//		addURI(AUTHORITY, PATH + "/details", MAIL_DETAIL_LIST);
//		return state;
//	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sort) {
		//TODO Muito importante descobrir essas URI
		int code =  MAIL_DETAIL_LIST; //this. matchURI(uri);
		if (code == MAIL_DETAIL_LIST) {
			Cursor cr = super.query(uri, projection, selection, selectionArgs,
					sort);
			OModel model = getModel(uri);
			MatrixCursor parents = new MatrixCursor(cr.getColumnNames());
			MatrixCursor childs = new MatrixCursor(cr.getColumnNames());
			if (cr.moveToFirst()) {
				do {
					int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
					if (parent_id == 0) {
						// add to parent
						List<Object> values = new ArrayList<Object>();
						for (String col : cr.getColumnNames()) {
							// TODO Verificar o que isto faz
							//OColumn c = new OColumn("");
							//c.setName(col);
							//values.add(model.createRecordRow(c, cr));
						}
						parents.addRow(values.toArray(new Object[values.size()]));
					} else {
						// add to child
						List<Object> values = new ArrayList<Object>();
						for (String col : cr.getColumnNames()) {
							//OColumn c = new OColumn("");
							//c.setName(col);
							//values.add(model.createRecordRow(c, cr));
						}
						childs.addRow(values.toArray(new Object[values.size()]));
					}
				} while (cr.moveToNext());
				MergeCursor mergedData = new MergeCursor(new Cursor[] {
						parents, childs });
				return mergedData;
			}
			parents.close();
			childs.close();
		}
		if (code == MAIL_INBOX) {
			Cursor cr = super.query(uri, projection, selection, selectionArgs,
					sort);
			List<String> parent_list = new ArrayList<String>();
			OModel model = getModel(uri);
			MatrixCursor newCr = null;
			if (cr.moveToFirst()) {
				newCr = new MatrixCursor(cr.getColumnNames());
				do {
					List<Object> values = new ArrayList<Object>();
					int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
					int _id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
					if (parent_id != 0
							&& !parent_list.contains("parent_" + parent_id)) {
						// Child found Creating parent row...
						List<ODataRow> parent_row = null; //model.select(parent_id);
								//.get(projection)
								//.addWhere(OColumn.ROW_ID, "=", parent_id)
								//.fetch(false);
						for (String col : cr.getColumnNames()) {
							if (parent_row.size() > 0
									&& parent_row.get(0).contains(col)) {
								ODataRow row = parent_row.get(0);
								if (col.equals("short_body")) {
									values.add(cr.getString(cr
											.getColumnIndex("short_body")));
								} else if (col.equals("date")) {
									values.add(cr.getString(cr
											.getColumnIndex("date")));
								} else if (col.equals("to_read")) {
									values.add(cr.getString(cr
											.getColumnIndex("to_read")));
								} else {
									values.add(row.get(col));
								}
							} else {
								//OColumn c = new OColumn("");
								//c.setName(col);
								//values.add(model.createRecordRow(c, cr));
							}
						}
						newCr.addRow(values.toArray(new Object[values.size()]));
						parent_list.add("parent_" + parent_id);
					} else {
						if (!parent_list.contains("parent_" + _id)
								&& parent_id == 0) {
							// Its parent. adding row
							for (String col : newCr.getColumnNames()) {
								//OColumn c = new OColumn("");
								//c.setName(col);
								//values.add(model.createRecordRow(c, cr));
							}
							parent_list.add("parent_" + _id);
							newCr.addRow(values.toArray(new Object[values
									.size()]));
						}
					}
				} while (cr.moveToNext());
				Context ctx = getContext();
				assert ctx != null;
				newCr.setNotificationUri(ctx.getContentResolver(), uri);
				return newCr;
			}
		}
		return super.query(uri, projection, selection, selectionArgs, sort);
	}

    @Override
    public String authority() {
        return MailMessage.AUTHORITY;
    }

//	@Override
//	public String authority() {
//		return MailProvider.AUTHORITY;
//	}
//
//	@Override
//	public OModel model(Context context) {
//		return new MailMessage(context);
//	}
//
//	@Override
//	public String path() {
//		return MailProvider.PATH;
//	}
//
//	@Override
//	public Uri uri() {
//		return MailProvider.CONTENT_URI;
//	}

}
