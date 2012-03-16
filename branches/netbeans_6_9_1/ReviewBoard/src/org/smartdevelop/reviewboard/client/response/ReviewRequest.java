/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 *
 * @author Pragalathan M
 */
public class ReviewRequest {

    private String status;
    private Date lastUpdated;
    private String description;
    private String changeDescription;
    private boolean published;
    private List<Group> groups;
    private List<Bug> bugs;
    private String changenum;
    private List<People> people;
    private String testingDone;
    private String branch;
    private Date timeAdded;
    private String summary;
    private Long id;
    private Map<String, Link> links;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    @JsonProperty("changedescription")
    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    @JsonDeserialize(using = JsonDateSerializer.class)
    @JsonProperty("last_updated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @JsonProperty("bugs_closed")
    public List<Bug> getBugs() {
        return bugs;
    }

    public void setBugs(List<Bug> bugs) {
        this.bugs = bugs;
    }

    public String getChangenum() {
        return changenum;
    }

    public void setChangenum(String changenum) {
        this.changenum = changenum;
    }

    @JsonProperty("target_groups")
    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("target_people")
    public List<People> getPeople() {
        return people;
    }

    public void setPeople(List<People> people) {
        this.people = people;
    }

    public boolean isPublished() {
        return published;
    }

    @JsonProperty("public")
    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTestingDone() {
        return testingDone;
    }

    @JsonProperty("testing_done")
    public void setTestingDone(String testingDone) {
        this.testingDone = testingDone;
    }

    @JsonDeserialize(using = JsonDateSerializer.class)
    public Date getTimeAdded() {
        return timeAdded;
    }

    @JsonProperty("time_added")
    public void setTimeAdded(Date timeAdded) {
        this.timeAdded = timeAdded;
    }
}