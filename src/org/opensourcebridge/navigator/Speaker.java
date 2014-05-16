package org.opensourcebridge.navigator;

import org.json.JSONException;
import org.json.JSONObject;

public class Speaker {
    String name = null;
    String biography = null;
    String twitter = null;
    String affiliation = null;
    String identica = null;
    String website = null;
    String blog = null;

    public Speaker(JSONObject attributes) throws JSONException {
        this.name  = attributes.getString("fullname");
        this.biography  = attributes.getString("biography").replace("\r","");
        if (attributes.has("twitter")) {
            this.twitter  = attributes.getString("twitter");
        }
        if (attributes.has("identica")){
            this.identica  = attributes.getString("identica");
        }
        if (attributes.has("website")) {
            this.website  = attributes.getString("website");
        }
        if (attributes.has("blog_url")) {
            this.blog = attributes.getString("blog_url");
        }
        if (attributes.has("affiliation")) {
            this.affiliation  = attributes.getString("affiliation");
        }

    }
}
