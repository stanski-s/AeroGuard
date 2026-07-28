package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class DiagnosticAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("action_id")
    private String actionId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("priority")
    private int priority;

    @JsonProperty("recommended_role")
    private String recommendedRole;

    @JsonProperty("is_fallback")
    private boolean fallback;

    public DiagnosticAction() {}

    public DiagnosticAction(String actionId, String title, String description, String severity, int priority, String recommendedRole) {
        this(actionId, title, description, severity, priority, recommendedRole, false);
    }

    public DiagnosticAction(String actionId, String title, String description, String severity, int priority, String recommendedRole, boolean fallback) {
        this.actionId = actionId;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.priority = priority;
        this.recommendedRole = recommendedRole;
        this.fallback = fallback;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getRecommendedRole() {
        return recommendedRole;
    }

    public void setRecommendedRole(String recommendedRole) {
        this.recommendedRole = recommendedRole;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    @Override
    public String toString() {
        return "DiagnosticAction{" +
                "actionId='" + actionId + '\'' +
                ", title='" + title + '\'' +
                ", severity='" + severity + '\'' +
                ", priority=" + priority +
                ", fallback=" + fallback +
                '}';
    }
}
