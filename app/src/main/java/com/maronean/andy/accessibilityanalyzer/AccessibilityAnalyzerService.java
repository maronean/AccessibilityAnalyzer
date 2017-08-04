/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modifications Copyright (C) 2017 Andrew Marone
 */

package com.maronean.andy.accessibilityanalyzer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Bitmap;
import android.util.Log;
import java.util.List;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;

import com.googlecode.eyesfree.utils.AccessibilityNodeInfoUtils;
import com.googlecode.eyesfree.utils.ContrastSwatch;
import com.googlecode.eyesfree.utils.ContrastUtils;
import com.googlecode.eyesfree.utils.ScreenshotUtils;
import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.ImageView;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.style.ClickableSpan;
import android.net.Uri;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import android.graphics.Rect;

import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.support.v4.view.ViewPager;
import java.util.HashSet;
import java.util.Set;


public class AccessibilityAnalyzerService extends AccessibilityService {
    public static CharSequence[] redundantKeywords = {"text","edit","button","image","view","checkbox","radio"};
    public HashMap<String,boolean[]> MAP = new HashMap<String,boolean[]>();
    static final String TAG = "AccessibilityAnalyzer";
    private Rect displayRect = new Rect();
    private String getEventType(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TYPE_VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "TYPE_VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "TYPE_VIEW_SELECTED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                return "TYPE_WINDOWS_CHANGED";
        }
        return "default";
    }

    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }
    public String globalPname;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {

            AccessibilityNodeInfo nodeInfo = event.getSource();
            AccessibilityNodeInfoCompat nodeInfoCompat = new AccessibilityNodeInfoCompat(nodeInfo);
            CharSequence pName = nodeInfoCompat.getPackageName();
            Context context = this;
            List<AccessibilityNodeInfoCompat> infoNodes = AccessibilityNodeInfoUtils.searchAllFromBfsInclusive(nodeInfoCompat);
            boolean test1 = true;
            boolean test2 = true;
            boolean test3 = true;
            boolean test4 = true;
            boolean test5 = true;
            boolean test6 = true;
            String pack = "";
            CharSequence packChars;
            //TODO
            /**if(nodeInfo != null && nodeInfoCompat != null) {
                if (!checkForSpeakableText(nodeInfo, nodeInfoCompat)) {
                    test5 = false;
                }
            }*/
            for(AccessibilityNodeInfoCompat infoCompat:infoNodes) {
                AccessibilityNodeInfo info = (AccessibilityNodeInfo)infoCompat.getInfo();
                if(infoCompat != null && info != null) {
                    packChars = info.getPackageName();
                    if(packChars != null){
                        pack = packChars.toString();
                        globalPname = pack;
                        if(!MAP.containsKey(pack)){
                            boolean[] bSetup = {true,true,true,true,true,true};
                            MAP.put(pack,bSetup);
                        }
                    }
                    if(!descriptionNotEditable(info)){
                        test1 = false;
                    }
                    if(!checkClickableSpans(info, this, infoCompat)){
                        test2 = false;
                    }
                    if(!checkDuplicateClickableObjectHelper(info, this)){
                        test3 = false;
                    }
                    if(!checkRedundantContent(infoCompat)){
                        test4 = false;
                    }
                    if(!checkForSpeakableText(info, infoCompat)){
                     test5 = false;
                    }
                    if(!this.checkTouchTargetSize(info,infoCompat)){
                        test6 = false;
                    }
                    boolean[] results = {test1,test2,test3,test4,test5,test6};
                    for(int i = 0; i<results.length;i++){
                        if(!results[i]){
                            MAP.get(pack)[i] = false;
                        }
                    }
                }

            }
            boolean[] results = MAP.get(pack);
            if(results != null) {
                String strResult = String.format("%b,%b,%b,%b,%b,%b", results[0], results[1], results[2], results[3], results[4], results[5]);
                Log.v(TAG + ": " + pack, strResult);
            }


        } catch(Exception e){
            Log.e(TAG,e.getMessage());
        }

    }

    /**
     *  Check if editable object includes a content description
     * @param info
     * @return
     */
    public boolean descriptionNotEditable(AccessibilityNodeInfo info){
        boolean result;
        if (info.isEditable()) {
            result = (TextUtils.isEmpty(info.getContentDescription()));
            if(!TextUtils.isEmpty(info.getContentDescription())) {
                sendErrorMessage("Editable object should not have content description");
                result = false;
            }
        }else{
            result = true;
        }
        return result;
    }

    /**
     * Check for use of clickable spans used as buttons
     * @param info
     * @param context
     * @param infoCompat
     * @return
     */
    public boolean checkClickableSpans(AccessibilityNodeInfo info, Context context, AccessibilityNodeInfoCompat infoCompat) {
        boolean result = true;
        if (AccessibilityNodeInfoUtils.nodeMatchesAnyClassByType(context, infoCompat, TextView.class)) {
            if (info.getText() instanceof Spanned) {
                Spanned text = (Spanned) info.getText();
                ClickableSpan[] clickableSpans = text.getSpans(0, text.length(), ClickableSpan.class);
                for (ClickableSpan clickableSpan : clickableSpans) {
                    if (clickableSpan instanceof URLSpan) {
                        String url = ((URLSpan) clickableSpan).getURL();
                        if (url == null) {
                            sendErrorMessage("URLSpan has null URL");
                            result = false;
                        } else {
                            Uri uri = Uri.parse(url);
                            Log.i(TAG,"Has URI" + uri.toString());
                            if (uri.isRelative()) {
                                sendErrorMessage("URLSpan should not contain relative links");
                                result = false;
                            }
                        }
                    } else { // Non-URLSpan ClickableSpan
                        sendErrorMessage("URLSpan should be used in place of ClickableSpan for improved accessibility");
                        result = false;
                    }
                }
            }
        }
        return result;
    }
    /**
     //TODO reimplement contrast check
    public void checkContrastHelper(AccessibilityNodeInfo info, AccessibilityNodeInfoCompat infoCompat) {
        CaptureBitmap captureBitmap = CaptureBitmap.getInstance();
        Intent svc = new Intent(this, CaptureBitmap.class);
        captureBitmap.takeScreenshot(this,24601,svc,info,infoCompat,this);
    }*/
    /**
     * check for issues between element contrast that impact visibility
     * @param info
     * @param infoCompat
     * @return
     */

    public boolean checkContrast(AccessibilityNodeInfo info, AccessibilityNodeInfoCompat infoCompat, Bitmap screenshot) {
        boolean isText =
                AccessibilityNodeInfoUtils.nodeMatchesAnyClassByType(this, infoCompat, TextView.class);
        boolean isImage =
                AccessibilityNodeInfoUtils.nodeMatchesAnyClassByType(this, infoCompat, ImageView.class);
        boolean hasText = !TextUtils.isEmpty(infoCompat.getText());
        boolean isVisible = AccessibilityNodeInfoUtils.isVisibleOrLegacy(infoCompat);
        if(isVisible && ((isText && hasText) || isImage)) {
            Rect screenCaptureBounds =
                    new Rect(0, 0, screenshot.getWidth() - 1, screenshot.getHeight() - 1);
            Rect viewBounds = new Rect();
            info.getBoundsInScreen(viewBounds);
            if (!screenCaptureBounds.contains(viewBounds)) {
                return true;
            }
            ContrastSwatch candidateSwatch = new ContrastSwatch(
                    ScreenshotUtils.cropBitmap(screenshot, viewBounds), viewBounds,
                    info.getViewIdResourceName());
            double contrastRatio = candidateSwatch.getContrastRatio();
            if (AccessibilityNodeInfoUtils.nodeMatchesAnyClassByType(this, infoCompat,
                    TextView.class)) {
                if (contrastRatio < ContrastUtils.CONTRAST_RATIO_WCAG_LARGE_TEXT) {
                    sendErrorMessage("This view's foreground to background contrast ratio is not sufficient");
                } else if (contrastRatio < ContrastUtils.CONTRAST_RATIO_WCAG_NORMAL_TEXT) {
                    sendErrorMessage("This view's foreground to background contrast ratio is not sufficient");
                }
            }
            candidateSwatch.recycle();
        }else {
            return true;
        }
        return true;
    }

    public void sendErrorMessage(String msg){
        Log.e("AccessibilityAnalyzer: "+ globalPname,msg);
    }

    public boolean checkDuplicateClickableObjectHelper(AccessibilityNodeInfo info, Context context) {
        Map<Rect, AccessibilityNodeInfo> clickableRectToInfoMap = new HashMap<>();
        return checkDuplicateClickableObject(info, clickableRectToInfoMap, context);

    }

    /**
     * Check for duplicate clickable objects
     * @param info
     * @param clickableRectToInfoMap
     * @param context
     */
    public boolean checkDuplicateClickableObject(AccessibilityNodeInfo info, Map<Rect, AccessibilityNodeInfo> clickableRectToInfoMap,  Context context) {

        if (info.isClickable() && info.isVisibleToUser()) {
            Rect bounds = new Rect();
            info.getBoundsInScreen(bounds);
            if (clickableRectToInfoMap.containsKey(bounds)) {
                sendErrorMessage("Duplicate clickable view found");
                return false;
            } else {
                clickableRectToInfoMap.put(bounds, AccessibilityNodeInfo.obtain(info));
            }
        }
        boolean duplicate = true;
        for (int i = 0; i < info.getChildCount(); ++i) {
            AccessibilityNodeInfo child = info.getChild(i);
            if(checkDuplicateClickableObject(child, clickableRectToInfoMap, context) == false){
                duplicate = false;
            }

            child.recycle();
        }
        return duplicate;
    }

    /**
     * Check for redundant content desc
     * @param info
     */
    public boolean checkRedundantContent(AccessibilityNodeInfoCompat info) {
        CharSequence desc = info.getContentDescription();
        for(CharSequence keyword : redundantKeywords) {
            if(desc != null) {
                if (desc.toString().toLowerCase().contains(keyword)) {
                    sendErrorMessage("Item description contains redundant content: " + desc.toString());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if event contains speakable text
     * @param info
     * @param compatInfo
     * @return
     */
    public boolean checkForSpeakableText(AccessibilityNodeInfo info,AccessibilityNodeInfoCompat compatInfo) {
        List<Class<? extends ViewGroup>> blacklistedViewTypes =
                Arrays.asList(ListView.class, ScrollView.class, ViewPager.class, WebView.class);
        for (Class<? extends ViewGroup> check : blacklistedViewTypes) {
            if (AccessibilityNodeInfoUtils.nodeMatchesAnyClassByType(null, compatInfo, check)) {
                return true;
            }
        }
        if (AccessibilityNodeInfoUtils.shouldFocusNode(this, compatInfo)) {
            if (TextUtils.isEmpty(getSpeakableTextForInfo(info))) {
                sendErrorMessage("View is missing speakable text needed for a screen reader");
                return false;
            }
        }
        return true;
    }

    static CharSequence getSpeakableTextForInfo(AccessibilityNodeInfo info) {
        if (info == null) {
            return null;
        }
        AccessibilityNodeInfo labeledBy = info.getLabeledBy();
        if (labeledBy != null) {
            Set<AccessibilityNodeInfo> infosVisited = new HashSet<>();
            infosVisited.add(info);
            infosVisited.add(labeledBy);
            AccessibilityNodeInfo endOfLabeledByChain = labeledBy.getLabeledBy();
            while (endOfLabeledByChain != null) {
                if (infosVisited.contains(endOfLabeledByChain)) {
                    infosVisited.remove(info);
                    for (AccessibilityNodeInfo infoVisited : infosVisited) {
                        infoVisited.recycle();
                    }
                    return null;
                }
                infosVisited.add(endOfLabeledByChain);
                labeledBy = endOfLabeledByChain;
                endOfLabeledByChain = labeledBy.getLabeledBy();
            }
            CharSequence labelText = getSpeakableTextForInfo(labeledBy);
            infosVisited.remove(info);
            for (AccessibilityNodeInfo infoVisited : infosVisited) {
                infoVisited.recycle();
            }
            return labelText;
        }

        AccessibilityNodeInfoCompat compat = new AccessibilityNodeInfoCompat(info);

        CharSequence nodeText = AccessibilityNodeInfoUtils.getNodeText(compat);
        StringBuilder returnStringBuilder = new StringBuilder((nodeText == null) ? "" : nodeText);
        if (TextUtils.isEmpty(compat.getContentDescription())) {
            for (int i = 0; i < compat.getChildCount(); ++i) {
                AccessibilityNodeInfoCompat child = compat.getChild(i);
                if (AccessibilityNodeInfoUtils.isVisibleOrLegacy(child)
                        && !AccessibilityNodeInfoUtils.isActionableForAccessibility(child)) {
                    returnStringBuilder.append(
                            getSpeakableTextForInfo((AccessibilityNodeInfo) child.getInfo()));
                }
            }
        }
        return returnStringBuilder;
    }

    /**
     * Check that any toutch targets are an acceptable size
     * @param info
     * @return
     */
    public boolean checkTouchTargetSize(AccessibilityNodeInfo info, AccessibilityNodeInfoCompat infoCompat) {
        if ((AccessibilityNodeInfoUtils.isClickable(infoCompat) || AccessibilityNodeInfoUtils.isLongClickable(infoCompat)) && infoCompat.isVisibleToUser()) {
            AccessibilityNodeInfo parent = info.getParent();
            if(parent!= null){
                if((AccessibilityNodeInfoUtils.isClickable(infoCompat.getParent())||AccessibilityNodeInfoUtils.isLongClickable(infoCompat.getParent())) && infoCompat.getParent().isVisibleToUser()){
                    Log.i("AccessibilityAnalyzer","Has a parent");
                    return checkTouchTargetSize(parent, new AccessibilityNodeInfoCompat(parent));
                }
            }
            float density = this.getResources().getDisplayMetrics().density;
            Rect bounds = new Rect();


            info.getBoundsInScreen(bounds);
            int targetHeight = (int) (Math.abs(bounds.height()) / density);
            int targetWidth = (int) (Math.abs(bounds.width()) / density);
            if (targetHeight < 48 || targetWidth < 48) {
                if(bounds.top == displayRect.top || bounds.bottom == displayRect.bottom ||
                        bounds.left == displayRect.left || bounds.right == displayRect.right) {
                    Log.i(TAG,"Intersecting Rect" + bounds.toString() + " + " + displayRect.toString());

                    return true;
                }
                if(targetHeight < 48 ){
                    sendErrorMessage(String.format("Touch target is below the minimum height %d",targetHeight));
                }
                if(targetWidth < 48){
                    sendErrorMessage(String.format("Touch target is below the minimum width %d",targetWidth));
                }
                CharSequence desc = infoCompat.getContentDescription();
                if(desc != null) {
                    Log.e(TAG,"Following object is too small: " + desc.toString());
                }

                return false;
            }
        }
        return true;
    }
    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected");
        Log.v(TAG,String.format("density: %f",this.getResources().getDisplayMetrics().density));
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getRectSize(displayRect);

        /**
         Intent svc = new Intent(this, AccessibilityOverlay.class);
         startService(svc);
         **/
    }

}