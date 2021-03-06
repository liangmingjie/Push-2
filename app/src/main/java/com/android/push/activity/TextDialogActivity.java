/************************************************************************
 * 版权所有 (C)2012, 深圳市康佳集团股份有限公司。
 *
 * 文件名称： TextDialogActivity.java
 * 内容摘要： Push的弹窗展示的文字、链接文字形式实现
 * 当前版本： 1.0
 * 作 者： 	 李翊星
 * 完成日期： 2015-11-24
 * 修改记录：
 * 修改日期：
 * 版 本 号：
 * 修 改 人：
 * 修改内容：
 **************************************************************************/

package com.android.push.activity;

import com.android.push.R;
import com.android.push.config.Config;
import com.android.push.constant.PushConstant;
import com.android.push.bean.DownloadInfoBean;
import com.android.push.bean.FeedbackBean;
import com.android.push.bean.PushBean;
import com.android.push.bean.PushInfoBean;
import com.android.push.db.PushDataBase;
import com.android.push.util.DownloadTask;
import com.android.push.util.FeedbackUtil;
import com.android.push.util.JsonUtil;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import static com.android.push.constant.PushConstant.PUSH_PARCEL_KEY;

/**
 * 类描述 ：文字、链接文字的Activity
 *
 * @author 李翊星
 * @version 1.0
 */
public class TextDialogActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "push.TextDialog";

    private TextView tvTitle, tvContent;
    private Button btnGo, btnOk;

    private PushBean push;

    //  是否为带链接的Push
    private boolean isUrlPush = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  获取Push的Json字符串，并转换成PushBean
        Intent intent = getIntent();
        push = intent.getParcelableExtra(PUSH_PARCEL_KEY);
        if (push == null) {
            finish();
        }
        setContentView(R.layout.dialog_text);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        initView(push);
    }

    /**
     * 初始化界面
     */
    private void initView(PushBean push) {

        if (push == null) {
            finish();
        }

        tvTitle = (TextView) findViewById(R.id.tv_dialog_text_title);
        tvContent = (TextView) findViewById(R.id.tv_dialog_text_content);
        btnOk = (Button) findViewById(R.id.btn_dialog_text_ok);
        btnGo = (Button) findViewById(R.id.btn_dialog_text_go);

        btnOk.setOnClickListener(this);
        btnGo.setOnClickListener(this);

        // 设置标题、内容和按钮
        tvTitle.setText(push.title);
        tvContent.setText(push.content_text);
        String txBtnGo = push.button_content;
        if (txBtnGo != null && !"".equals(txBtnGo)) {
            isUrlPush = true;
            btnGo.setText(txBtnGo);
            btnGo.setVisibility(View.VISIBLE);
            btnOk.setBackground(getDrawable(R.drawable.shape_dialog_btn_ok1));
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_dialog_text_ok) {
            Log.d(TAG, "btnOk onclick");
            finish();
        }

        if (v.getId() == R.id.btn_dialog_text_go) {
            Log.d(TAG, "btnGo onclick");

            int pushType = push.type;
            switch (pushType) {

                // 网页推送
                case PushConstant.WEB_PUSH:
                    if (!"".equals(push.url)) {
                        webPush();
                    }
                    break;

                // 系统更新推送
                case PushConstant.OTA_PUSH:
                    try {
                        otaPush();
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                // 应用推送
                case PushConstant.APP_PUSH:
                    if (!"".equals(push.url)) {
                        appPush();
                    }
                    break;
            }

            finish();
        }
    }

    /**
     * 仅推送文字
     */
    private void onlyTextPush() {
        PushInfoBean pushInfo = push.push_info;

        //  记录PushInfo
        PushDataBase pushDataBase = PushDataBase.getInstance(this);
        pushDataBase.open();
        pushDataBase.recordPushInfo(pushInfo);
        pushDataBase.close();

        //  反馈操作
        FeedbackBean feedback = new FeedbackBean();
        feedback.push_id = pushInfo.push_id;
        feedback.is_clicked = 1;
        FeedbackUtil.feedbackToServer(this, feedback);
    }

    /**
     * 网页推送
     */
    private void webPush() {

        String url = push.url;
        PushInfoBean pushInfo = push.push_info;

        //  记录PushInfo
        PushDataBase pushDataBase = PushDataBase.getInstance(this);
        pushDataBase.open();
        pushDataBase.recordPushInfo(pushInfo);
        pushDataBase.close();

        //  反馈操作
        FeedbackBean feedback = new FeedbackBean();
        feedback.push_id = pushInfo.push_id;
        feedback.is_clicked = 1;
        FeedbackUtil.feedbackToServer(this, feedback);

        //  跳转到浏览器，打开指定页面
        if (url.startsWith("http://")) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private void otaPush() {
        PushInfoBean pushInfo = push.push_info;

        //  记录PushInfo
        PushDataBase pushDataBase = PushDataBase.getInstance(this);
        pushDataBase.open();
        pushDataBase.recordPushInfo(pushInfo);
        pushDataBase.close();

        //  反馈操作
        FeedbackBean feedback = new FeedbackBean();
        feedback.push_id = pushInfo.push_id;
        feedback.is_clicked = 1;
        FeedbackUtil.feedbackToServer(this, feedback);

        Intent otaIntent = new Intent(PushConstant.OTA_INTENT_ACTION);
        startActivity(otaIntent);
    }

    /**
     * 应用推送
     */
    private void appPush() {

        String url = push.url;
        PushInfoBean pushInfo = push.push_info;

        //初始化下载信息
        DownloadInfoBean downloadInfo = new DownloadInfoBean();
        downloadInfo.push_id = pushInfo.push_id;
        downloadInfo.push_priority = pushInfo.push_priority;
        downloadInfo.push_count = pushInfo.push_count;
        downloadInfo.file_type = PushConstant.APK_FILE_TYPE;
        // 安装方式
        if (push.install_mode == PushConstant.CLICK_INSTALL) {
            downloadInfo.install_mode = PushConstant.CLICK_INSTALL;
        } else {
            downloadInfo.install_mode = PushConstant.AUTO_INSTALL;
        }

        Log.d(TAG, "应用推送");
        downloadInfo.file_type = 2;
        DownloadTask.startDownload(this, url, downloadInfo);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (!isUrlPush && push != null) {
            onlyTextPush();
        }
        super.onDestroy();
    }
}
