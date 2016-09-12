package com.lingyi.redbag.auto;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by lingyi on 16/9/6.
 * Copyright © 1994-2016 lingyi™ Inc. All rights reserved.
 */

public class MyAccessibilityService extends AccessibilityService {

    public static final String TAG = "lingyi";

    private boolean isKouling = false;

    private boolean isNeedClickKL = false;

    private AccessibilityNodeInfo curentInfo = null;

    private CopyOnWriteArrayList<AccessibilityNodeInfo> nodesToFetch = new CopyOnWriteArrayList<>();

    private List<String> fetchedIdentifiers = new ArrayList<>();

    private long specificHBTime = 0;



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

        int eventType = accessibilityEvent.getEventType();

        Log.i(TAG, "onAccessibilityEvent: "+eventType);
        Log.i(TAG, "onAccessibilityEvent: "+ accessibilityEvent.getClassName().toString());
        Log.i(TAG, "onAccessibilityEvent: "+accessibilityEvent.getClassName());
        Log.i(TAG, "onAccessibilityEvent: "+accessibilityEvent.getSource());
        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.i(TAG, "onAccessibilityEvent: notification");
                List<CharSequence> texts = accessibilityEvent.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        Log.i("demo", "text:"+content);
                        if (content.contains("[微信红包]")||content.contains("[QQ红包]")) {
                            //模拟打开通知栏消息
                            if (accessibilityEvent.getParcelableData() != null
                                    &&
                                    accessibilityEvent.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) accessibilityEvent.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }


                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                isKouling = false;
                String className = accessibilityEvent.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //开始抢红包
                   findAllWindowHongBao();
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    //开始打开红包
                    openPacket();
                } else if(className.equals("com.tencent.mobileqq.activity.SplashActivity")){
                    getPacket();
                }else if(className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")){
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }else if(className.equals("cooperation.qwallet.plugin.QWalletPluginProxyActivity")){
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    break;

                }
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                Log.i(TAG, "onAccessibilityEvent: TYPE_VIEW_ACCESSIBILITY_FOCUSED");
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.i(TAG, "onAccessibilityEvent: TYPE_VIEW_CLICKED");
                break;

            default:
                getPacket();
                findAllWindowHongBao();
                break;
        }

        for (CharSequence charSequence : accessibilityEvent.getText()) {
            Log.i(TAG, "onAccessibilityEvent: text:"+charSequence);
        }

    }

    @SuppressLint("NewApi")
    private void openPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            clickButton(nodeInfo);
            close();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void close() {
        AccessibilityNodeInfo nodeInfo;
        nodeInfo = getRootInActiveWindow();
        if(nodeInfo != null){
            List<AccessibilityNodeInfo> lists1 = nodeInfo
                    .findAccessibilityNodeInfosByText("红包详情");

           if(lists1 != null && lists1.size() > 0){
               nodesToFetch.remove(curentInfo);
               performGlobalAction(GLOBAL_ACTION_BACK);
               return;
           }
            //com.tencent.mobileqq:id/name
            //com.tencent.mobileqq:id/ivTitleBtnLeft

            List<AccessibilityNodeInfo> lists4 = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/name");
            if(lists4 != null && lists4.size() > 0){
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }
            List<AccessibilityNodeInfo> lists3 = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/ivTitleBtnLeft");
            if(lists3 != null && lists3.size() > 0){
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }
            List<AccessibilityNodeInfo> lists0 = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bak");
            if(lists0 != null && lists0.size() > 0){
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }

            List<AccessibilityNodeInfo> lists2 = nodeInfo
                    .findAccessibilityNodeInfosByText("手慢了,红包派完了");
            if(lists1 != null && lists1.size() > 0){
                nodesToFetch.remove(curentInfo);
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }
        }
    }

    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 打印一个节点的结构
     * @param info
     */
    @SuppressLint("NewApi")
    public void recycle(AccessibilityNodeInfo info) {
        if(info == null){
            return;
        }
        if (info.getChildCount() == 0) {
            if(info.getText() != null){
                if(!isNeedClickKL && ("领取红包".equals(info.getText().toString()))){
                    //这里有一个问题需要注意，就是需要找到一个可以点击的View
                    perClick(info);

                }else if(!isNeedClickKL && "口令红包".equals(info.getText().toString()) && !isKouling ){
                    if(info.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                        isKouling = true;
                        getPacket();
                    }
                    AccessibilityNodeInfo parent = info.getParent();
                    while(parent != null){
                        Log.i("demo", "parent isClick:"+parent.isClickable());
                        if(parent.isClickable()){
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if(!isKouling){
                                isKouling = true;
                                getPacket();
                            }
                            break;
                        }
                        parent = parent.getParent();
                    }
                }else if(isKouling && "点击输入口令".equals(info.getText().toString())){
                    perClick(info);
                    isNeedClickKL = true;
                    AccessibilityNodeInfo root   = getRootInActiveWindow();
                    if(root != null){
                       List<AccessibilityNodeInfo> sends =  root.findAccessibilityNodeInfosByText("发送");
                        if (sends != null) {
                            for (AccessibilityNodeInfo send : sends) {
                                if(send.isClickable()){
                                    send.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    isNeedClickKL = false;
                                    isKouling = false;
                                }else{
                                    AccessibilityNodeInfo parent = send.getParent();
                                    if (parent != null && parent.isClickable()) {
                                        send.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                        isNeedClickKL = false;
                                        isKouling = false;
                                    }
                                }

                                break;
                            }
                        }

                    }
                    getPacket();
                }else if("点击拆开".equals(info.getText().toString()) && !isKouling){
                    perClick(info);
                }else if( !isNeedClickKL && "QQ红包个性版".equals(info.getText().toString())){
                    long currentTime = System.currentTimeMillis();
                    if((currentTime - specificHBTime) / (100000*60*10) > 1){
                        specificHBTime =currentTime;
                        perClick(info);
                    }
                }
            }
        } else {
            for (int i = info.getChildCount() -1; i >= 0; i--) {
                if(info.getChild(i)!=null){
                    recycle(info.getChild(i));
                }
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void perClick(AccessibilityNodeInfo info) {
        if(info == null){
            return;
        }

        AccessibilityNodeInfo parent = info.getParent();
        while(parent != null){
            Log.i("demo", "parent isClick:"+parent.isClickable());
            if(parent.isClickable()){
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            parent = parent.getParent();
        }
    }

    @TargetApi(18)
    private String getHongbaoHash(AccessibilityNodeInfo info){
        AccessibilityNodeInfo parent = info;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        List<AccessibilityNodeInfo> ids = null;
        List<AccessibilityNodeInfo> title = null;
        if(root != null){
            ids = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/h5");
            title = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ey");
        }
       
        String desc = "";
        if(ids != null){
            for (AccessibilityNodeInfo id : ids) {
                Rect headRect = new Rect();
                id.getBoundsInScreen(headRect);
                Rect hbRect = new Rect();
                info.getBoundsInScreen(hbRect);
                if((headRect.bottom  - hbRect.top) > 0 &&(headRect.bottom  - hbRect.top) < 60){
                    String room = "";
                    if(title != null && title.size() > 0){
                        for (AccessibilityNodeInfo nodeInfo : title) {
                            if(nodeInfo.getClassName() != null && nodeInfo.getClassName().equals("android.widget.TextView")){
                                room = nodeInfo.getText().toString();
                                break;
                            }
                        }
                    }
                    desc = id.getContentDescription().toString()+id.hashCode()+info.hashCode()+room;
                    Log.i("lingyi","desc:"+desc);
                }
            }
        }


        String hash  = info.getText().toString() + info.getWindowId()+desc;
        Log.i(TAG, "getHongbaoHash: "+hash);
        return  hash;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void findAllWindowHongBao(){
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        find(rootNode);
        getMoney();
    }

    private void find(AccessibilityNodeInfo info){
        if(info == null){
            return;
        }
        int count = info.getChildCount();
        if(count == 0){
            if(info != null && info.getText() != null && (info.getText().toString().equals("领取红包")||info.getText().toString().equals("查看红包"))){
                String hash =  getHongbaoHash(info);
                if(hash != null && !fetchedIdentifiers.contains(hash)){
                    fetchedIdentifiers.add(hash);
                    nodesToFetch.add(info);
                }
            }
        }else{
            for (int i = info.getChildCount() -1; i >= 0; i--) {
                if(info.getChild(i)!=null){
                    find(info.getChild(i));
                }
            }
        }
    }

    private void getMoney(){
        if(nodesToFetch != null){
            for (AccessibilityNodeInfo info : nodesToFetch) {
                if(info.isClickable()){
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    curentInfo = info;
                    nodesToFetch.remove(info);
                }else{
                    AccessibilityNodeInfo parent = info.getParent();
                    if(parent != null && parent.isClickable()){
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        curentInfo = info;
                        nodesToFetch.remove(info);
                    }
                }

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void clickButton(AccessibilityNodeInfo info){
        if(info == null){
            return;
        }
        int count =  info.getChildCount();
       if(count == 0){
           if(info != null && info.getClassName() != null && info.getClassName().equals("android.widget.Button")){
               info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
               return;
           }
       }else {
           for (int i = 0; i < count; i++) {
               AccessibilityNodeInfo infos = info.getChild(i);
               clickButton(infos);
           }
       }
    }
}
