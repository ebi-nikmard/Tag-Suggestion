/*
 * The `TagModel` class represents a model for storing information about a tag associated with an image.
 * It includes fields for the tag's confidence level and name, as well as methods to retrieve and set these values.
 */

package com.tag.tag.project.tag.model;

public class TagModel {
    private String tagConfidence;
    private String tagName;

    // Constructor to initialize TagModel with confidence and name
    public TagModel(String tagConfidence, String tagName) {
        this.tagConfidence = tagConfidence;
        this.tagName = tagName;
    }

    // Getter method to retrieve the tag's confidence level
    public String getTagConfidence() {
        return tagConfidence;
    }

    // Setter method to update the tag's confidence level
    public void setTagConfidence(String tagConfidence) {
        this.tagConfidence = tagConfidence;
    }

    // Getter method to retrieve the tag's name
    public String getTagName() {
        return tagName;
    }

    // Setter method to update the tag's name
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
