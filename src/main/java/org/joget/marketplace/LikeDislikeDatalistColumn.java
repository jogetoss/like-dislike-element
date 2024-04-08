package org.joget.marketplace;

import org.apache.commons.lang.StringUtils;
import org.joget.apps.datalist.model.DataListDisplayColumnDefault;
import org.joget.apps.datalist.model.DataListQueryParam;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class LikeDislikeDatalistColumn extends DataListDisplayColumnDefault implements PluginWebSupport{
    private final static String MESSAGE_PATH = "messages/LikeDislikeDatalistColumn";
 
    @Override
    public String getName() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.likedislike.datalist.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }
    
    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.marketplace.likedislike.datalist.pluginLabel", getClassName(), MESSAGE_PATH);
    }
    
    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.likedislike.datalist.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getColumnHeader() {
        return getPropertyString("label");
    }

    @Override
    public Boolean isRenderHtml() {
        return true;
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/likeDislikeDatalistColumn.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-thumbs-up\"></i>";
    }

    @Override
    public String getRowValue(Object row, int index) {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        HashMap<String, Object> rowHash = (HashMap<String, Object>) row;
        DataListQueryParam param = getDatalist().getQueryParam(null, null);
        int offset = param.getStart() + 1;
        
        String rowNo = Integer.toString(offset + index);
        
        if (!getPropertyString("addLeadingZero").isEmpty()) {
            try {
                int digit = Integer.parseInt(getPropertyString("addLeadingZero"));
                if (digit > rowNo.length()) {
                    rowNo = StringUtils.leftPad(rowNo, digit, '0');
                }
            } catch (Exception e){
                //ignore
            }
        }
        
        String fkValue = rowHash.get("id").toString();
        String formDefId = getPropertyString("formDefId");
        String fkField = getPropertyString("foreignKeyField");
        String likeDislikeField = getPropertyString("likeDislikeField");
        long likeCount = getLikeDislikeCount(formDefId, fkField, fkValue, likeDislikeField, "like");
        long dislikeCount = getLikeDislikeCount(formDefId, fkField, fkValue, likeDislikeField, "dislike");

        //create nonce
        String paramName = "form-contents";
        String nonce = SecurityUtil.generateNonce(new String[]{"ContentLikeDislike", appDef.getAppId(), appDef.getVersion().toString(), paramName}, 1);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("foreignKeyField", getPropertyString("foreignKeyField"));
        jsonObject.put("formDefId", getPropertyString("formDefId"));
        jsonObject.put("likeDislikeField", getPropertyString("likeDislikeField"));
        jsonObject.put("redirectUserviewMenu", getPropertyString("redirectUserviewMenu"));
        jsonObject.put("redirectUserviewMenuListID", getPropertyString("redirectUserviewMenuListID"));
        jsonObject.put("nonce", nonce);
        String config = jsonObject.toString();
        config = StringUtil.escapeString(SecurityUtil.encrypt(config), StringUtil.TYPE_URL, null);
      
        String html = "<a class=\"far fa-lg fa-thumbs-up\" href=\"" + getServerUrlPlugin(nonce, paramName) + "&config=" + config + "&id=" + rowHash.get("id").toString() + "&buttonId=like-btn" + "\"> " + likeCount + "</a>";
        html += "&nbsp;&nbsp;<a class=\"far fa-lg fa-thumbs-down\" href=\"" + getServerUrlPlugin(nonce, paramName) + "&config=" + config + "&id=" + rowHash.get("id").toString() + "&buttonId=dislike-btn"  + "\"> " + dislikeCount + "</a>";
        return html;
    }

    public String getServerUrlPlugin(String nonce, String paramName) {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formDefId = getPropertyString("formDefId");
        String url = WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/app/" + appDef.getAppId() + "/" + appDef.getVersion() + "/plugin/org.joget.marketplace.LikeDislikeDatalistColumn/service";

        try {
            url = url + "?_nonce=" + URLEncoder.encode(nonce, "UTF-8") + "&_paramName=" + URLEncoder.encode(paramName, "UTF-8") + "&_formDefId=" + formDefId;
        } catch (Exception e) {
        }
        return url;
    }

    public static String getServerUrl() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        StringBuffer url = request.getRequestURL();
        URL requestUrl;
        String serverUrl = "";
        try {
            requestUrl = new URL(url.toString());
            serverUrl = requestUrl.getProtocol() + "://" + requestUrl.getHost();
            // Include port if it is present
            int port = requestUrl.getPort();
            if (port != -1) {
                serverUrl += ":" + port;
            }
            serverUrl += request.getContextPath();
        } catch (MalformedURLException ex) {
            LogUtil.error("", ex, ex.getMessage());
        }
        return serverUrl;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String nonce = request.getParameter("_nonce");
        String paramName = request.getParameter("_paramName");
        String fkValue = request.getParameter("id");
        String formDefId = "";
        String fkField = "";
        String likeDislikeField = "";
        String redirectUserviewMenu = "";
        String redirectUserviewMenuListID = "";
        if (SecurityUtil.verifyNonce(nonce, new String[] { "ContentLikeDislike", appDef.getAppId(), appDef.getVersion().toString(), paramName })) {
            String config = SecurityUtil.decrypt(request.getParameter("config"));
            JSONObject c = new JSONObject(config);
            if (nonce.equals(c.getString("nonce"))) {
                formDefId = c.getString("formDefId");
                likeDislikeField = c.getString("likeDislikeField");
                fkField = c.getString("foreignKeyField");
                redirectUserviewMenu = c.getString("redirectUserviewMenu");
                redirectUserviewMenuListID = c.getString("redirectUserviewMenuListID");
                String condition = "where e.customProperties." + fkField + " = ? AND e.createdBy = ?";
                String buttonId = request.getParameter("buttonId");
                AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                String tableName = appService.getFormTableName(appDef, formDefId);
                String username = WorkflowUtil.getCurrentUsername();
                // check if the this user has record in table
                FormRowSet ldRows = formDataDao.find(formDefId, tableName, condition, new String[] { fkValue, username }, FormUtil.PROPERTY_DATE_MODIFIED, true, 0, 1);
                if (ldRows != null && !ldRows.isEmpty()) {
                    FormRow ldRow = ldRows.get(0);
                    String rId = ldRow.getId();
                    handleLikeDislike(buttonId, fkValue, formDefId, tableName, fkField, likeDislikeField, rId, appService);
                } else {
                    // no record found, insert new row
                    handleLikeDislike(buttonId, fkValue, formDefId, tableName, fkField, likeDislikeField, null, appService);
                }
            }
        }
        long likeCount = getLikeDislikeCount(formDefId, fkField, fkValue, likeDislikeField, "like");
        long dislikeCount = getLikeDislikeCount(formDefId, fkField, fkValue, likeDislikeField, "dislike");

        String appId = appDef.getAppId();
        String redirectURL = getServerUrl() + "/web/userview/" + appId + "/" + redirectUserviewMenu + "/_/" + redirectUserviewMenuListID;
        response.sendRedirect(redirectURL);
    }

    protected long getLikeDislikeCount(String formDefId, String foreignKeyField, String foreignKeyValue, String fieldId, String event) {
        long count = 0;
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String tableName = appService.getFormTableName(appDef, formDefId);
        String condition = "where e.customProperties." + foreignKeyField + " = ? AND e.customProperties." + fieldId + "= ?";
        count = formDataDao.count(formDefId, tableName, condition, new String[]{foreignKeyValue, event});
        return count;
    }

    private void handleLikeDislike(String buttonId, String fkId, String formDefId, String tableName, String fkField, String fieldId, String rId, AppService appService) {
        FormRowSet rows = new FormRowSet();
        FormRow row = new FormRow();
        row.put(fkField, fkId);
        row.put(fieldId, buttonId.equals("like-btn") ? "like" : "dislike");
        rows.add(row);
        appService.storeFormData(formDefId, tableName, rows, rId);
    }
}
