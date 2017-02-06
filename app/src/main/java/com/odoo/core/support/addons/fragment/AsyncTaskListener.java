package com.odoo.core.support.addons.fragment;

public interface AsyncTaskListener {
    public Object onPerformTask();

    public void onFinish(Object result);
}
