package com.odoo.addons.mail.models;

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.odoo.R;
import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.base.addons.mail.MailFollowers;
import com.odoo.core.orm.ServerDataHelper;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.OColumn.RelationType;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.JSONUtils;

import com.odoo.core.rpc.helper.OArguments;

public class MailGroup extends OModel {
	Context mContext = null;

	OColumn name = new OColumn("Name", OVarchar.class).setSize(64);
	OColumn description = new OColumn("Description", OText.class);
	OColumn image_medium = new OColumn("Image_Medium", OBlob.class);
	OColumn message_follower_ids = new OColumn("Followers",
			MailFollowers.class, RelationType.ManyToMany);
	@Odoo.Functional(method = "hasFollowed", depends = { "message_follower_ids" }, store = true)
	OColumn has_followed = new OColumn("Followed", OBoolean.class)
			.setDefaultValue(0).setLocalColumn();

	public MailGroup(Context context, OUser oUser) {
		super(context, "mail.group", oUser);
		mContext = context;
	}

	public int hasFollowed(OValues vals) {
		List<Integer> ids = JSONUtils.toList((JSONArray) vals
				.get("message_follower_ids"));
		if (ids.indexOf(getUser().getPartnerId()) > -1) {
			return 1;
		}
		return 0;
	}

	public void followUnfollowGroup(int group_id, boolean follow) {
		try {
			ServerDataHelper sync = getServerDataHelper();
			ORecordValues values = new ORecordValues();
			OArguments args = new OArguments();
			args.add(new JSONArray().put(selectServerId(group_id)));
			if (follow) {
				// Action Follow
				sync.callMethod("action_follow", args, new HashMap<String, Object>());
				values.put("has_followed", 1);
			} else {
				// Action unfollow
				sync.callMethod("action_unfollow", args, new HashMap<String, Object>());
				values.put("has_followed", 0);
			}
			sync.updateOnServer(values, group_id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
