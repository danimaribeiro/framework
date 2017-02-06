/*
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 */
package com.odoo.base.addons.mail;

import android.content.Context;
import android.net.Uri;

import com.odoo.base.addons.mail.provider.MailFollowerProvider;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.OColumn.RelationType;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.provider.BaseModelProvider;
import com.odoo.core.support.OUser;

public class MailFollowers extends OModel {

    public static final String AUTHORITY = "com.odoo.core.base.provider.content.sync.mailfollower";
    private Context mContext;

    OColumn res_model = new OColumn("Model", OText.class);
    OColumn res_id = new OColumn("Res ID", OInteger.class);
    OColumn partner_id = new OColumn("Partner", ResPartner.class,
            RelationType.ManyToOne);

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public MailFollowers(Context context, OUser user) {
        super(context, "mail.followers", user);
        mContext = context;
    }
}
