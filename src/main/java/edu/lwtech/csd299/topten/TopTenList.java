package edu.lwtech.csd299.topten;

import java.util.*;

public class TopTenList {
    
    // Encapsulated member variables
    private int id;                 // Database ID (or -1 if it isn't in the database yet)
    private String description;
    private List<String> items;     // Note: First item in list is #10.  Last item is #1
    private int ownerID;
    private boolean published;
    private long numViews;
    private long numLikes;
    
    public TopTenList(int id, TopTenList list) {
        this(id, list.description, list.items, list.published, list.ownerID, list.numViews, list.numLikes);
    }

    public TopTenList(String description, List<String> items, int ownerID) {
        this(-1, description, items, false, ownerID, 0, 0);
    }
    
    public TopTenList(int id, String description, List<String> items, boolean published, int ownerID, long numViews, long numLikes) {

        if (id < -1) throw new IllegalArgumentException("Invalid TopTenList argument: id < -1");
        if (description == null) throw new IllegalArgumentException("Invalid TopTenList argument: description is null");
        if (description.isEmpty()) throw new IllegalArgumentException("Invalid TopTenList argument: description is empty");
        if (items == null) throw new IllegalArgumentException("Invalid TopTenList argument: item list is null");
        if (items.size() < 10) throw new IllegalArgumentException("Invalid TopTenList argument: less than 10 items");
        if (ownerID < 0) throw new IllegalArgumentException("Invalid TopTenList argument: ownerID < 0");
        if (numViews < 0) throw new IllegalArgumentException("Invalid TopTenList argument: numViews < 0");
        if (numLikes < 0) throw new IllegalArgumentException("Invalid TopTenList argument: numLikes < 0");

        this.id = id;
        this.description = description;
        this.items = items;
        this.ownerID = ownerID;
        this.published = published;
        this.numViews = numViews;
        this.numLikes = numLikes;
    }
    
    public int getID() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getItems() {
        return new ArrayList<>(items);      // Return a copy of the item list
    }

    public int getOwnerID() {
        return this.ownerID;
    }

    public boolean isPublished() {
        return this.published;
    }

    public long getNumViews() {
        return this.numViews;
    }

    public long getNumLikes() {
        return this.numLikes;
    }

    public long addView() {
        return ++numViews;
    }

    public long addLike() {
        return ++numLikes;
    }

    public void publish(int ownerID) {
        if (ownerID == this.ownerID)
            published = true;
    }
    
    @Override
    public String toString() {
        return id + ": " + description
                + " (" + numViews + "/" + numLikes + ") "
                + (published ? "[Published]" : "[Draft]");
    }

}
