package com.odoo.base.addons.mail.provider;

import android.content.Context;
import android.net.Uri;

import com.odoo.base.addons.mail.MailFollowers;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.provider.BaseModelProvider;

public class MailFollowerProvider extends BaseModelProvider {

    @Override
    public String authority() {
        return MailFollowers.AUTHORITY;
    }

}
