package org.joget.marketplace;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class LikeDislike extends Element implements FormBuilderPaletteElement, PluginWebSupport {

    private final static String MESSAGE_PATH = "messages/LikeDislikeElement";

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "likeDislikeElement.ftl";
        String value = FormUtil.getElementPropertyValue(this, formData);
        String id = formData.getPrimaryKeyValue();

        String likeDislikeField = getPropertyString("likeDislikeField");
        String foreignKeyField = getPropertyString("foreignKeyField");
        String formDefId = getPropertyString("formDefId");
        
        long likeCount = 0;
        long dislikeCount = 0;

        String config = getConfigString();

        WorkflowUserManager wum = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        boolean anonymous = wum.isCurrentUserAnonymous();
        dataModel.put("anonymous", anonymous ? "true" : "false");
        if (FormUtil.isFormBuilderActive()) {
            dataModel.put("likes", likeCount);
            dataModel.put("dislikes", dislikeCount);
            id = "";
        } else {
            HttpServletRequest httpServletRequest = WorkflowUtil.getHttpServletRequest();
            likeCount = getLikeDislikeCount(formDefId, foreignKeyField, id, likeDislikeField, "like");
            dislikeCount = getLikeDislikeCount(formDefId, foreignKeyField, id, likeDislikeField, "dislike");
            dataModel.put("likes", likeCount);
            dataModel.put("dislikes", dislikeCount);
        }
        
        dataModel.put("id", id);
        
        // current user like dislike active value
        String likedislike = getUserLikeDislike(formDefId, foreignKeyField, id, likeDislikeField);
        dataModel.put("active", likedislike);

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    public String getServiceUrl() {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String url = WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/app/" + appDef.getAppId() + "/" + appDef.getVersion() + "/plugin/org.joget.marketplace.LikeDislike/service";
        //create nonce
        String paramName = "form-contents";
        String formDefId = getPropertyString("formDefId");
        String nonce = SecurityUtil.generateNonce(new String[]{"ContentLikeDislike", appDef.getAppId(), appDef.getVersion().toString(), paramName}, 1);
        setProperty("nonce", nonce);
        try {
            url = url + "?_nonce=" + URLEncoder.encode(nonce, "UTF-8") + "&_paramName=" + URLEncoder.encode(paramName, "UTF-8") + "&_formDefId=" + formDefId;
        } catch (Exception e) {
        }
        return url;
    }

    public String getConfigString() {
        String config = "";
        try {
            JSONObject pluginProperties = FormUtil.generatePropertyJsonObject(getProperties());
            config = pluginProperties.toString();
        } catch (JSONException ex) {
            LogUtil.error(getClassName(), ex, ex.getMessage());
        }
        config = SecurityUtil.encrypt(config);
        return config;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("org.joget.marketplace.likedislike.element.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.likedislike.element.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.marketplace.likedislike.element.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/likeDislikeElement.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String getFormBuilderCategory() {
        return "Marketplace";
    }

    @Override
    public int getFormBuilderPosition() {
        return 500;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-thumbs-up\"></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>Like Dislike</label><div></div>";
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            String nonce = request.getParameter("_nonce");
            String paramName = request.getParameter("_paramName");
            String fkValue = request.getParameter("id");
            String formDefId = "";
            String fkField = "";
            String likeDislikeField = "";
            if (SecurityUtil.verifyNonce(nonce, new String[]{"ContentLikeDislike", appDef.getAppId(), appDef.getVersion().toString(), paramName})) {
                String config = SecurityUtil.decrypt(request.getParameter("config"));
                JSONObject c = new JSONObject(config);
                if (nonce.equals(c.getString("nonce"))) {
                    formDefId = c.getString("formDefId");
                    likeDislikeField = c.getString("likeDislikeField");
                    fkField = c.getString("foreignKeyField");
                    String condition = "where e.customProperties." + fkField + " = ? AND e.createdBy = ?" ;
                    String clickedValue = request.getParameter("clickedValue");
                    String buttonId = request.getParameter("buttonId");
                    AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
                    FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                    String tableName = appService.getFormTableName(appDef, formDefId);
                    String username = WorkflowUtil.getCurrentUsername();

                    if (Integer.parseInt(clickedValue) == 0){
                        FormRowSet ldRows = formDataDao.find(formDefId, tableName, condition, new String[]{fkValue, username}, FormUtil.PROPERTY_DATE_MODIFIED, true, 0, 1);
                        if (ldRows != null && !ldRows.isEmpty()) {
                            formDataDao.delete(formDefId, tableName, ldRows);
                        }
                    } else {
                        // check if the this user has record in table
                        FormRowSet ldRows = formDataDao.find(formDefId, tableName, condition, new String[]{fkValue, username}, FormUtil.PROPERTY_DATE_MODIFIED, true, 0, 1);
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
            }
            long likeCount = getLikeDislikeCount(formDefId, fkField, fkValue, likeDislikeField, "like");
            long dislikeCount = getLikeDislikeCount(formDefId, fkField, fkValue, likeDislikeField, "dislike");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("likes", likeCount);
            jsonObject.put("dislikes", dislikeCount);
            response.setContentType("application/json");
            response.getWriter().write(jsonObject.toString());
        }
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

    protected String getUserLikeDislike(String formDefId, String foreignKeyField, String foreignKeyValue, String fieldId) {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String tableName = appService.getFormTableName(appDef, formDefId);
        String likedislike = "";
        String username = WorkflowUtil.getCurrentUsername();
        String condition = "where e.customProperties." + foreignKeyField + " = ? AND e.createdBy = ?";
        FormRowSet rows = formDataDao.find(formDefId, tableName, condition, new String[]{foreignKeyValue, username}, FormUtil.PROPERTY_DATE_MODIFIED, true, 0, null);
        if (rows != null && !rows.isEmpty()) {
            likedislike = rows.get(0).get(fieldId).toString();
        }
        return likedislike;
    }

}
